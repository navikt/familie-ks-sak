package no.nav.familie.ks.sak.kjerne.behandling.steg

import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.data.shouldNotBeNull
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakStatus
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Målform
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlagRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired

class RegistrerPersonGrunnlagStegTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var registrerPersonGrunnlagSteg: RegistrerPersonGrunnlagSteg

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    @Autowired
    private lateinit var vilkårsvurderingService: VilkårsvurderingService

    @BeforeEach
    fun init() {
        opprettSøkerFagsakOgBehandling(fagsakStatus = FagsakStatus.LØPENDE)
    }

    @Test
    fun `utførSteg skal utføre REGISTRERE_PERSONGRUNNLAG steg for FGB`() {
        assertDoesNotThrow { registrerPersonGrunnlagSteg.utførSteg(behandling.id) }

        val personopplysningGrunnlag =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id).shouldNotBeNull()
        assertEquals(1, personopplysningGrunnlag.personer.size)

        val person = personopplysningGrunnlag.personer.single()
        assertEquals(behandling.fagsak.aktør, person.aktør)
        assertEquals(Målform.NB, person.målform)
        assertEquals(PersonType.SØKER, person.type)

        assertTrue(person.sivilstander.isNotEmpty())
        assertTrue(person.bostedsadresser.isNotEmpty())
        assertTrue(person.statsborgerskap.isEmpty())
    }

    @Test
    fun `utførSteg skal utføre REGISTRERE_PERSONGRUNNLAG steg for Revurdering med opprettet årsak søknad`() {
        val barnAktør = lagreAktør(randomAktør())

        val gammelPersonopplysningGrunnlag =
            lagPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = behandling.fagsak.aktør.aktivFødselsnummer(),
                søkerAktør = behandling.fagsak.aktør,
                barnasIdenter = listOf(barnAktør.aktivFødselsnummer()),
                barnAktør = listOf(barnAktør),
            )
        personopplysningGrunnlagRepository.save(gammelPersonopplysningGrunnlag)

        lagreBehandling(
            behandling.also {
                it.status = BehandlingStatus.AVSLUTTET
                it.aktiv = false
            },
        )

        val revurdering =
            behandlingRepository.saveAndFlush(
                lagBehandling(
                    fagsak = behandling.fagsak,
                    type = BehandlingType.REVURDERING,
                    opprettetÅrsak = BehandlingÅrsak.SØKNAD,
                ),
            )
        assertDoesNotThrow { registrerPersonGrunnlagSteg.utførSteg(revurdering.id) }

        val personopplysningGrunnlag =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(revurdering.id).shouldNotBeNull()
        assertEquals(1, personopplysningGrunnlag.personer.size)

        val søker = personopplysningGrunnlag.personer.single { it.type == PersonType.SØKER }
        assertEquals(revurdering.fagsak.aktør, søker.aktør)
        assertEquals(Målform.NB, søker.målform)

        assertTrue(søker.sivilstander.isNotEmpty())
        assertTrue(søker.bostedsadresser.isNotEmpty())
        assertTrue(søker.statsborgerskap.isEmpty())

        assertTrue { vilkårsvurderingService.finnAktivVilkårsvurdering(revurdering.id) == null }
    }

    @Test
    fun `utførSteg skal utføre REGISTRERE_PERSONGRUNNLAG steg for Revurdering med opprettet årsak ÅRLIG_KONTROLL`() {
        val barnAktør = lagreAktør(randomAktør())

        val gammelPersonopplysningGrunnlag =
            lagPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = behandling.fagsak.aktør.aktivFødselsnummer(),
                søkerAktør = behandling.fagsak.aktør,
                barnasIdenter = listOf(randomFnr()),
                barnAktør = listOf(barnAktør),
            )
        personopplysningGrunnlagRepository.save(gammelPersonopplysningGrunnlag)
        vilkårsvurderingService.opprettVilkårsvurdering(behandling, null)

        lagreBehandling(
            behandling.also {
                it.status = BehandlingStatus.AVSLUTTET
                it.aktiv = false
            },
        )

        val revurdering =
            behandlingRepository.saveAndFlush(
                lagBehandling(
                    fagsak = behandling.fagsak,
                    type = BehandlingType.REVURDERING,
                    opprettetÅrsak = BehandlingÅrsak.ÅRLIG_KONTROLL,
                ),
            )
        assertDoesNotThrow { registrerPersonGrunnlagSteg.utførSteg(revurdering.id) }

        val personopplysningGrunnlag =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(revurdering.id).shouldNotBeNull()
        assertEquals(1, personopplysningGrunnlag.personer.size)

        val søker = personopplysningGrunnlag.personer.single { it.type == PersonType.SØKER }
        assertEquals(revurdering.fagsak.aktør, søker.aktør)
        assertEquals(Målform.NB, søker.målform)

        assertTrue(søker.sivilstander.isNotEmpty())
        assertTrue(søker.bostedsadresser.isNotEmpty())
        assertTrue(søker.statsborgerskap.isEmpty())

        assertTrue { vilkårsvurderingService.finnAktivVilkårsvurdering(revurdering.id) != null }
    }
}
