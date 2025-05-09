package no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag

import no.nav.familie.kontrakter.felles.kodeverk.BetydningDto
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import no.nav.familie.ks.sak.common.entitet.DatoIntervallEntitet
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Medlemskap
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.statsborgerskap.GrStatsborgerskap
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class StatsborgerskapService(
    val integrasjonClient: IntegrasjonClient,
) {
    fun hentLand(landkode: String): String = integrasjonClient.hentLand(landkode)

    fun hentStatsborgerskapMedMedlemskap(
        statsborgerskap: Statsborgerskap,
        person: Person,
    ): List<GrStatsborgerskap> {
        if (statsborgerskap.iNordiskLand()) {
            return listOf(
                GrStatsborgerskap(
                    gyldigPeriode =
                        DatoIntervallEntitet(
                            fom = statsborgerskap.hentFom(),
                            tom = statsborgerskap.gyldigTilOgMed,
                        ),
                    landkode = statsborgerskap.land,
                    medlemskap = Medlemskap.NORDEN,
                    person = person,
                ),
            )
        }

        val eøsMedlemskapsPerioderForValgtLand =
            integrasjonClient.hentAlleEØSLand().betydninger[statsborgerskap.land] ?: emptyList()

        val datoFra = statsborgerskap.hentFom()

        return if (datoFra == null && statsborgerskap.gyldigTilOgMed == null) {
            val idag = LocalDate.now()
            listOf(
                GrStatsborgerskap(
                    gyldigPeriode =
                        DatoIntervallEntitet(
                            fom = idag,
                            tom = null,
                        ),
                    landkode = statsborgerskap.land,
                    medlemskap =
                        finnMedlemskap(
                            statsborgerskap = statsborgerskap,
                            eøsMedlemskapsperioderForValgtLand = eøsMedlemskapsPerioderForValgtLand,
                            gyldigFraOgMed = idag,
                        ),
                    person = person,
                ),
            )
        } else {
            hentMedlemskapsperioderUnderStatsborgerskapsperioden(
                medlemskapsperioderForValgtLand = eøsMedlemskapsPerioderForValgtLand,
                statsborgerFra = datoFra,
                statsborgerTil = statsborgerskap.gyldigTilOgMed,
            ).fold(emptyList()) { medlemskapsperioder, periode ->
                val medlemskapsperiode =
                    GrStatsborgerskap(
                        gyldigPeriode = periode,
                        landkode = statsborgerskap.land,
                        medlemskap =
                            finnMedlemskap(
                                statsborgerskap = statsborgerskap,
                                eøsMedlemskapsperioderForValgtLand = eøsMedlemskapsPerioderForValgtLand,
                                gyldigFraOgMed = periode.fom,
                            ),
                        person = person,
                    )
                medlemskapsperioder + listOf(medlemskapsperiode)
            }
        }
    }

    private fun hentMedlemskapsperioderUnderStatsborgerskapsperioden(
        medlemskapsperioderForValgtLand: List<BetydningDto>,
        statsborgerFra: LocalDate?,
        statsborgerTil: LocalDate?,
    ): List<DatoIntervallEntitet> {
        val datoerMedlemskapEndrerSeg =
            medlemskapsperioderForValgtLand.flatMap {
                listOf(
                    it.gyldigFra,
                    it.gyldigTil.plusDays(1),
                )
            }

        val endringsdatoerUnderStatsborgerskapsperioden =
            datoerMedlemskapEndrerSeg
                .filter { datoForEndringIMedlemskap ->
                    erInnenforDatoerSomBetegnerUendelighetIKodeverk(datoForEndringIMedlemskap)
                }.filter { datoForEndringIMedlemskap ->
                    erInnenforDatoerForStatsborgerskapet(datoForEndringIMedlemskap, statsborgerFra, statsborgerTil)
                }

        val datoerMedlemskapEllerStatsborgerskapEndrerSeg =
            listOf(statsborgerFra) + endringsdatoerUnderStatsborgerskapsperioden + listOf(statsborgerTil)

        val naivePerioder = datoerMedlemskapEllerStatsborgerskapEndrerSeg.windowed(2, 1)

        return hentDatointervallerMedSluttdatoFørNesteStarter(naivePerioder)
    }

    private fun finnMedlemskap(
        statsborgerskap: Statsborgerskap,
        eøsMedlemskapsperioderForValgtLand: List<BetydningDto>,
        gyldigFraOgMed: LocalDate?,
    ): Medlemskap =
        when {
            statsborgerskap.iNordiskLand() -> Medlemskap.NORDEN
            erEØSMedlemPåGittDato(eøsMedlemskapsperioderForValgtLand, gyldigFraOgMed) -> Medlemskap.EØS
            statsborgerskap.iTredjeland() -> Medlemskap.TREDJELANDSBORGER
            statsborgerskap.erStatsløs() -> Medlemskap.STATSLØS
            else -> Medlemskap.UKJENT
        }

    private fun erEØSMedlemPåGittDato(
        eøsMedlemskapsperioderForValgtLand: List<BetydningDto>,
        gjeldendeDato: LocalDate?,
    ): Boolean = eøsMedlemskapsperioderForValgtLand.any { gjeldendeDato == null || (it.gyldigFra <= gjeldendeDato && it.gyldigTil >= gjeldendeDato) }

    private fun erInnenforDatoerSomBetegnerUendelighetIKodeverk(dato: LocalDate) = dato.isAfter(TIDLIGSTE_DATO_I_KODEVERK) && dato.isBefore(SENESTE_DATO_I_KODEVERK)

    private fun erInnenforDatoerForStatsborgerskapet(
        dato: LocalDate,
        statsborgerFra: LocalDate?,
        statsborgerTil: LocalDate?,
    ) = (statsborgerFra == null || dato.isAfter(statsborgerFra)) &&
        (statsborgerTil == null || dato.isBefore(statsborgerTil))

    private fun hentDatointervallerMedSluttdatoFørNesteStarter(intervaller: List<List<LocalDate?>>): List<DatoIntervallEntitet> =
        intervaller.mapIndexed { index, endringsdatoPar ->
            val fra = endringsdatoPar[0]
            val nesteEndringsdato = endringsdatoPar[1]

            if (index != intervaller.lastIndex && nesteEndringsdato == null) {
                throw Feil("EØS-medlemskap skal ikke kunne ha null som fra/til-dato")
            }

            val intervallSluttdato =
                if (index != intervaller.lastIndex) nesteEndringsdato!!.minusDays(1) else nesteEndringsdato

            DatoIntervallEntitet(fra, intervallSluttdato)
        }

    companion object {
        const val LANDKODE_UKJENT = "XUK"
        const val LANDKODE_STATSLØS = "XXX"
        val TIDLIGSTE_DATO_I_KODEVERK: LocalDate = LocalDate.parse("1900-01-02")
        val SENESTE_DATO_I_KODEVERK: LocalDate = LocalDate.parse("9990-01-01")
    }
}

fun Statsborgerskap.hentFom() = this.bekreftelsesdato ?: this.gyldigFraOgMed

fun Statsborgerskap.iNordiskLand() = Norden.values().map { it.name }.contains(this.land)

fun Statsborgerskap.iTredjeland() = this.land != StatsborgerskapService.LANDKODE_UKJENT

fun Statsborgerskap.erStatsløs() = this.land == StatsborgerskapService.LANDKODE_STATSLØS

/**
 * Norge, Sverige, Finland, Danmark, Island, Grønland, Færøyene og Åland
 */
enum class Norden {
    NOR,
    SWE,
    FIN,
    DNK,
    ISL,
    FRO,
    GRL,
    ALA,
}
