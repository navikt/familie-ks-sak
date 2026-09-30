package no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag

import no.nav.familie.kontrakter.felles.personopplysning.OPPHOLDSTILLATELSE
import no.nav.familie.kontrakter.felles.personopplysning.Oppholdsadresse
import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagBostedsadresse
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.lagSivilstand
import no.nav.familie.ks.sak.data.lagStatsborgerskap
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Kjønn
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Medlemskap
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlagRepository
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.arbeidsforhold.GrArbeidsforhold
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.bostedsadresse.GrBostedsadresse
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.dødsfall.Dødsfall
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.opphold.GrOpphold
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.oppholdsadresse.GrOppholdsadresse
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.sivilstand.GrSivilstand
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.statsborgerskap.GrStatsborgerskap
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class SlettInaktivePersonopplysningsgrunnlagServiceTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var slettInaktivePersonopplysningsgrunnlagService: SlettInaktivePersonopplysningsgrunnlagService

    @Autowired
    private lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    @Test
    fun `skal slette inaktivt grunnlag når behandlingen har et aktivt grunnlag`() {
        // Arrange
        opprettSøkerFagsakOgBehandling()
        opprettPersonopplysningGrunnlagOgPersonForBehandling()
        val inaktivtGrunnlag = lagreInaktivtGrunnlagMedPerson(behandling.id)

        // Act
        val slettetBatch = slettInaktivePersonopplysningsgrunnlagService.slettBatchMedInaktiveGrunnlag(etterId = 0, batchStørrelse = 200)

        // Assert
        assertThat(slettetBatch.grunnlagIder).containsExactly(inaktivtGrunnlag.id)
        assertThat(slettetBatch.antallSlettedeRaderPerTabell).containsExactly(
            entry("gr_personopplysninger", 1L),
            entry("po_person", 1L),
            entry("po_statsborgerskap", 1L),
            entry("po_opphold", 1L),
            entry("po_arbeidsforhold", 1L),
            entry("po_sivilstand", 1L),
            entry("po_bostedsadresse", 1L),
            entry("po_doedsfall", 1L),
            entry("po_oppholdsadresse", 1L),
        )
        assertThat(personopplysningGrunnlagRepository.findById(inaktivtGrunnlag.id)).isEmpty
        assertThat(personopplysningGrunnlagRepository.findById(personopplysningGrunnlag.id)).isPresent
    }

    @Test
    fun `skal ikke slette inaktivt grunnlag når behandlingen ikke har noe aktivt grunnlag`() {
        // Arrange
        val behandlingUtenAktivtGrunnlag = lagreBehandling(lagBehandling(fagsak = lagreFagsak(lagFagsak(aktør = lagreAktør(randomAktør())))))
        val inaktivtGrunnlag = lagreInaktivtGrunnlagMedPerson(behandlingUtenAktivtGrunnlag.id)

        // Act
        val slettetBatch = slettInaktivePersonopplysningsgrunnlagService.slettBatchMedInaktiveGrunnlag(etterId = 0, batchStørrelse = 200)

        // Assert
        assertThat(slettetBatch.grunnlagIder).isEmpty()
        assertThat(slettetBatch.antallSlettedeRaderPerTabell).isEmpty()
        assertThat(personopplysningGrunnlagRepository.findById(inaktivtGrunnlag.id)).isPresent
        assertThat(slettInaktivePersonopplysningsgrunnlagService.tellInaktiveGrunnlagUtenAktivtGrunnlagPåSammeBehandling()).isEqualTo(1)
    }

    @Test
    fun `skal ikke slette samme grunnlag på nytt når etterId er satt til siste slettede id`() {
        // Arrange
        opprettSøkerFagsakOgBehandling()
        opprettPersonopplysningGrunnlagOgPersonForBehandling()
        val inaktivtGrunnlag = lagreInaktivtGrunnlagMedPerson(behandling.id)

        // Act
        val slettetBatch = slettInaktivePersonopplysningsgrunnlagService.slettBatchMedInaktiveGrunnlag(etterId = inaktivtGrunnlag.id, batchStørrelse = 200)

        // Assert
        assertThat(slettetBatch.grunnlagIder).isEmpty()
        assertThat(personopplysningGrunnlagRepository.findById(inaktivtGrunnlag.id)).isPresent
    }

    private fun lagreInaktivtGrunnlagMedPerson(behandlingId: Long): PersonopplysningGrunnlag {
        val inaktivtGrunnlag = lagrePersonopplysningGrunnlag(PersonopplysningGrunnlag(behandlingId = behandlingId, aktiv = false))
        lagrePerson(
            Person(
                aktør = lagreAktør(randomAktør()),
                type = PersonType.SØKER,
                personopplysningGrunnlag = inaktivtGrunnlag,
                fødselsdato = LocalDate.of(1990, 1, 1),
                navn = "",
                kjønn = Kjønn.KVINNE,
            ).also { person ->
                person.statsborgerskap = mutableListOf(GrStatsborgerskap.fraStatsborgerskap(lagStatsborgerskap(), Medlemskap.NORDEN, person))
                person.opphold = mutableListOf(GrOpphold(type = OPPHOLDSTILLATELSE.PERMANENT, person = person))
                person.arbeidsforhold = mutableListOf(GrArbeidsforhold(arbeidsgiverId = "999999999", arbeidsgiverType = "ORGANISASJON", person = person))
                person.sivilstander = mutableListOf(GrSivilstand.fraSivilstand(lagSivilstand(), person))
                person.bostedsadresser = mutableListOf(GrBostedsadresse.fraBostedsadresse(lagBostedsadresse(), person))
                person.oppholdsadresser = mutableListOf(GrOppholdsadresse.fraOppholdsadresse(Oppholdsadresse(gyldigFraOgMed = LocalDate.of(2020, 1, 1)), person))
                person.dødsfall =
                    Dødsfall(
                        person = person,
                        dødsfallDato = LocalDate.now().minusDays(1),
                        dødsfallAdresse = null,
                        dødsfallPostnummer = null,
                        dødsfallPoststed = null,
                    )
            },
        )
        return inaktivtGrunnlag
    }
}
