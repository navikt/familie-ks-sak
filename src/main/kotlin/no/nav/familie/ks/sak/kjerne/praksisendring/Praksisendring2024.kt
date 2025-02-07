package no.nav.familie.ks.sak.kjerne.praksisendring

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ks.sak.common.entitet.BaseEntitet
import no.nav.familie.ks.sak.common.util.YearMonthConverter
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import java.time.YearMonth

@Entity(name = "Praksisendring2024")
@Table(name = "PRAKSISENDRING_2024")
data class Praksisendring2024(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "praksisendring_2024_seq_generator")
    @SequenceGenerator(
        name = "praksisendring_2024_seq_generator",
        sequenceName = "praksisendring_2024_seq",
        allocationSize = 50,
    )
    val id: Long = 0,
    @Column(name = "fk_fagsak_id", updatable = false, nullable = false)
    val fagsakId: Long,
    @OneToOne(optional = false)
    @JoinColumn(name = "fk_aktoer_id", nullable = false, updatable = false)
    val aktør: Aktør,
    @Column(name = "utbetalingsmaned")
    @Convert(converter = YearMonthConverter::class)
    val utbetalingsmåned: YearMonth,
) : BaseEntitet()
