package no.nav.familie.ks.sak.kjerne.brev

import no.nav.familie.kontrakter.felles.dokarkiv.AvsenderMottaker
import no.nav.familie.ks.sak.api.dto.ManuellAdresseInfo
import no.nav.familie.ks.sak.api.dto.ManueltBrevDto

data class JournalførManueltBrevDto(
    val fagsakId: Long,
    val behandlingId: Long?,
    val manueltBrevDto: ManueltBrevDto,
    val avsenderMottaker: AvsenderMottaker?,
    val manuellAdresseInfo: ManuellAdresseInfo?,
    val eksternReferanseId: String,
)
