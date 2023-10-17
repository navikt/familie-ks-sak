package no.nav.familie.ks.sak.kjerne.behandling.domene

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ks.sak.common.entitet.BaseEntitet
import java.time.LocalDateTime

@Entity(name = "BehandlingSøknadsinfo")
@Table(name = "BEHANDLING_SOKNADSINFO")
data class BehandlingSøknadsinfo(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "behandling_søknadsinfo_seq_generator")
    @SequenceGenerator(
        name = "behandling_søknadsinfo_seq_generator",
        sequenceName = "behandling_soknadsinfo_seq",
        allocationSize = 50,
    )
    val id: Long = 0,
    @ManyToOne(optional = false)
    @JoinColumn(name = "fk_behandling_id", nullable = false, updatable = false)
    val behandling: Behandling,
    @Column(name = "mottatt_dato")
    val mottattDato: LocalDateTime,
    @Column(name = "journalpost_id")
    val journalpostId: String,
    @Column(name = "er_digital")
    val erDigital: Boolean,
) : BaseEntitet()
