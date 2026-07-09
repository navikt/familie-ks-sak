package no.nav.familie.ks.sak.common.exception

import io.micrometer.core.instrument.Metrics
import jakarta.servlet.http.HttpServletRequest
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.ks.sak.common.util.RessursUtils
import no.nav.familie.ks.sak.common.util.RessursUtils.unauthorized
import org.slf4j.LoggerFactory
import org.springframework.core.NestedExceptionUtils
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.context.request.async.AsyncRequestNotUsableException
import org.springframework.web.servlet.resource.NoResourceFoundException
import tools.jackson.databind.exc.InvalidFormatException
import tools.jackson.databind.exc.MismatchedInputException
import java.io.EOFException
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.net.SocketException
import java.nio.channels.ClosedChannelException

@ControllerAdvice
class ApiExceptionHandler {
    private val logger = LoggerFactory.getLogger(ApiExceptionHandler::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @ExceptionHandler(RolleTilgangskontrollFeil::class)
    fun handleRolleTilgangskontrollFeil(rolleTilgangskontrollFeil: RolleTilgangskontrollFeil): ResponseEntity<Ressurs<Nothing>> = RessursUtils.rolleTilgangResponse(rolleTilgangskontrollFeil)

    @ExceptionHandler(Exception::class)
    fun handleException(exception: Exception): ResponseEntity<Ressurs<Nothing>> {
        val mostSpecificCause = NestedExceptionUtils.getMostSpecificCause(exception)
        // log stacktrace med den generelle feilen
        secureLogger.info("Mottok en ukjent exception. Original stacktrace er:", exception)

        return RessursUtils.illegalState(mostSpecificCause.message.toString(), mostSpecificCause)
    }

    @ExceptionHandler(HttpClientErrorException.Forbidden::class)
    fun handleForbidden(foriddenException: HttpClientErrorException.Forbidden): ResponseEntity<Ressurs<Nothing>> {
        val mostSpecificCause = NestedExceptionUtils.getMostSpecificCause(foriddenException)

        return RessursUtils.forbidden(mostSpecificCause.message ?: "Ikke tilgang")
    }

    @ExceptionHandler(HttpClientErrorException.Unauthorized::class)
    fun handleUnauhtorized(): ResponseEntity<Ressurs<Nothing>> {
        logger.info("Fikk 401 Unauthorized")
        return unauthorized("Unauthorized")
    }

    @ExceptionHandler(IntegrasjonException::class)
    fun handleIntegrasjonException(integrasjonException: IntegrasjonException): ResponseEntity<Ressurs<Nothing>> = RessursUtils.illegalState(integrasjonException.message.toString(), integrasjonException)

    @ExceptionHandler(PdlNotFoundException::class)
    fun handlePdlNotFoundException(feil: PdlNotFoundException): ResponseEntity<Ressurs<Nothing>> {
        logger.warn("Finner ikke personen i PDL")
        return ResponseEntity
            .ok()
            .body(Ressurs.failure(frontendFeilmelding = "Fant ikke person"))
    }

    @ExceptionHandler(Feil::class)
    fun handleFeil(feil: Feil): ResponseEntity<Ressurs<Nothing>> {
        val mostSpecificCause =
            if (feil.throwable != null) NestedExceptionUtils.getMostSpecificCause(feil.throwable!!) else null

        return RessursUtils.frontendFeil(feil, mostSpecificCause)
    }

    @ExceptionHandler(FunksjonellFeil::class)
    fun handleFunksjonellFeil(funksjonellFeil: FunksjonellFeil): ResponseEntity<Ressurs<Nothing>> = RessursUtils.funksjonellFeil(funksjonellFeil)

    @ExceptionHandler(EksternTjenesteFeilException::class)
    fun handleEksternTjenesteFeil(feil: EksternTjenesteFeilException): ResponseEntity<EksternTjenesteFeil> {
        val mostSpecificThrowable =
            if (feil.throwable != null) NestedExceptionUtils.getMostSpecificCause(feil.throwable) else null
        feil.eksternTjenesteFeil.exception =
            if (mostSpecificThrowable != null) "[${mostSpecificThrowable::class.java.name}] " else null

        if (mostSpecificThrowable != null) {
            val sw = StringWriter()
            feil.printStackTrace(PrintWriter(sw))
            feil.eksternTjenesteFeil.stackTrace = sw.toString()
        }

        secureLogger.info("$feil")
        logger.info(
            "Feil ekstern tjeneste: path:${feil.eksternTjenesteFeil.path} status:${feil.eksternTjenesteFeil.status} exception:${feil.eksternTjenesteFeil.exception}",
        )

        return ResponseEntity.status(feil.eksternTjenesteFeil.status).body(feil.eksternTjenesteFeil)
    }

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFoundException(exception: NoResourceFoundException): ResponseEntity<Ressurs<Nothing>> {
        logger.info("Fant ikke ressurs for request=${exception.resourcePath}", exception.resourcePath)

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            Ressurs.failure(
                frontendFeilmelding = "Fant ikke ressurs for request=${exception.resourcePath}",
            ),
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(httpMessageNotReadableException: HttpMessageNotReadableException): ResponseEntity<Ressurs<Nothing>> {
        val errorMessage =
            when (httpMessageNotReadableException.cause) {
                is InvalidFormatException -> {
                    val invalidFormatException = httpMessageNotReadableException.cause as InvalidFormatException
                    "Ugyldig verdi ${invalidFormatException.value} for felt ${invalidFormatException.path.joinToString(".")}"
                }

                is MismatchedInputException -> {
                    val mismatchedInputException = httpMessageNotReadableException.cause as MismatchedInputException
                    "Mangler verdi for felt ${mismatchedInputException.path.joinToString(".")}"
                }

                else -> {
                    logger.error("Ukjent feil ved lesing av request. Se securelogger for mer informasjon")
                    secureLogger.error("Ukjent feil ved lesing av request", httpMessageNotReadableException)
                    httpMessageNotReadableException.message ?: "Ukjent feil ved lesing av request"
                }
            }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            Ressurs.failure(
                errorMessage = errorMessage,
                frontendFeilmelding = errorMessage,
            ),
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleInputValideringFeil(valideringFeil: MethodArgumentNotValidException): ResponseEntity<Ressurs<Nothing>> {
        val errorMessage =
            valideringFeil.bindingResult.fieldErrors
                .map { fieldError ->
                    fieldError.defaultMessage
                }.joinToString(", ")
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            Ressurs.failure(
                errorMessage = errorMessage,
                frontendFeilmelding = errorMessage,
            ),
        )
    }

    /**
     * AsyncRequestNotUsableException er en exception som blir kastet når en async request blir avbrutt. Velger
     * å skjule denne exceptionen fra loggen da den ikke er interessant for oss.
     */
    @ExceptionHandler(AsyncRequestNotUsableException::class)
    fun handlAsyncRequestNotUsableException(e: AsyncRequestNotUsableException): ResponseEntity<Any> {
        logger.info("En AsyncRequestNotUsableException har oppstått, som skjer når en async request blir avbrutt", e)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
    }

    @ExceptionHandler(IOException::class, ClosedChannelException::class, EOFException::class)
    fun handleNettverksfeil(
        e: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<Ressurs<Nothing>> {
        val cause = NestedExceptionUtils.getMostSpecificCause(e)
        val type = NettverksfeilType.fraException(cause)

        nettverksfeilTeller[type]?.increment()
        logger.info(
            "Nettverksfeil av type=${type.metrikknavn} url=${request.method} ${request.requestURI} melding=${cause.message}",
        )

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
            Ressurs.failure(frontendFeilmelding = "Tilkoblingen ble brutt"),
        )
    }

    private val nettverksfeilTeller =
        NettverksfeilType.entries.associateWith {
            Metrics.counter("nettverksfeil.klientavbrudd", "type", it.metrikknavn)
        }
}

enum class NettverksfeilType(
    val metrikknavn: String,
) {
    BROKEN_PIPE("broken_pipe"),
    CLOSED_CHANNEL("closed_channel"),
    CONNECTION_RESET("connection_reset"),
    EOF("eof"),
    UKJENT("ukjent"),
    ;

    companion object {
        fun fraException(e: Throwable): NettverksfeilType =
            when {
                e is ClosedChannelException -> CLOSED_CHANNEL
                e is EOFException -> EOF
                e is IOException && e.message?.lowercase()?.contains("broken pipe") == true -> BROKEN_PIPE
                e is SocketException && e.message?.lowercase()?.contains("connection reset") == true -> CONNECTION_RESET
                else -> UKJENT
            }
    }
}
