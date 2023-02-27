package no.nav.familie.ks.sak.api.mapper

import no.nav.familie.ks.sak.api.dto.RegisterHistorikkResponsDto
import no.nav.familie.ks.sak.api.dto.RegisteropplysningResponsDto
import no.nav.familie.ks.sak.common.util.storForbokstav
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.bostedsadresse.GrBostedsadresse
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.dødsfall.Dødsfall
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.opphold.GrOpphold
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.sivilstand.GrSivilstand
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.statsborgerskap.GrStatsborgerskap

object RegisterHistorikkMapper {

    fun lagRegisterHistorikkResponsDto(person: Person, landKodeOgLandNavn: Map<String, String>?) =
        RegisterHistorikkResponsDto(
            hentetTidspunkt = person.personopplysningGrunnlag.opprettetTidspunkt,
            oppholdstillatelse = person.opphold.map { lagRegisterOpplysningDto(it) },
            statsborgerskap = landKodeOgLandNavn?.let {
                person.statsborgerskap.map { statsborgerskap ->
                    lagRegisterOpplysningDto(
                        statsborgerskap,
                        landKodeOgLandNavn
                    )
                }
            },
            bostedsadresse = person.bostedsadresser.map { lagRegisterOpplysningDto(it) },
            sivilstand = person.sivilstander.map { lagRegisterOpplysningDto(it) },
            dødsboadresse = person.dødsfall?.let { listOf(lagRegisterOpplysningDto(it)) } ?: emptyList()
        )

    private fun lagRegisterOpplysningDto(dødsfall: Dødsfall): RegisteropplysningResponsDto =
        RegisteropplysningResponsDto(
            fom = dødsfall.dødsfallDato,
            tom = null,
            verdi = dødsfall.dødsfallAdresse?.let { dødsfall.hentAdresseToString() } ?: "-"
        )

    private fun lagRegisterOpplysningDto(sivilstand: GrSivilstand) =
        RegisteropplysningResponsDto(
            fom = sivilstand.fom,
            tom = null,
            verdi = sivilstand.type.name.replace('_', ' ').storForbokstav()
        )

    private fun lagRegisterOpplysningDto(bostedsAdresse: GrBostedsadresse) =
        RegisteropplysningResponsDto(
            fom = bostedsAdresse.periode?.fom,
            tom = bostedsAdresse.periode?.tom,
            verdi = bostedsAdresse.tilFrontendString()
        )

    private fun lagRegisterOpplysningDto(statsborgerskap: GrStatsborgerskap, landKodeOgLandNavn: Map<String, String>): RegisteropplysningResponsDto {
        val landNavn = landKodeOgLandNavn[statsborgerskap.landkode]!!

        val verdi = if (landNavn.equals("uoppgitt", true)) {
            "$landNavn ($landNavn)"
        } else {
            landNavn.storForbokstav()
        }

        return RegisteropplysningResponsDto(
            fom = statsborgerskap.gyldigPeriode?.fom,
            tom = statsborgerskap.gyldigPeriode?.tom,
            verdi = verdi
        )
    }

    private fun lagRegisterOpplysningDto(opphold: GrOpphold): RegisteropplysningResponsDto =
        RegisteropplysningResponsDto(
            fom = opphold.gyldigPeriode?.fom,
            tom = opphold.gyldigPeriode?.tom,
            verdi = opphold.type.name.replace('_', ' ').storForbokstav()
        )
}
