package no.nav.familie.ks.sak.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
class RolleConfig(
    @Value("\${rolle.beslutter}")
    val BESLUTTER_ROLLE: String,
    @Value("\${rolle.saksbehandler}")
    val SAKSBEHANDLER_ROLLE: String,
    @Value("\${rolle.veileder}")
    val VEILEDER_ROLLE: String,
    @Value("\${rolle.kode6}")
    val KODE6: String,
    @Value("\${rolle.kode7}")
    val KODE7: String
)

enum class BehandlerRolle(val nivå: Int) {
    SYSTEM(4),
    BESLUTTER(3),
    SAKSBEHANDLER(2),
    VEILEDER(1),
    UKJENT(0)
}
