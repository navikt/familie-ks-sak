package no.nav.familie.ks.sak.integrasjon.sanity

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelseType
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.NasjonalEllerFellesBegrunnelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SanityServiceTest {
    private val cachedSanityKlient = mockk<CachedSanityKlient>()

    private val sanityService = SanityService(cachedSanityKlient)

    @Test
    fun `Skal hente SanityBegrunnelser`() {
        every { cachedSanityKlient.hentSanityBegrunnelserCached() } returns
            listOf(
                SanityBegrunnelse(
                    NasjonalEllerFellesBegrunnelse.INNVILGET_IKKE_BARNEHAGE.sanityApiNavn,
                    "innvilgetIkkeBarnehage",
                    SanityBegrunnelseType.STANDARD,
                    Vilkår.entries,
                    rolle = emptyList(),
                    triggere = emptyList(),
                    utdypendeVilkårsvurderinger = emptyList(),
                    hjemler = emptyList(),
                    endretUtbetalingsperiode = emptyList(),
                    endringsårsaker = emptyList(),
                    støtterFritekst = false,
                    skalAlltidVises = false,
                    ikkeIBruk = false,
                    resultat = SanityResultat.INNVILGET,
                ),
            )

        assertThat(sanityService.hentSanityBegrunnelser()).hasSize(1)
    }

    @Test
    fun `Skal ikke returnere SanityBegrunnelser som er markert ikke i bruk`() {
        every { cachedSanityKlient.hentSanityBegrunnelserCached() } returns
            listOf(
                SanityBegrunnelse(
                    NasjonalEllerFellesBegrunnelse.INNVILGET_IKKE_BARNEHAGE.sanityApiNavn,
                    "innvilgetIkkeBarnehage",
                    SanityBegrunnelseType.STANDARD,
                    Vilkår.entries,
                    rolle = emptyList(),
                    triggere = emptyList(),
                    utdypendeVilkårsvurderinger = emptyList(),
                    hjemler = emptyList(),
                    endretUtbetalingsperiode = emptyList(),
                    endringsårsaker = emptyList(),
                    støtterFritekst = false,
                    skalAlltidVises = false,
                    ikkeIBruk = true,
                    resultat = SanityResultat.INNVILGET,
                ),
            )

        assertThat(sanityService.hentSanityBegrunnelser()).isEmpty()
    }

    @Test
    fun `Skal kaste feil hvis det ikke er mulig å hente fra Sanity eller finnes i Cache`() {
        every { cachedSanityKlient.hentSanityBegrunnelserCached() } throws RuntimeException("Feil ved henting av begrunnelser i test")

        val e = assertThrows<RuntimeException> { sanityService.hentSanityBegrunnelser() }
        assertThat(e.message).isEqualTo("Feil ved henting av begrunnelser i test")
    }

    @Test
    fun `Skal bruke allerede lagret cache hvis det feiler mot sanity`() {
        every { cachedSanityKlient.hentSanityBegrunnelserCached() } returns
            listOf(
                SanityBegrunnelse(
                    NasjonalEllerFellesBegrunnelse.INNVILGET_IKKE_BARNEHAGE.sanityApiNavn,
                    "innvilgetIkkeBarnehage",
                    SanityBegrunnelseType.STANDARD,
                    Vilkår.entries,
                    rolle = emptyList(),
                    triggere = emptyList(),
                    utdypendeVilkårsvurderinger = emptyList(),
                    hjemler = emptyList(),
                    endretUtbetalingsperiode = emptyList(),
                    endringsårsaker = emptyList(),
                    støtterFritekst = false,
                    skalAlltidVises = false,
                    ikkeIBruk = false,
                    resultat = SanityResultat.INNVILGET,
                ),
            ) andThenThrows RuntimeException("Feil får å teste cachet versjon")

        assertThat(sanityService.hentSanityBegrunnelser()).hasSize(1)
    }
}
