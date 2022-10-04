package no.nav.familie.ks.sak.no.nav.familie.ks.sak.kjerne.søknad.domene

import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.api.dto.BarnMedOpplysningerDto
import no.nav.familie.ks.sak.api.dto.SøkerMedOpplysningerDto
import no.nav.familie.ks.sak.api.dto.SøknadDto
import no.nav.familie.ks.sak.api.dto.tilSøknadGrunnlag
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.fagsak.domene.Fagsak
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakRepository
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personident.AktørRepository
import no.nav.familie.ks.sak.kjerne.søknad.domene.SøknadGrunnlagRepository
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.hamcrest.CoreMatchers.`is` as Is

class SøknadGrunnlagRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var søknadGrunnlagRepository: SøknadGrunnlagRepository

    @Autowired
    private lateinit var aktørRepository: AktørRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    private lateinit var søker: Aktør

    private lateinit var fagsak: Fagsak

    private lateinit var behandling: Behandling

    @BeforeEach
    fun beforeEach() {
        søker = lagreAktør(randomAktør())
        fagsak = lagreFagsak(søker)
        behandling = lagreBehandling(fagsak)
    }

    @Test
    fun `hentAktiv - skal hente aktiv SøknadGrunnlag tilknyttet behandlingId`() {
        val barn = lagreAktør(randomAktør())
        val søknadDto = lagreSøknadGrunnlag(behandling.id, listOf(barn), true)

        val søknadGrunnlag = søknadGrunnlagRepository.finnAktiv(behandling.id)

        assertNotNull(søknadGrunnlag)
        assertThat(søknadGrunnlag!!.behandlingId, Is(behandling.id))
        assertThat(søknadGrunnlag.søknad, Is(søknadDto.tilSøknadGrunnlag(behandling.id).søknad))
    }

    @Test
    fun `hentAktiv - skal returnere null dersom det ikke finnes et SøknadsGrunnlag tilknyttet behandlingId`() {
        val søknadGrunnlag = søknadGrunnlagRepository.finnAktiv(404L)

        assertNull(søknadGrunnlag)
    }

    @Test
    fun `hentAlle - skal returnere alle SøknadsGrunnlag tilknyttet behandlingId`() {
        lagreSøknadGrunnlag(behandling.id, listOf(randomAktør()))
        lagreSøknadGrunnlag(behandling.id, listOf(randomAktør()))
        lagreSøknadGrunnlag(behandling.id, listOf(randomAktør()), true)

        val søknadsGrunnlag = søknadGrunnlagRepository.hentAlle(behandling.id)
        assertEquals(3, søknadsGrunnlag.size)
    }

    fun lagreAktør(aktør: Aktør): Aktør {
        return aktørRepository.saveAndFlush(aktør)
    }

    fun lagreFagsak(søker: Aktør): Fagsak {
        return fagsakRepository.saveAndFlush(lagFagsak(aktør = søker))
    }

    fun lagreBehandling(fagsak: Fagsak): Behandling {
        return behandlingRepository.saveAndFlush(lagBehandling(fagsak, opprettetÅrsak = BehandlingÅrsak.SØKNAD))
    }

    fun lagreSøknadGrunnlag(behandlingId: Long, barna: List<Aktør>, aktiv: Boolean = false): SøknadDto {
        val søknadDto = SøknadDto(
            SøkerMedOpplysningerDto(ident = søker.aktivFødselsnummer()),
            barna.map { BarnMedOpplysningerDto(ident = it.aktivFødselsnummer()) },
            endringAvOpplysningerBegrunnelse = ""
        )
        søknadGrunnlagRepository.saveAndFlush(søknadDto.tilSøknadGrunnlag(behandlingId).also { it.aktiv = aktiv })

        return søknadDto
    }
}
