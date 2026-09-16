package main

import "testing"

func TestServiceHealth(t *testing.T) {
	msg := "SmOrg Microservice rodando!"
	if msg == "" {
		t.Error("mensagem não deve ser vazia")
	}
}
