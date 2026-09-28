CREATE TABLE equipamentos (
    id UUID PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    nfc_tag_id VARCHAR(50) UNIQUE,
    status VARCHAR(50) NOT NULL,
    categoria VARCHAR(50) NOT NULL
);
