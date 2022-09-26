package no.nav.familie.ks.sak.data

import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.fagsak.domene.Fagsak
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personident.Personident
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Kjønn
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Medlemskap
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.sivilstand.GrSivilstand
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.statsborgerskap.GrStatsborgerskap
import java.time.LocalDate
import kotlin.math.abs
import kotlin.random.Random

object Testdata {

    fun randomFnr(): String = "12345678910"

    fun tilAktør(fnr: String, toSisteSiffrer: String = "00") = Aktør(fnr + toSisteSiffrer).also {
        it.personidenter.add(Personident(fnr, aktør = it))
    }

    fun defaultFagsak(aktør: Aktør = tilAktør(randomFnr())) = Fagsak(
        1,
        aktør = aktør
    )

    fun lagBehandling(
        fagsak: Fagsak = defaultFagsak(),
        behandlingKategori: BehandlingKategori = BehandlingKategori.NASJONAL,
        behandlingType: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
        årsak: BehandlingÅrsak = BehandlingÅrsak.SØKNAD,
        skalBehandlesAutomatisk: Boolean = false,
        resultat: Behandlingsresultat = Behandlingsresultat.IKKE_VURDERT
    ) =
        Behandling(
            id = nesteBehandlingId(),
            fagsak = fagsak,
            skalBehandlesAutomatisk = skalBehandlesAutomatisk,
            type = behandlingType,
            kategori = behandlingKategori,
            opprettetÅrsak = årsak,
            resultat = resultat
        )

    private var gjeldendeBehandlingId: Long = abs(Random.nextLong(10000000))
    private const val ID_INKREMENT = 50

    fun nesteBehandlingId(): Long {
        gjeldendeBehandlingId += ID_INKREMENT
        return gjeldendeBehandlingId
    }

    fun lagPersonopplysningGrunnlag(
        behandlingId: Long,
        søkerPersonIdent: String,
        barnasIdenter: List<String>,
        barnasFødselsdatoer: List<LocalDate> = barnasIdenter.map { LocalDate.of(2019, 1, 1) },
        søkerAktør: Aktør = tilAktør(søkerPersonIdent).also {
            it.personidenter.add(
                Personident(
                    fødselsnummer = søkerPersonIdent,
                    aktør = it,
                    aktiv = søkerPersonIdent == it.personidenter.first().fødselsnummer
                )
            )
        },
        barnAktør: List<Aktør> = barnasIdenter.map { fødselsnummer ->
            tilAktør(fødselsnummer).also {
                it.personidenter.add(
                    Personident(
                        fødselsnummer = fødselsnummer,
                        aktør = it,
                        aktiv = fødselsnummer == it.personidenter.first().fødselsnummer
                    )
                )
            }
        }
    ): PersonopplysningGrunnlag {
        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandlingId)

        val søker = Person(
            aktør = søkerAktør,
            type = PersonType.SØKER,
            personopplysningGrunnlag = personopplysningGrunnlag,
            fødselsdato = LocalDate.of(2019, 1, 1),
            navn = "",
            kjønn = Kjønn.KVINNE
        ).also { søker ->
            søker.statsborgerskap =
                mutableListOf(GrStatsborgerskap(landkode = "NOR", medlemskap = Medlemskap.NORDEN, person = søker))
            søker.bostedsadresser = mutableListOf()
            søker.sivilstander = mutableListOf(
                GrSivilstand(
                    type = SIVILSTAND.GIFT,
                    person = søker
                )
            )
        }
        personopplysningGrunnlag.personer.add(søker)

        barnAktør.mapIndexed { index, aktør ->
            personopplysningGrunnlag.personer.add(
                Person(
                    aktør = aktør,
                    type = PersonType.BARN,
                    personopplysningGrunnlag = personopplysningGrunnlag,
                    fødselsdato = barnasFødselsdatoer.get(index),
                    navn = "",
                    kjønn = Kjønn.MANN
                ).also { barn ->
                    barn.statsborgerskap =
                        mutableListOf(
                            GrStatsborgerskap(
                                landkode = "NOR",
                                medlemskap = Medlemskap.NORDEN,
                                person = barn
                            )
                        )
                    barn.bostedsadresser = mutableListOf()
                    barn.sivilstander = mutableListOf(
                        GrSivilstand(
                            type = SIVILSTAND.UGIFT,
                            person = barn
                        )
                    )
                }
            )
        }
        return personopplysningGrunnlag
    }
}
