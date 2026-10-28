package no.nav.familie.ks.sak.common.exception

import no.nav.familie.log.mdc.MDCConstants
import org.slf4j.MDC
import org.springframework.http.HttpStatus

open class Feil(
    message: String,
    open val frontendFeilmelding: String? = null,
    open val httpStatus: HttpStatus = HttpStatus.OK,
    open val throwable: Throwable? = null,
    override val cause: Throwable? = throwable,
    open val callId: String? = MDC.get(MDCConstants.MDC_CALL_ID),
) : RuntimeException(message)

open class FunksjonellFeil(
    open val melding: String,
    open val frontendFeilmelding: String? = melding,
    open val httpStatus: HttpStatus = HttpStatus.OK,
    open val throwable: Throwable? = null,
    override val cause: Throwable? = throwable,
    open val callId: String? = MDC.get(MDCConstants.MDC_CALL_ID),
) : RuntimeException(melding)

class UtbetalingsikkerhetFeil(
    melding: String,
    override val frontendFeilmelding: String? = null,
    override val httpStatus: HttpStatus = HttpStatus.OK,
    override val throwable: Throwable? = null,
    override val cause: Throwable? = throwable,
) : FunksjonellFeil(
        melding,
        frontendFeilmelding,
        httpStatus,
        throwable,
    )

class RolleTilgangskontrollFeil(
    melding: String,
    override val frontendFeilmelding: String = melding,
    override val httpStatus: HttpStatus = HttpStatus.OK,
    override val throwable: Throwable? = null,
    override val cause: Throwable? = throwable,
) : FunksjonellFeil(
        melding,
        frontendFeilmelding,
        httpStatus,
        throwable,
    )

class PdlRequestException(
    message: String,
) : Feil(message)

class PdlNotFoundException : FunksjonellFeil("Fant ikke person")

class PdlPersonKanIkkeBehandlesIFagsystem(
    val årsak: PdlPersonKanIkkeBehandlesIFagSystemÅrsak,
) : FunksjonellFeil("Person kan ikke behandles i fagsystem: $årsak")

enum class PdlPersonKanIkkeBehandlesIFagSystemÅrsak {
    MANGLER_FØDSELSDATO,
    OPPHØRT,
}

const val KONTAKT_TEAMET_SUFFIX = "Kontakt teamet for hjelp."
