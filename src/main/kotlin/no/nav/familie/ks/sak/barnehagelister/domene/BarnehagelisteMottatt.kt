package no.nav.familie.ks.sak.barnehagelister.domene

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity(name = "BarnehagelisteMottatt")
@Table(name = "BARNEHAGELISTE_MOTTATT")
data class BarnehagelisteMottatt(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "MELDING_ID", nullable = false, updatable = false, unique = true)
    val meldingId: String,
    @Column(name = "MELDING", nullable = true)
    val melding: String,
    @Column(name = "MOTTATT_TID", nullable = false)
    val mottatTid: LocalDateTime,
)
