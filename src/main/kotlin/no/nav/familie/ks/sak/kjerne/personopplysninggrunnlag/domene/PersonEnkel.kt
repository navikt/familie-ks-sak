package no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene

import no.nav.familie.ks.sak.kjerne.personident.Aktør
import java.time.LocalDate

/**
 * Støtteobjekt for å ikke hente inn hele [Person] som henter mye annet som mange ganger er unødvendig
 */
data class PersonEnkel(
    val type: PersonType,
    val aktør: Aktør,
    val fødselsdato: LocalDate,
    val dødsfallDato: LocalDate?,
    val målform: Målform,
)
