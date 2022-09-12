package no.nav.familie.ks.sak.common.entitet

import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import java.io.Serializable
import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.MappedSuperclass
import javax.persistence.PreUpdate
import javax.persistence.Version

/**
 * En basis [Entity] klasse som håndtere felles standarder for utformign av tabeller (eks. sporing av hvem som har
 * opprettet eller oppdatert en rad, og når).
 */
@MappedSuperclass
abstract class BaseEntitet : Serializable {

    // The properties have to be open because when a subclass is lazy class, hibernate needs to override the accessor
    // to intercept its behavior. If they are final, hibernate will complain and it also can cause potential bug.
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
