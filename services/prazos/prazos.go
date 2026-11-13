// Package prazos implementa as regras de negócio puras para o cálculo de
// prazos de devolução de ativos físicos com base em perfis acadêmicos e calendário.
//
// O pacote é o domínio isolado do microsserviço: não importa gRPC, HTTP nem banco.
// Todas as operações respeitam o cancelamento e o prazo transmitidos via context.Context.
package prazos

import (
	"context"
	"errors"
	"fmt"
	"strings"
	"time"
)

// Erros sentinela nomeados para verificação com errors.Is.
var (
	// ErrDadosInvalidos indica que campos obrigatórios da solicitação estão ausentes.
	ErrDadosInvalidos = errors.New("dados da solicitação de prazo inválidos")

	// ErrPerfilDesconhecido indica que o perfil do usuário não é atendido pelas regras do sistema.
	ErrPerfilDesconhecido = errors.New("perfil de usuário não reconhecido")
)

// Constantes de perfis de usuário suportados pelo SmOrg.
const (
	PerfilAluno     = "ALUNO"
	PerfilMonitor   = "MONITOR"
	PerfilProfessor = "PROFESSOR"
)

// Prazos em dias úteis concedidos por perfil acadêmico.
const (
	DiasUteisAluno     = 3
	DiasUteisMonitor   = 7
	DiasUteisProfessor = 15
)

// SolicitacaoCalculo contém os parâmetros necessários para calcular o prazo de devolução.
type SolicitacaoCalculo struct {
	UsuarioID            string
	PerfilUsuario        string
	EquipamentoID        string
	CategoriaEquipamento string
	DataRetirada         time.Time
}

// ResultadoCalculo representa a data prevista e os detalhes da concessão do empréstimo.
type ResultadoCalculo struct {
	DataDevolucaoPrevista time.Time
	DiasConcedidos        int
	Observacoes           string
}

// Calculador é o motor de regras que computa prazos com base no calendário acadêmico.
type Calculador struct {
	calendario Calendario
}

// NovoCalculador cria uma nova instância do motor de cálculo de prazos.
func NovoCalculador(cal Calendario) *Calculador {
	if cal == nil {
		cal = NovoCalendarioAcademico()
	}
	return &Calculador{calendario: cal}
}

// ObterDiasPorPerfil retorna a quantidade de dias úteis padrão para cada perfil acadêmico.
func ObterDiasPorPerfil(perfil string) (int, error) {
	switch strings.ToUpper(strings.TrimSpace(perfil)) {
	case PerfilAluno:
		return DiasUteisAluno, nil
	case PerfilMonitor:
		return DiasUteisMonitor, nil
	case PerfilProfessor:
		return DiasUteisProfessor, nil
	default:
		return 0, fmt.Errorf("%w: perfil %q", ErrPerfilDesconhecido, perfil)
	}
}

// CalcularPrazo calcula a data limite de devolução com base nos dias úteis e calendário.
// Respeita context.Context: se o prazo do chamador expirar, desiste imediatamente.
func (c *Calculador) CalcularPrazo(ctx context.Context, s SolicitacaoCalculo) (ResultadoCalculo, error) {
	// 1. Respeito ao contexto antes de iniciar processamento
	select {
	case <-ctx.Done():
		return ResultadoCalculo{}, fmt.Errorf("cálculo cancelado por prazo: %w", ctx.Err())
	default:
	}

	// 2. Validação dos dados obrigatórios
	if strings.TrimSpace(s.UsuarioID) == "" {
		return ResultadoCalculo{}, fmt.Errorf("%w: usuario_id não informado", ErrDadosInvalidos)
	}
	if strings.TrimSpace(s.EquipamentoID) == "" {
		return ResultadoCalculo{}, fmt.Errorf("%w: equipamento_id não informado", ErrDadosInvalidos)
	}
	if strings.TrimSpace(s.PerfilUsuario) == "" {
		return ResultadoCalculo{}, fmt.Errorf("%w: perfil_usuario não informado", ErrDadosInvalidos)
	}

	diasUteis, err := ObterDiasPorPerfil(s.PerfilUsuario)
	if err != nil {
		return ResultadoCalculo{}, err
	}

	// Data base de partida (se não informada, assume o instante atual UTC)
	dataAtual := s.DataRetirada.UTC()
	if dataAtual.IsZero() {
		dataAtual = time.Now().UTC()
	}

	// 3. Cálculo incremental dos dias úteis
	diasContados := 0
	cursor := dataAtual

	for diasContados < diasUteis {
		select {
		case <-ctx.Done():
			return ResultadoCalculo{}, fmt.Errorf("cálculo interrompido: %w", ctx.Err())
		default:
		}

		cursor = cursor.AddDate(0, 0, 1)
		if c.calendario.EhDiaUtil(cursor) {
			diasContados++
		}
	}

	// Data de devolução expira às 23:59:59 UTC do último dia útil computado
	dataFinal := time.Date(cursor.Year(), cursor.Month(), cursor.Day(), 23, 59, 59, 0, time.UTC)

	obs := fmt.Sprintf("Concedidos %d dias úteis para o perfil %s conforme calendário acadêmico",
		diasUteis, strings.ToUpper(strings.TrimSpace(s.PerfilUsuario)))

	return ResultadoCalculo{
		DataDevolucaoPrevista: dataFinal,
		DiasConcedidos:        diasUteis,
		Observacoes:           obs,
	}, nil
}

