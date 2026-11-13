# Imagem multi-stage do microsserviço de prazos em Go
# Compila com a imagem oficial do Go e transfere apenas o binário estático para distroless
FROM golang:1.23-alpine AS build
WORKDIR /src

COPY go.mod ./
COPY . .

# Compilação estática (CGO desativado) do subcomando cmd/prazos
RUN CGO_ENABLED=0 go build -ldflags="-s -w" -o /prazos ./cmd/prazos

# Imagem final estática mínima, sem shell e de alta segurança
FROM gcr.io/distroless/static-debian12
COPY --from=build /prazos /prazos
USER nonroot

ENV PORT=50051
EXPOSE 50051

ENTRYPOINT ["/prazos"]

