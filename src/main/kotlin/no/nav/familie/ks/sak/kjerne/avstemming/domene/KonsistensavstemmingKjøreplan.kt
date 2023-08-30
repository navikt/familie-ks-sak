package no.nav.familie.ks.sak.kjerne.avstemming.domene

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import java.time.LocalDate

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
    LEDIG,
    MANUELL // Brukes for å kjøre Konsistensavstemming manuelt
}
