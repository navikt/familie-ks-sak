package no.nav.familie.ks.sak.kjerne.adopsjon

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
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import java.time.LocalDate

@Entity(name = "Adopsjon")
@Table(name = "ADOPSJON")
class Adopsjon(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "adopsjon_seq_generator")
    @SequenceGenerator(
        name = "adopsjon_seq_generator",
        sequenceName = "adopsjon_seq",
        allocationSize = 50,
    )
    val id: Long = 0,
    @ManyToOne(optional = false)
    @JoinColumn(name = "fk_aktoer_id", nullable = false, updatable = false)
    val aktør: Aktør,
    @Column(name = "fk_behandling_id", updatable = false, nullable = false)
    val behandlingId: Long,
    @Column(name = "adopsjonsdato", columnDefinition = "DATE")
    var adopsjonsdato: LocalDate,
) : BaseEntitet()
