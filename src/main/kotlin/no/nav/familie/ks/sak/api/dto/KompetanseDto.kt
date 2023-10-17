package no.nav.familie.ks.sak.api.dto

import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.AnnenForeldersAktivitet
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.SøkersAktivitet
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import org.springframework.http.HttpStatus
import java.time.YearMonth

data class KompetanseDto(
    val id: Long, // brukes for å slette kompetanse
    val fom: YearMonth?,
    val tom: YearMonth?,
    val barnIdenter: List<String>,
    val søkersAktivitet: SøkersAktivitet? = null,
    val søkersAktivitetsland: String? = null,
    val annenForeldersAktivitet: AnnenForeldersAktivitet? = null,
    val annenForeldersAktivitetsland: String? = null,
    val barnetsBostedsland: String? = null,
    val resultat: KompetanseResultat? = null,
    override val status: UtfyltStatus = UtfyltStatus.IKKE_UTFYLT,
) : AbstractEøsSkjemaUtfyltStatus<KompetanseDto>() {
    init {
        when {
            this.fom == null -> {
                throw FunksjonellFeil("Manglende fom", httpStatus = HttpStatus.BAD_REQUEST)
            }
            this.tom != null && this.fom > this.tom -> {
                throw FunksjonellFeil("fom er etter tom", httpStatus = HttpStatus.BAD_REQUEST)
            }
            this.barnIdenter.isEmpty() -> {
                throw FunksjonellFeil("Mangler barn", httpStatus = HttpStatus.BAD_REQUEST)
            }
        }
    }

    override fun medUtfyltStatus(): KompetanseDto {
        var antallUtfylteFelter =
            finnAntallUtfylt(
                listOf(
                    this.annenForeldersAktivitet,
                    this.barnetsBostedsland,
                    this.annenForeldersAktivitetsland,
                    this.resultat,
                    this.søkersAktivitet,
                    this.søkersAktivitetsland,
                ),
            )
        if (annenForeldersAktivitetsland == null) {
            antallUtfylteFelter += (
                annenForeldersAktivitet.let {
                    if (it == AnnenForeldersAktivitet.INAKTIV || it == AnnenForeldersAktivitet.IKKE_AKTUELT) 1 else 0
                }
            )
        }
        if (søkersAktivitetsland == null) {
            antallUtfylteFelter += (søkersAktivitet.let { if (it == SøkersAktivitet.INAKTIV) 1 else 0 })
        }
        return this.copy(status = utfyltStatus(antallUtfylteFelter, 6))
    }
}

fun KompetanseDto.tilKompetanse(barnAktører: List<Aktør>) =
    Kompetanse(
        fom = this.fom,
        tom = this.tom,
        barnAktører = barnAktører.toSet(),
        søkersAktivitet = this.søkersAktivitet,
        søkersAktivitetsland = this.søkersAktivitetsland,
        annenForeldersAktivitet = this.annenForeldersAktivitet,
        annenForeldersAktivitetsland = this.annenForeldersAktivitetsland,
        barnetsBostedsland = this.barnetsBostedsland,
        resultat = this.resultat,
    )

fun Kompetanse.tilKompetanseDto() =
    KompetanseDto(
        id = this.id,
        fom = this.fom,
        tom = this.tom,
        barnIdenter = barnAktører.map { it.aktivFødselsnummer() },
        søkersAktivitet = this.søkersAktivitet,
        søkersAktivitetsland = this.søkersAktivitetsland,
        annenForeldersAktivitet = this.annenForeldersAktivitet,
        annenForeldersAktivitetsland = this.annenForeldersAktivitetsland,
        barnetsBostedsland = this.barnetsBostedsland,
        resultat = this.resultat,
    ).medUtfyltStatus()
