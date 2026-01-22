package no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.oppholdsadresse

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import no.nav.familie.kontrakter.felles.personopplysning.OppholdAnnetSted.PAA_SVALBARD
import no.nav.familie.kontrakter.felles.personopplysning.UtenlandskAdresse
import no.nav.familie.ks.sak.common.util.storForbokstav
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.adresser.Adresse

@Entity(name = "GrUtenlandskAdresseOppholdsadresse")
@DiscriminatorValue("UtenlandskAdresse")
data class GrUtenlandskAdresseOppholdsadresse(
    @Column(name = "adressenavn")
    val adressenavnNummer: String?,
    @Column(name = "husnummer")
    val bygningEtasjeLeilighet: String?,
    @Column(name = "postboks")
    val postboksNummerNavn: String?,
    @Column(name = "postnummer")
    val postkode: String?,
    @Column(name = "by_sted")
    val bySted: String?,
    @Column(name = "region")
    val regionDistriktOmraade: String?,
    @Column(name = "landkode")
    val landkode: String,
) : GrOppholdsadresse() {
    override fun toSecureString(): String =
        "GrUtenlandskAdresseOppholdsadresse(" +
            "adressenavnNummer=$adressenavnNummer, " +
            "bygningEtasjeLeilighet=$bygningEtasjeLeilighet, " +
            "postboksNummerNavn=$postboksNummerNavn, " +
            "postkode=$postkode, " +
            "bySted=$bySted, " +
            "regionDistriktOmraade=$regionDistriktOmraade, " +
            "landkode=$landkode, " +
            "oppholdAnnetSted=$oppholdAnnetSted" +
            ")"

    override fun tilFrontendString(): String {
        val adressenavnNummer = adressenavnNummer?.storForbokstav()
        val bygningEtasjeLeilighet = bygningEtasjeLeilighet?.let { ", $it" } ?: ""
        val postboks = postboksNummerNavn?.let { ", $it" } ?: ""
        val postkode = postkode?.let { ", $it" } ?: ""
        val bySted = bySted?.let { ", $it" } ?: ""
        val regionDistriktOmraade = regionDistriktOmraade?.let { ", $it" } ?: ""
        val landkode = landkode?.let { ", $it" } ?: ""
        return adressenavnNummer?.let {
            "$adressenavnNummer$bygningEtasjeLeilighet$postboks$postkode$bySted$regionDistriktOmraade$landkode"
        } ?: "Ukjent utenlandsk adresse$landkode"
    }

    override fun tilAdresse(): Adresse =
        Adresse(
            gyldigFraOgMed = periode?.fom,
            gyldigTilOgMed = periode?.tom,
            oppholdAnnetSted = oppholdAnnetSted,
            utenlandskAdresse =
                UtenlandskAdresse(
                    adressenavnNummer = adressenavnNummer,
                    bygningEtasjeLeilighet = bygningEtasjeLeilighet,
                    postboksNummerNavn = postboksNummerNavn,
                    postkode = postkode,
                    bySted = bySted,
                    regionDistriktOmraade = regionDistriktOmraade,
                    landkode = landkode,
                ),
        )

    override fun toString(): String = "GrUtenlandskAdresseOppholdsadresse(detaljer skjult)"

    override fun erPåSvalbard(): Boolean = oppholdAnnetSted == PAA_SVALBARD

    companion object {
        fun fraUtenlandskAdresse(utenlandskAdresse: UtenlandskAdresse): GrUtenlandskAdresseOppholdsadresse =
            GrUtenlandskAdresseOppholdsadresse(
                utenlandskAdresse.adressenavnNummer.takeUnless { it.isNullOrBlank() },
                utenlandskAdresse.bygningEtasjeLeilighet.takeUnless { it.isNullOrBlank() },
                utenlandskAdresse.postboksNummerNavn.takeUnless { it.isNullOrBlank() },
                utenlandskAdresse.postkode.takeUnless { it.isNullOrBlank() },
                utenlandskAdresse.bySted.takeUnless { it.isNullOrBlank() },
                utenlandskAdresse.regionDistriktOmraade.takeUnless { it.isNullOrBlank() },
                utenlandskAdresse.landkode,
            )
    }
}
