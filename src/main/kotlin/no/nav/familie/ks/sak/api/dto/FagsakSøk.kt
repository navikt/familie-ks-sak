package no.nav.familie.ks.sak.api.dto

import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakStatus
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Kjønn
import no.nav.familie.tilgangsmaskin.Avvisningskode

data class SøkParamDto(
    val personIdent: String,
    val barnasIdenter: List<String> = emptyList(),
)

enum class FagsakDeltagerRolle {
    BARN,
    FORELDER,
    UKJENT,
}

data class FagsakDeltagerResponsDto(
    val navn: String? = null,
    val ident: String = "",
    val rolle: FagsakDeltagerRolle,
    val kjønn: Kjønn = Kjønn.UKJENT,
    val fagsakId: Long? = null,
    val fagsakStatus: FagsakStatus? = null,
    val adressebeskyttelseGradering: ADRESSEBESKYTTELSEGRADERING? = null,
    val harTilgang: Boolean = true,
    val avvisningskode: Avvisningskode? = null,
    val erEgenAnsatt: Boolean? = null,
) {
    override fun toString(): String = "FagsakDeltagerResponsDto(rolle=$rolle, fagsakId=$fagsakId)"
}
