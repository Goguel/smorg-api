CREATE TABLE emprestimos (
    id UUID PRIMARY KEY,
    usuario_id UUID NOT NULL,
    equipamento_id UUID NOT NULL REFERENCES equipamentos(id),
    data_retirada VARCHAR(50) NOT NULL,
    data_devolucao_prevista VARCHAR(50) NOT NULL,
    data_devolucao_real VARCHAR(50),
    status VARCHAR(50) NOT NULL,
    devolvido_com_atraso BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_emprestimos_equipamento_id ON emprestimos(equipamento_id);
CREATE INDEX idx_emprestimos_usuario_id ON emprestimos(usuario_id);
CREATE INDEX idx_emprestimos_status ON emprestimos(status);
