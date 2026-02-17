package no.nav.familie.ks.sak.kjerne.brev.begrunnelser

import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.ks.sak.api.dto.EndretUtbetalingAndelRequestDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class IBegrunnelseDeserializerTest {
    @Test
    fun `deserialiserer liste av enumverdier til IBegrunnelse`() {
        val testdata =
            EndretUtbetalingAndelRequestDto(
                id = 1L,
                personIdent = "12345678903",
                personIdenter = listOf("12345678903"),
                prosent = java.math.BigDecimal(100),
                fom = java.time.YearMonth.of(2023, 1),
                tom = java.time.YearMonth.of(2023, 12),
                årsak = no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.Årsak.ALLEREDE_UTBETALT,
                søknadstidspunkt = java.time.LocalDate.of(2023, 1, 1),
                begrunnelse = "en begrunnelse",
                erEksplisittAvslagPåSøknad = false,
                vedtaksbegrunnelser =
                    listOf(
                        NasjonalEllerFellesBegrunnelse.AVSLAG_ENDRINGSPERIODE_ALLEREDE_UTBETALT_SØKER,
                        NasjonalEllerFellesBegrunnelse.AVSLAG_ENDRINGSPERIODE_ALLEREDE_UTBETALT_ANNEN_FORELDER,
                    ),
            )
        val json = jsonMapper.writeValueAsString(testdata)
        val forventetJson = """{"id":1,"personIdent":"12345678903","personIdenter":["12345678903"],"prosent":100,"fom":"2023-01","tom":"2023-12","årsak":"ALLEREDE_UTBETALT","søknadstidspunkt":"2023-01-01","begrunnelse":"en begrunnelse","erEksplisittAvslagPåSøknad":false,"vedtaksbegrunnelser":["NasjonalEllerFellesBegrunnelse${'$'}AVSLAG_ENDRINGSPERIODE_ALLEREDE_UTBETALT_SØKER","NasjonalEllerFellesBegrunnelse${'$'}AVSLAG_ENDRINGSPERIODE_ALLEREDE_UTBETALT_ANNEN_FORELDER"]}"""
        assertEquals(forventetJson, json)
        assertEquals(testdata, jsonMapper.readValue(json, EndretUtbetalingAndelRequestDto::class.java))
    }
}
