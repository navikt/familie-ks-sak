package no.nav.familie.ks.sak.data.no.nav.familie.ks.sak.datagenerator

import no.nav.familie.kontrakter.felles.personopplysning.OppholdAnnetSted
import no.nav.familie.ks.sak.common.entitet.DatoIntervallEntitet
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.oppholdsadresse.GrMatrikkeladresseOppholdsadresse
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.oppholdsadresse.GrUkjentAdresseOppholdsadresse
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.oppholdsadresse.GrUtenlandskAdresseOppholdsadresse
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.oppholdsadresse.GrVegadresseOppholdsadresse

fun lagGrVegadresseOppholdsadresse(
    matrikkelId: Long? = null,
    husnummer: String? = null,
    husbokstav: String? = null,
    bruksenhetsnummer: String? = null,
    adressenavn: String? = null,
    kommunenummer: String? = null,
    tilleggsnavn: String? = null,
    postnummer: String? = null,
    periode: DatoIntervallEntitet? = null,
    poststed: String? = null,
) = GrVegadresseOppholdsadresse(
    matrikkelId = matrikkelId,
    husnummer = husnummer,
    husbokstav = husbokstav,
    bruksenhetsnummer = bruksenhetsnummer,
    adressenavn = adressenavn,
    kommunenummer = kommunenummer,
    tilleggsnavn = tilleggsnavn,
    postnummer = postnummer,
    poststed = poststed,
).also { it.periode = periode }

fun lagGrMatrikkelOppholdsadresse(
    matrikkelId: Long? = null,
    bruksenhetsnummer: String? = null,
    kommunenummer: String? = null,
    tilleggsnavn: String? = null,
    postnummer: String? = null,
    periode: DatoIntervallEntitet? = null,
    poststed: String? = null,
) = GrMatrikkeladresseOppholdsadresse(
    matrikkelId = matrikkelId,
    bruksenhetsnummer = bruksenhetsnummer,
    kommunenummer = kommunenummer,
    tilleggsnavn = tilleggsnavn,
    postnummer = postnummer,
    poststed = poststed,
).also { it.periode = periode }

fun lagGrUtenlandskOppholdsadresse(
    adressenavnNummer: String? = null,
    bygningEtasjeLeilighet: String? = null,
    postboksNummerNavn: String? = null,
    postkode: String? = null,
    bySted: String? = null,
    regionDistriktOmraade: String? = null,
    landkode: String? = null,
    periode: DatoIntervallEntitet? = null,
    oppholdAnnetSted: OppholdAnnetSted? = null,
) = GrUtenlandskAdresseOppholdsadresse(
    adressenavnNummer = adressenavnNummer,
    bygningEtasjeLeilighet = bygningEtasjeLeilighet,
    postboksNummerNavn = postboksNummerNavn,
    postkode = postkode,
    bySted = bySted,
    regionDistriktOmraade = regionDistriktOmraade,
    landkode = landkode,
).also {
    it.periode = periode
    it.oppholdAnnetSted = oppholdAnnetSted
}

fun lagGrUkjentAdresseOppholdsadresse(
    periode: DatoIntervallEntitet? = null,
    oppholdAnnetSted: OppholdAnnetSted? = null,
) = GrUkjentAdresseOppholdsadresse().also {
    it.periode = periode
    it.oppholdAnnetSted = oppholdAnnetSted
}
