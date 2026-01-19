package no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.adresser

import no.nav.familie.kontrakter.felles.personopplysning.Folkeregistermetadata
import no.nav.familie.kontrakter.felles.personopplysning.Matrikkeladresse
import no.nav.familie.kontrakter.felles.personopplysning.OppholdAnnetSted
import no.nav.familie.kontrakter.felles.personopplysning.UkjentBosted
import no.nav.familie.kontrakter.felles.personopplysning.UtenlandskAdresse
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import java.time.LocalDate

/*
 Felles representasjon av adresseinformasjon lagret i personopplysninggrunnlag.
 */
data class Adresse(
    val gyldigFraOgMed: LocalDate? = null,
    val gyldigTilOgMed: LocalDate? = null,
    val vegadresse: Vegadresse? = null,
    val matrikkeladresse: Matrikkeladresse? = null,
    val ukjentBosted: UkjentBosted? = null,
    val oppholdAnnetSted: OppholdAnnetSted? = null,
    val folkeregistermetadata: Folkeregistermetadata? = null,
    val utenlandskAdresse: UtenlandskAdresse? = null,
)
