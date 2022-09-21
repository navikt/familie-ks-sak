package no.nav.familie.ks.sak.sikkerhet

import no.nav.familie.ks.sak.config.BehandlerRolle

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class SjekkTilgang(
    val tilgang: Tilgang,
    val handling: String = "",
    val index: Int = 0,
    val minimumBehandlerRolle: BehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
    val auditLoggerEvent: AuditLoggerEvent = AuditLoggerEvent.ACCESS
)
