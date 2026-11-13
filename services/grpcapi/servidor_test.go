package grpcapi

import (
	"context"
	"net"
	"testing"
	"time"

	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/credentials/insecure"
	"google.golang.org/grpc/status"
	"google.golang.org/grpc/test/bufconn"

	prazosv1 "smorg/services/gen/smorg/prazos/v1"
	"smorg/services/prazos"
)

// cliente conecta o stub gRPC a um servidor em memória através de bufconn.
func cliente(t *testing.T) prazosv1.PrazoServiceClient {
	t.Helper()
	lis := bufconn.Listen(1024 * 1024)
	srv := grpc.NewServer()

	calc := prazos.NovoCalculador(prazos.NovoCalendarioAcademico())
	prazosv1.RegisterPrazoServiceServer(srv, NovoServidor(calc))

	go func() {
		_ = srv.Serve(lis)
	}()
	t.Cleanup(srv.Stop)

	conn, err := grpc.NewClient("passthrough:///bufnet",
		grpc.WithContextDialer(func(context.Context, string) (net.Conn, error) { return lis.Dial() }),
		grpc.WithTransportCredentials(insecure.NewCredentials()))
	if err != nil {
		t.Fatalf("falha ao conectar bufconn: %v", err)
	}
	t.Cleanup(func() { _ = conn.Close() })

	return prazosv1.NewPrazoServiceClient(conn)
}

func TestCalcularPrazoPorGRPC(t *testing.T) {
	c := cliente(t)

	// Quinta-feira, 05/11/2026 às 10:00:00Z
	dataRetirada := "2026-11-05T10:00:00Z"
	req := &prazosv1.CalcularPrazoDevolucaoRequest{
		UsuarioId:            "usr-001",
		PerfilUsuario:        "ALUNO",
		EquipamentoId:        "eqp-999",
		CategoriaEquipamento: "MULTIMETRO",
		DataRetirada:         dataRetirada,
	}

	resp, err := c.CalcularPrazoDevolucao(context.Background(), req)
	if err != nil {
		t.Fatalf("erro inesperado na chamada gRPC: %v", err)
	}

	if resp.GetDiasConcedidos() != 3 {
		t.Errorf("esperava 3 dias concedidos, veio %d", resp.GetDiasConcedidos())
	}

	if resp.GetDataDevolucaoPrevista() == "" {
		t.Error("data_devolucao_prevista não pode ser vazia")
	}

	// 05/11 (Qui) -> 06/11 (Sex, 1) -> fim de semana -> 09/11 (Seg, 2) -> 10/11 (Ter, 3) às 23:59:59Z
	esperado := "2026-11-10T23:59:59Z"
	if resp.GetDataDevolucaoPrevista() != esperado {
		t.Errorf("esperava data prevista %s, veio %s", esperado, resp.GetDataDevolucaoPrevista())
	}
}

func TestCalcularPrazoInvalidoRetornaInvalidArgument(t *testing.T) {
	c := cliente(t)

	casos := []struct {
		nome string
		req  *prazosv1.CalcularPrazoDevolucaoRequest
	}{
		{
			"sem usuario",
			&prazosv1.CalcularPrazoDevolucaoRequest{PerfilUsuario: "ALUNO", EquipamentoId: "eq-1"},
		},
		{
			"perfil desconhecido",
			&prazosv1.CalcularPrazoDevolucaoRequest{UsuarioId: "u1", PerfilUsuario: "CONVIDADO", EquipamentoId: "eq-1"},
		},
	}

	for _, tc := range casos {
		t.Run(tc.nome, func(t *testing.T) {
			_, err := c.CalcularPrazoDevolucao(context.Background(), tc.req)
			if status.Code(err) != codes.InvalidArgument {
				t.Errorf("esperava código INVALID_ARGUMENT, veio %v (err: %v)", status.Code(err), err)
			}
		})
	}
}

func TestCalcularPrazoRespeitaDeadline(t *testing.T) {
	c := cliente(t)

	// Contexto com deadline curto ou já vencido
	ctx, cancel := context.WithDeadline(context.Background(), time.Now().Add(-10*time.Millisecond))
	defer cancel()

	req := &prazosv1.CalcularPrazoDevolucaoRequest{
		UsuarioId:     "usr-001",
		PerfilUsuario: "ALUNO",
		EquipamentoId: "eqp-001",
	}

	_, err := c.CalcularPrazoDevolucao(ctx, req)
	if status.Code(err) != codes.DeadlineExceeded {
		t.Errorf("esperava código DEADLINE_EXCEEDED, veio %v (err: %v)", status.Code(err), err)
	}
}

