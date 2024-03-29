package no.nav.familie.ks.sak.common.entitet

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.PreUpdate
import jakarta.persistence.Version
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import java.io.Serializable
import java.time.LocalDateTime

/**
 * En basis [Entity] klasse som håndterer felles standarder for utforming av tabeller (eks. sporing av hvem som har
 * opprettet eller oppdatert en rad, og når).
 */
@MappedSuperclass
abstract class BaseEntitet : Serializable {
    // The properties have to be open because when a subclass is lazy class, hibernate needs to override the accessor
    // to intercept its behavior. If they are final, hibernate will complain, and it can also cause potential bugs.
    // See: https://stackoverflow.com/questions/55958667/kotlin-inheritance-and-jpa
    @Column(name = "opprettet_av", nullable = false, updatable = false)
    open val opprettetAv: String = SikkerhetContext.hentSaksbehandler()

    @Column(name = "opprettet_tid", nullable = false, updatable = false)
    open val opprettetTidspunkt: LocalDateTime = LocalDateTime.now()

    @Column(name = "endret_av")
    open var endretAv: String = SikkerhetContext.hentSaksbehandler()

    @Column(name = "endret_tid")
    open var endretTidspunkt: LocalDateTime = LocalDateTime.now()

    @Version
    @Column(name = "versjon", nullable = false)
    open var versjon: Long = 0

    @PreUpdate
    protected fun onUpdate() {
        endretAv = SikkerhetContext.hentSaksbehandler()
        endretTidspunkt = LocalDateTime.now()
    }
}
