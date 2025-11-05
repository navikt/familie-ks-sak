package no.nav.familie.ks.sak.integrasjon.oppdrag

import no.nav.familie.http.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations

@Service
class AvstemmingKlient(
    @Value("\${FAMILIE_OPPDRAG_API_URL}")
    private val familieOppdragUri: String,
    @Qualifier("jwtBearerLongTimeout") restOperations: RestOperations,
) : AbstractRestClient(restOperations, "økonomi_kontantstøtte")
