package no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag

import no.nav.familie.kontrakter.felles.personopplysning.OPPHOLDSTILLATELSE
import no.nav.familie.kontrakter.felles.personopplysning.Opphold
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.api.mapper.RegisterHistorikkMapper
import no.nav.familie.ks.sak.fake.FakePersonopplysningerService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Målform
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlagRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class PersonopplysningGrunnlagServiceIntegrasjonTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var personopplysningGrunnlagService: PersonopplysningGrunnlagService

    @Autowired
    private lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    @Test
    fun `skal beholde aktivt grunnlag og oppdatere endretTidspunkt når registeropplysningene er uendret`() {
        // Arrange
        opprettSøkerFagsakOgBehandling()
        val forrigeGrunnlag =
            personopplysningGrunnlagService.lagreSøkerOgBarnINyttGrunnlag(
                aktør = søker,
                behandling = behandling,
                målform = Målform.NB,
                barnasAktør = listOf(barn),
            )
        val endretTidspunktFørOppdatering = personopplysningGrunnlagRepository.findByBehandling(behandling.id)!!.endretTidspunkt

        // Act
        val oppdatertGrunnlag = personopplysningGrunnlagService.oppdaterRegisteropplysningerPåBehandling(behandling)

        // Assert
        val aktivtGrunnlag = personopplysningGrunnlagRepository.findByBehandling(behandling.id)!!
        assertThat(oppdatertGrunnlag.id).isEqualTo(forrigeGrunnlag.id)
        assertThat(aktivtGrunnlag.id).isEqualTo(forrigeGrunnlag.id)
        assertThat(aktivtGrunnlag.endretTidspunkt).isAfter(endretTidspunktFørOppdatering)
        assertThat(RegisterHistorikkMapper.lagRegisterHistorikkResponsDto(aktivtGrunnlag.søker, null).hentetTidspunkt).isEqualTo(aktivtGrunnlag.endretTidspunkt)
    }

    @Test
    fun `skal beholde aktivt grunnlag når opphold og nordisk statsborgerskap uten datoer er uendret`() {
        // Arrange
        opprettSøkerFagsakOgBehandling()
        FakePersonopplysningerService.personInfo[søker.aktivFødselsnummer()] =
            FakePersonopplysningerService.mockSøkerBehandling.copy(
                opphold = listOf(Opphold(type = OPPHOLDSTILLATELSE.OPPLYSNING_MANGLER, oppholdFra = null, oppholdTil = null)),
                statsborgerskap = listOf(Statsborgerskap(land = "NOR", gyldigFraOgMed = null, bekreftelsesdato = null, gyldigTilOgMed = null)),
            )
        val forrigeGrunnlag =
            personopplysningGrunnlagService.lagreSøkerOgBarnINyttGrunnlag(
                aktør = søker,
                behandling = behandling,
                målform = Målform.NB,
                barnasAktør = listOf(barn),
            )

        // Act
        val oppdatertGrunnlag = personopplysningGrunnlagService.oppdaterRegisteropplysningerPåBehandling(behandling)

        // Assert
        assertThat(forrigeGrunnlag.søker.opphold).hasSize(1)
        assertThat(forrigeGrunnlag.søker.statsborgerskap).hasSize(1)
        assertThat(oppdatertGrunnlag.id).isEqualTo(forrigeGrunnlag.id)
    }
}
