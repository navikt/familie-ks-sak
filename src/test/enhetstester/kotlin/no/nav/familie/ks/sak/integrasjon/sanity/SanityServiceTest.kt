package no.nav.familie.ks.sak.integrasjon.sanity

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelseType
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.NasjonalEllerFellesBegrunnelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SanityServiceTest {
    @MockK
    private lateinit var cachedSanityKlient: CachedSanityKlient

    @InjectMockKs
    private lateinit var sanityService: SanityService

    @BeforeEach
    fun setUp() {
    }

    @Test
    fun `Skal hente SanityBegrunnelser`() {
        every { cachedSanityKlient.hentSanityBegrunnelserCached() } returns
            listOf(
                SanityBegrunnelse(
                    NasjonalEllerFellesBegrunnelse.INNVILGET_IKKE_BARNEHAGE.sanityApiNavn,
                    "innvilgetIkkeBarnehage",
                    SanityBegrunnelseType.STANDARD,
                    Vilkår.values().toList(),
                    rolle = emptyList(),
                    triggere = emptyList(),
                    utdypendeVilkårsvurderinger = emptyList(),
                    hjemler = emptyList(),
                    endretUtbetalingsperiode = emptyList(),
                    endringsårsaker = emptyList(),
                    støtterFritekst = false,
                    skalAlltidVises = false,
                    resultat = SanityResultat.INNVILGET,
                ),
            )

        assertThat(sanityService.hentSanityBegrunnelser()).hasSize(1)
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
                    Vilkår.values().toList(),
                    rolle = emptyList(),
                    triggere = emptyList(),
                    utdypendeVilkårsvurderinger = emptyList(),
                    hjemler = emptyList(),
                    endretUtbetalingsperiode = emptyList(),
                    endringsårsaker = emptyList(),
                    støtterFritekst = false,
                    skalAlltidVises = false,
                    resultat = SanityResultat.INNVILGET,
                ),
            ) andThenThrows RuntimeException("Feil får å teste cachet versjon")

        assertThat(sanityService.hentSanityBegrunnelser()).hasSize(1)
    }
}
