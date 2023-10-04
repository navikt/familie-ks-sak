package no.nav.familie.ks.sak.barnehagelister.domene

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import no.nav.familie.ks.sak.common.entitet.BaseEntitet
import java.time.LocalDateTime
import java.util.UUID

@Entity(name = "BarnehagelisteMottattArkiv")
@Table(name = "BARNEHAGELISTE_MOTTATT_ARKIV")
data class BarnehagelisteMottattArkiv(
    @Id
    val id: UUID,

    @Column(name = "MELDING_ID", nullable = false, updatable = false, unique = true)
    val meldingId: String,

    @Column(name = "MELDING", nullable = true)
    val melding: String,

    @Column(name = "MOTTATT_TID", nullable = false)
    val mottatTid: LocalDateTime,
) : BaseEntitet()
