package no.nav.familie.ks.sak.kjerne.avstemming.domene

import java.time.LocalDate
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@Entity(name = "KonsistensavstemmingKjoreplan")
@Table(name = "konsistensavstemming_kjoreplan")
class KonsistensavstemmingKjøreplan(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "konsistensavstemming_kjoreplan_seq")
    @SequenceGenerator(name = "konsistensavstemming_kjoreplan_seq")
    val id: Long = 0,

    @Column(name = "kjoredato", nullable = false)
    val kjøredato: LocalDate,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: KjøreStatus = KjøreStatus.LEDIG
)

enum class KjøreStatus {
    FERDIG,
    BEHANDLER,
    LEDIG
}
