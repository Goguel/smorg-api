package prazos

import (
	"context"
	"errors"
	"testing"
	"time"
)

func TestCalcularPrazoAlunoSemFimDeSemana(t *testing.T) {
	cal := NovoCalendarioAcademico()
	calc := NovoCalculador(cal)

	// Quinta-feira, 05/11/2026 às 10:00:00 UTC
	dataInicio := time.Date(2026, 11, 5, 10, 0, 0, 0, time.UTC)

	sol := SolicitacaoCalculo{
		UsuarioID:            "user-123",
		PerfilUsuario:        "ALUNO",
		EquipamentoID:        "equip-456",
		CategoriaEquipamento: "MULTIMETRO",
		DataRetirada:         dataInicio,
	}

	res, err := calc.CalcularPrazo(context.Background(), sol)
	if err != nil {
		t.Fatalf("erro inesperado no cálculo: %v", err)
	}

	if res.DiasConcedidos != 3 {
		t.Errorf("esperava 3 dias concedidos, obteve %d", res.DiasConcedidos)
	}

	// 05/11 (Qui) -> 06/11 (Sex, dia 1) -> 07/11 (Sáb, pula) -> 08/11 (Dom, pula) -> 09/11 (Seg, dia 2) -> 10/11 (Ter, dia 3)
	esperado := time.Date(2026, 11, 10, 23, 59, 59, 0, time.UTC)
	if !res.DataDevolucaoPrevista.Equal(esperado) {
		t.Errorf("data prevista divergente. Esperava %v, obteve %v", esperado, res.DataDevolucaoPrevista)
	}
}

func TestCalcularPrazoMonitorEProfessor(t *testing.T) {
	calc := NovoCalculador(nil)
	dataInicio := time.Date(2026, 11, 4, 8, 0, 0, 0, time.UTC) // Quarta

	casos := []struct {
		perfil       string
		diasEsperado int
	}{
		{"MONITOR", 7},
		{"PROFESSOR", 15},
		{"aluno", 3}, // Testa case-insensitive
	}

	for _, c := range casos {
		t.Run(c.perfil, func(t *testing.T) {
			res, err := calc.CalcularPrazo(context.Background(), SolicitacaoCalculo{
				UsuarioID:     "u1",
				PerfilUsuario: c.perfil,
				EquipamentoID: "e1",
				DataRetirada:  dataInicio,
			})
			if err != nil {
				t.Fatalf("erro inesperado: %v", err)
			}
			if res.DiasConcedidos != c.diasEsperado {
				t.Errorf("para %s esperava %d dias, obteve %d", c.perfil, c.diasEsperado, res.DiasConcedidos)
			}
		})
	}
}

func TestCalcularPrazoComFeriados(t *testing.T) {
	// Cria calendário acadêmico com feriado do Dia do Servidor (30/10) e Finados (02/11)
	cal := NovoCalendarioAcademico()
	calc := NovoCalculador(cal)

	// Quarta-feira, 28/10/2026
	dataInicio := time.Date(2026, 10, 28, 14, 0, 0, 0, time.UTC)

	sol := SolicitacaoCalculo{
		UsuarioID:     "aluno-1",
		PerfilUsuario: "ALUNO", // 3 dias úteis
		EquipamentoID: "eq-1",
		DataRetirada:  dataInicio,
	}

	res, err := calc.CalcularPrazo(context.Background(), sol)
	if err != nil {
		t.Fatalf("erro inesperado: %v", err)
	}

	// 28/10 (Qua)
	// 29/10 (Qui) -> dia 1
	// 30/10 (Sex) -> feriado UFRN (pula)
	// 31/10 (Sáb) e 01/11 (Dom) -> fim de semana (pula)
	// 02/11 (Seg) -> feriado Finados (pula)
	// 03/11 (Ter) -> dia 2
	// 04/11 (Qua) -> dia 3
	esperado := time.Date(2026, 11, 4, 23, 59, 59, 0, time.UTC)
	if !res.DataDevolucaoPrevista.Equal(esperado) {
		t.Errorf("esperava devolução em %v, obteve %v", esperado, res.DataDevolucaoPrevista)
	}
}

func TestCalcularPrazoDadosInvalidos(t *testing.T) {
	calc := NovoCalculador(nil)

	casos := []struct {
		nome string
		sol  SolicitacaoCalculo
	}{
		{"sem usuario", SolicitacaoCalculo{PerfilUsuario: "ALUNO", EquipamentoID: "eq1"}},
		{"sem equipamento", SolicitacaoCalculo{UsuarioID: "u1", PerfilUsuario: "ALUNO"}},
		{"sem perfil", SolicitacaoCalculo{UsuarioID: "u1", EquipamentoID: "eq1"}},
	}

	for _, c := range casos {
		t.Run(c.nome, func(t *testing.T) {
			_, err := calc.CalcularPrazo(context.Background(), c.sol)
			if !errors.Is(err, ErrDadosInvalidos) {
				t.Errorf("esperava ErrDadosInvalidos, obteve %v", err)
			}
		})
	}
}

func TestCalcularPrazoPerfilDesconhecido(t *testing.T) {
	calc := NovoCalculador(nil)
	sol := SolicitacaoCalculo{
		UsuarioID:     "u1",
		PerfilUsuario: "EXTERNO",
		EquipamentoID: "eq1",
	}

	_, err := calc.CalcularPrazo(context.Background(), sol)
	if !errors.Is(err, ErrPerfilDesconhecido) {
		t.Errorf("esperava ErrPerfilDesconhecido, obteve %v", err)
	}
}

func TestCalcularPrazoRespeitaCancelamentoContexto(t *testing.T) {
	calc := NovoCalculador(nil)
	ctx, cancel := context.WithCancel(context.Background())
	cancel() // Cancela antes de chamar

	sol := SolicitacaoCalculo{
		UsuarioID:     "u1",
		PerfilUsuario: "ALUNO",
		EquipamentoID: "eq1",
	}

	_, err := calc.CalcularPrazo(ctx, sol)
	if !errors.Is(err, context.Canceled) {
		t.Errorf("esperava context.Canceled, obteve %v", err)
	}
}

func TestCalcularPrazoRespeitaTimeoutContexto(t *testing.T) {
	calc := NovoCalculador(nil)
	// Timeout já vencido
	ctx, cancel := context.WithDeadline(context.Background(), time.Now().Add(-1*time.Minute))
	defer cancel()

	sol := SolicitacaoCalculo{
		UsuarioID:     "u1",
		PerfilUsuario: "ALUNO",
		EquipamentoID: "eq1",
	}

	_, err := calc.CalcularPrazo(ctx, sol)
	if !errors.Is(err, context.DeadlineExceeded) {
		t.Errorf("esperava context.DeadlineExceeded, obteve %v", err)
	}
}

