package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode

import io.mockk.mockk
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeMedBegrunnelser
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.hamcrest.CoreMatchers.`is` as Is

class VedtaksperiodeUtilTest {
    @ParameterizedTest
    @EnumSource(
        value = Vedtaksperiodetype::class,
        names = ["OPPHØR", "AVSLAG"],
        mode = EnumSource.Mode.INCLUDE,
    )
    fun `validerVedtaksperiodeMedBegrunnelser - skal kaste FunksjonellFeil dersom det er fritekst uten stanadard begrunnelser i opphør eller avslag`(
        vedtaksperiodetype: Vedtaksperiodetype,
    ) {
        val vedtaksperiodeMedBegrunnelser =
            VedtaksperiodeMedBegrunnelser(
                vedtak = mockk(),
                type = vedtaksperiodetype,
                fritekster = mutableListOf(mockk()),
            )

        val funksjonellFeil =
            assertThrows<FunksjonellFeil> {
                validerVedtaksperiodeMedBegrunnelser(vedtaksperiodeMedBegrunnelser)
            }

        assertThat(
            funksjonellFeil.message,
            Is(
                "Fritekst kan kun brukes i kombinasjon med en eller flere begrunnelser. Legg først til en ny begrunnelse eller fjern friteksten(e).",
            ),
        )
    }
}
