package no.nav.familie.ks.sak.sikkerhet

import no.nav.familie.ks.sak.common.exception.RolleTilgangskontrollFeil
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.config.RolleConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.persistence.PrePersist
import javax.persistence.PreRemove
import javax.persistence.PreUpdate

@Component
class RollestyringMotDatabase {

    @Autowired
    private lateinit var rolleConfig: RolleConfig

    @PrePersist
    @PreUpdate
    @PreRemove
    fun kontrollerSkrivetilgang(objekt: Any) {
        val høyesteRolletilgang = SikkerhetContext.hentHøyesteRolletilgangForInnloggetBruker(rolleConfig)

        if (!harSkrivetilgang(høyesteRolletilgang)) {
            throw RolleTilgangskontrollFeil(
                melding = "${SikkerhetContext.hentSaksbehandlerNavn()} med rolle $høyesteRolletilgang har ikke skrivetilgang til databasen.",
                frontendFeilmelding = "Du har ikke tilgang til å gjøre denne handlingen."
            )
        }
    }

    private fun harSkrivetilgang(høyesteRolletilgang: BehandlerRolle) =
        høyesteRolletilgang.nivå >= BehandlerRolle.SAKSBEHANDLER.nivå
}
