package no.nav.familie.ks.sak.integrasjon.pdl

import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.Adressebeskyttelse
import no.nav.familie.ks.sak.common.exception.PdlNotFoundException
import no.nav.familie.ks.sak.common.exception.PdlRequestException
import no.nav.familie.ks.sak.integrasjon.pdl.domene.Doedsfall
import no.nav.familie.ks.sak.integrasjon.pdl.domene.DødsfallData
import no.nav.familie.ks.sak.integrasjon.pdl.domene.ForelderBarnRelasjonInfo
import no.nav.familie.ks.sak.integrasjon.pdl.domene.ForelderBarnRelasjonInfoMaskert
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlBaseRespons
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlPersonData
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlPersonInfo
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate

val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")
val logger: Logger = LoggerFactory.getLogger("PdlUtil")

inline fun <reified DATA : Any, reified T : Any> feilsjekkOgReturnerData(
    ident: String?,
    pdlRespons: PdlBaseRespons<DATA>,
    dataMapper: (DATA) -> T?,
): T {
    if (pdlRespons.harFeil()) {
        if (pdlRespons.errors?.any { it.extensions?.notFound() == true } == true) {
            throw PdlNotFoundException()
        }
        secureLogger.error("Feil ved henting av ${T::class} fra PDL: ${pdlRespons.errorMessages()}")
        throw PdlRequestException("Feil ved henting av ${T::class} fra PDL. Se secure logg for detaljer.")
    }

    if (pdlRespons.harAdvarsel()) {
        logger.warn("Advarsel ved henting av ${T::class} fra PDL. Se securelogs for detaljer.")
        secureLogger.warn("Advarsel ved henting av ${T::class} fra PDL: ${pdlRespons.extensions?.warnings}")
    }

    val data = dataMapper.invoke(pdlRespons.data)
    if (data == null) {
        val errorMelding = if (ident != null) "Feil ved oppslag på ident $ident. " else "Feil ved oppslag på person."
        secureLogger.error("$errorMelding PDL rapporterte ingen feil men returnerte tomt datafelt")
        throw PdlRequestException("Manglende ${T::class} ved feilfri respons fra PDL. Se secure logg for detaljer.")
    }
    return data
}

fun tilPersonInfo(
    pdlPersonData: PdlPersonData,
    forelderBarnRelasjoner: Set<ForelderBarnRelasjonInfo> = emptySet(),
    maskertForelderBarnRelasjoner: Set<ForelderBarnRelasjonInfoMaskert> = emptySet(),
): PdlPersonInfo {
    return PdlPersonInfo(
        fødselsdato = LocalDate.parse(pdlPersonData.foedsel.first().foedselsdato),
        navn = pdlPersonData.navn.firstOrNull()?.fulltNavn(),
        kjønn = pdlPersonData.kjoenn.firstOrNull()?.kjoenn,
        forelderBarnRelasjoner = forelderBarnRelasjoner,
        forelderBarnRelasjonerMaskert = maskertForelderBarnRelasjoner,
        adressebeskyttelseGradering = pdlPersonData.adressebeskyttelse.firstOrNull()?.gradering,
        bostedsadresser = pdlPersonData.bostedsadresse,
        statsborgerskap = pdlPersonData.statsborgerskap,
        opphold = pdlPersonData.opphold,
        sivilstander = pdlPersonData.sivilstand,
        dødsfall = hentDødsfallDataFraListeMedDødsfall(pdlPersonData.doedsfall),
        kontaktinformasjonForDoedsbo = pdlPersonData.kontaktinformasjonForDoedsbo.firstOrNull(),
    )
}

fun List<Adressebeskyttelse>.tilAdressebeskyttelse() =
    this.firstOrNull()?.gradering
        ?: ADRESSEBESKYTTELSEGRADERING.UGRADERT

private fun hentDødsfallDataFraListeMedDødsfall(doedsfall: List<Doedsfall>): DødsfallData? {
    val dødsdato = doedsfall.filter { it.doedsdato != null }.map { it.doedsdato }.firstOrNull()

    if (doedsfall.isEmpty() || dødsdato == null) {
        return null
    }
    return DødsfallData(erDød = true, dødsdato = dødsdato)
}
