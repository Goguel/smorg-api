// Package grpcapi é o adaptador gRPC do microsserviço: traduz entre as mensagens
// geradas pelo contrato Protocol Buffers e o domínio puro do pacote prazos.
// O domínio puro não possui dependências deste pacote.
package grpcapi

import (
	"context"
	"errors"
	"time"

	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"

	prazosv1 "smorg/services/gen/smorg/prazos/v1"
	"smorg/services/prazos"
)

// folga: o servidor encerra o processamento 50ms antes do prazo do chamador para garantir
// que a resposta ou o erro estruturado ainda chegue antes do cliente dar timeout na conexão TCP.
const folga = 50 * time.Millisecond

// Servidor implementa prazosv1.PrazoServiceServer.
type Servidor struct {
	prazosv1.UnimplementedPrazoServiceServer
	calculador *prazos.Calculador
}

// NovoServidor instancia o adaptador gRPC sobre o motor de regras de prazos.
func NovoServidor(calc *prazos.Calculador) *Servidor {
	return &Servidor{calculador: calc}
}

// CalcularPrazoDevolucao recebe a requisição gRPC, aciona o domínio e traduz os resultados e erros.
func (s *Servidor) CalcularPrazoDevolucao(ctx context.Context, req *prazosv1.CalcularPrazoDevolucaoRequest) (*prazosv1.CalcularPrazoDevolucaoResponse, error) {
	// 1. Respeito ao deadline com margem de folga
	if deadline, ok := ctx.Deadline(); ok {
		var cancelar context.CancelFunc
		ctx, cancelar = context.WithDeadline(ctx, deadline.Add(-folga))
		defer cancelar()
	}

	// 2. Extração e conversão dos parâmetros
	var dataRetirada time.Time
	if req.GetDataRetirada() != "" {
		parsed, err := time.Parse(time.RFC3339, req.GetDataRetirada())
		if err == nil {
			dataRetirada = parsed
		}
	}

	sol := prazos.SolicitacaoCalculo{
		UsuarioID:            req.GetUsuarioId(),
		PerfilUsuario:        req.GetPerfilUsuario(),
		EquipamentoID:        req.GetEquipamentoId(),
		CategoriaEquipamento: req.GetCategoriaEquipamento(),
		DataRetirada:         dataRetirada,
	}

	// 3. Execução das regras de negócio no domínio puro
	res, err := s.calculador.CalcularPrazo(ctx, sol)
	if err != nil {
		switch {
		case errors.Is(err, prazos.ErrDadosInvalidos), errors.Is(err, prazos.ErrPerfilDesconhecido):
			return nil, status.Error(codes.InvalidArgument, err.Error())
		case errors.Is(err, context.DeadlineExceeded):
			return nil, status.Error(codes.DeadlineExceeded, "prazo limite para cálculo de devolução excedido")
		case errors.Is(err, context.Canceled):
			return nil, status.Error(codes.Canceled, "cálculo de devolução cancelado")
		default:
			return nil, status.Error(codes.Internal, "falha interna no processamento de prazos")
		}
	}

	// 4. Mapeamento para o DTO do contrato gRPC
	return &prazosv1.CalcularPrazoDevolucaoResponse{
		DataDevolucaoPrevista: res.DataDevolucaoPrevista.Format(time.RFC3339),
		DiasConcedidos:        int32(res.DiasConcedidos),
		Observacoes:           res.Observacoes,
	}, nil
}

