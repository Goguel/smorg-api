package com.smorg.arquitetura.violacao.domain

import io.ktor.http.HttpStatusCode

/**
 * Fixture de teste para validação de falsos negativos no teste de arquitetura ArchUnit.
 * Esta classe propositalmente reside em um pacote 'domain' e importa o framework Ktor
 * para comprovar que a regra de dependência detecta a violação.
 */
class Contaminado {
    fun status(): HttpStatusCode = HttpStatusCode.OK
}

