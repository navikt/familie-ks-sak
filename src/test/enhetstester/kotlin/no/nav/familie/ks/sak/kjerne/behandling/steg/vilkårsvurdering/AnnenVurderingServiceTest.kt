package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ks.sak.api.dto.AnnenVurderingDto
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.AnnenVurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.AnnenVurderingRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.AnnenVurderingType
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.util.Optional

@ExtendWith(MockKExtension::class)
class AnnenVurderingServiceTest {
    @MockK
    private lateinit var annenVurderingRepository: AnnenVurderingRepository

    @InjectMockKs
    private lateinit var annenVurderingService: AnnenVurderingService

    @Test
    fun `endreAnnenVurdering - skal kaste feil dersom AnnenVurdering med forespurt id ikke finnes i db`() {
        every { annenVurderingRepository.findById(any()) } returns Optional.ofNullable(null)

        val annenVurderingDto =
            AnnenVurderingDto(404L, Resultat.OPPFYLT, AnnenVurderingType.OPPLYSNINGSPLIKT, "Begrunnelse")
        val funksjonellFeil =
            assertThrows<FunksjonellFeil> {
                annenVurderingService.endreAnnenVurdering(
                    annenVurderingDto,
                )
            }

        assertEquals("Annen vurdering med id ${annenVurderingDto.id} finnes ikke i db", funksjonellFeil.message)
    }

    @Test
    fun `endreAnnenVurdering - endre AnnenVurdering med forespurt id`() {
        val endretAnnenVurderingSlot = slot<AnnenVurdering>()
        val eksisterendeAnnenVurdering =
            AnnenVurdering(
                200L,
                PersonResultat(
                    vilkårsvurdering = Vilkårsvurdering(behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)),
                    aktør = randomAktør(),
                ),
                type = AnnenVurderingType.OPPLYSNINGSPLIKT,
                resultat = Resultat.IKKE_VURDERT,
                begrunnelse = null,
            )
        every { annenVurderingRepository.findById(any()) } returns Optional.of(eksisterendeAnnenVurdering)
        every { annenVurderingRepository.save(capture(endretAnnenVurderingSlot)) } returns mockk()

        val annenVurderingDto =
            AnnenVurderingDto(200L, Resultat.OPPFYLT, AnnenVurderingType.OPPLYSNINGSPLIKT, "Begrunnelse")

        annenVurderingService.endreAnnenVurdering(
            annenVurderingDto,
        )

        val endretAnnenVurdering = endretAnnenVurderingSlot.captured

        assertEquals(Resultat.OPPFYLT, endretAnnenVurdering.resultat)
        assertEquals("Begrunnelse", endretAnnenVurdering.begrunnelse)
    }
}
