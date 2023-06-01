package no.nav.familie.ks.sak.barnehagelister.domene

import java.time.LocalDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

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
