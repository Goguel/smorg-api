// Comando prazos: o servidor gRPC do microsserviço de cálculo de prazos do SmOrg.
//
//	go run ./cmd/prazos                  # escuta na porta padrão 50051
//	PRAZOS_PORTA=6000 go run ./cmd/prazos
//	prazos saude                         # consulta a saúde do serviço localmente (health check gRPC)
package main

import (
	"context"
	"log/slog"
	"net"
	"os"
	"os/signal"
	"syscall"
	"time"

	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"
	"google.golang.org/grpc/health"
	healthpb "google.golang.org/grpc/health/grpc_health_v1"
	"google.golang.org/grpc/reflection"

	prazosv1 "smorg/services/gen/smorg/prazos/v1"
	"smorg/services/grpcapi"
	"smorg/services/prazos"
)

func main() {
	porta := os.Getenv("PRAZOS_PORTA")
	if porta == "" {
		porta = os.Getenv("PORT")
	}
	if porta == "" {
		porta = "50051"
	}

	// Subcomando de verificação de saúde local (usado pelo Docker health check sem precisar de curl)
	if len(os.Args) > 1 && os.Args[1] == "saude" {
		os.Exit(verificarSaude(porta))
	}

	lis, err := net.Listen("tcp", ":"+porta)
	if err != nil {
		slog.Error("falha ao abrir porta do servidor", "porta", porta, "erro", err)
		os.Exit(1)
	}

	srv := grpc.NewServer()

	// 1. Registro do serviço de prazos sobre o domínio puro
	calculador := prazos.NovoCalculador(prazos.NovoCalendarioAcademico())
	servidorGrpc := grpcapi.NovoServidor(calculador)
	prazosv1.RegisterPrazoServiceServer(srv, servidorGrpc)

	// 2. Health check padrão do gRPC (grpc.health.v1)
	saude := health.NewServer()
	healthpb.RegisterHealthServer(srv, saude)
	saude.SetServingStatus(prazosv1.PrazoService_ServiceDesc.ServiceName, healthpb.HealthCheckResponse_SERVING)
	saude.SetServingStatus("", healthpb.HealthCheckResponse_SERVING)

	// 3. Reflection: permite que grpcurl descubra contratos sem precisar do .proto em mãos
	reflection.Register(srv)

	// 4. Encerramento gracioso ao receber SIGINT ou SIGTERM
	go func() {
		sinal := make(chan os.Signal, 1)
		signal.Notify(sinal, syscall.SIGINT, syscall.SIGTERM)
		<-sinal
		slog.Info("recebido sinal de encerramento, finalizando graciosamente...")
		saude.Shutdown()
		srv.GracefulStop()
	}()

	slog.Info("microsserviço de prazos SmOrg iniciado", "porta", porta)
	if err := srv.Serve(lis); err != nil {
		slog.Error("servidor gRPC encerrado com erro", "erro", err)
		os.Exit(1)
	}
}

// verificarSaude conecta via gRPC ao próprio processo e checa se o status é SERVING.
func verificarSaude(porta string) int {
	conn, err := grpc.NewClient("localhost:"+porta, grpc.WithTransportCredentials(insecure.NewCredentials()))
	if err != nil {
		return 1
	}
	defer conn.Close()

	ctx, cancelar := context.WithTimeout(context.Background(), 2*time.Second)
	defer cancelar()

	resp, err := healthpb.NewHealthClient(conn).Check(ctx, &healthpb.HealthCheckRequest{
		Service: prazosv1.PrazoService_ServiceDesc.ServiceName,
	})
	if err != nil || resp.GetStatus() != healthpb.HealthCheckResponse_SERVING {
		return 1
	}
	return 0
}

