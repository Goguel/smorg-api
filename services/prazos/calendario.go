package prazos

import "time"

// Calendario determina se uma data específica é considerada dia útil acadêmico.
// A interface é enxuta e declarada no domínio por quem a consome.
type Calendario interface {
	EhDiaUtil(data time.Time) bool
}

// CalendarioAcademico é a implementação padrão de Calendario para a UFRN.
// Desconsidera finais de semana (sábados e domingos) e feriados nacionais e acadêmicos.
type CalendarioAcademico struct {
	feriados map[string]bool
}

// NovoCalendarioAcademico cria uma instância de CalendarioAcademico com feriados padrão.
func NovoCalendarioAcademico(feriadosCustomizados ...time.Time) *CalendarioAcademico {
	f := make(map[string]bool)

	// Feriados padrão UFRN / Nacionais do semestre 2026.2 (conforme cronograma acadêmico)
	padroes := []string{
		"2026-09-07", // Independência do Brasil
		"2026-10-12", // Nossa Senhora Aparecida
		"2026-10-30", // Dia do Servidor Público (UFRN)
		"2026-11-02", // Finados
		"2026-11-15", // Proclamação da República
		"2026-11-20", // Consciência Negra
		"2026-12-25", // Natal
	}
	for _, dataStr := range padroes {
		f[dataStr] = true
	}
	for _, t := range feriadosCustomizados {
		f[t.Format("2006-01-02")] = true
	}

	return &CalendarioAcademico{feriados: f}
}

// EhDiaUtil verifica se a data informada não é sábado, domingo nem feriado cadastrado.
func (c *CalendarioAcademico) EhDiaUtil(data time.Time) bool {
	diaSemana := data.Weekday()
	if diaSemana == time.Saturday || diaSemana == time.Sunday {
		return false
	}
	dataStr := data.Format("2006-01-02")
	return !c.feriados[dataStr]
}
