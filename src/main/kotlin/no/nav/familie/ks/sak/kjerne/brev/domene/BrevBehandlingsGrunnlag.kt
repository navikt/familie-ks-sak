package no.nav.familie.ks.sak.kjerne.brev.domene

import java.time.LocalDate

data class BrevBehandlingsGrunnlag(
    val personerPåBehandling: List<BrevPerson>,
    val personResultater: List<BrevPersonResultat>,
    val endretUtbetalingAndeler: List<BrevEndretUtbetalingAndel>
)

data class BrevtUregistrertBarn(
    val personIdent: String,
    val navn: String,
    val fødselsdato: LocalDate? = null
)
