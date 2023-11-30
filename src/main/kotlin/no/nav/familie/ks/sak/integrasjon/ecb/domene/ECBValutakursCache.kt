package no.nav.familie.ks.sak.integrasjon.ecb.domene

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ks.sak.common.entitet.BaseEntitet
import java.math.BigDecimal
import java.time.LocalDate

@Entity(name = "EcbValutakursCache")
@Table(name = "ECBVALUTAKURSCACHE")
data class ECBValutakursCache(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ecbvalutakurscache_seq_generator")
    @SequenceGenerator(name = "ecbvalutakurscache_seq_generator", sequenceName = "ecbvalutakurscache_seq", allocationSize = 50)
    val id: Long = 0,
    @Column(name = "valutakursdato", columnDefinition = "DATE")
    val valutakursdato: LocalDate? = null,
    @Column(name = "valutakode")
    val valutakode: String? = null,
    @Column(name = "kurs", nullable = false)
    val kurs: BigDecimal,
) : BaseEntitet()
