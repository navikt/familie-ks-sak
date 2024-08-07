package no.nav.familie.ks.sak.common.exception

import no.nav.familie.http.client.RessursException
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.ks.sak.common.util.RessursUtils
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.slf4j.LoggerFactory
import org.springframework.core.NestedExceptionUtils
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.client.HttpClientErrorException
import java.io.PrintWriter
import java.io.StringWriter

@ControllerAdvice
class ApiExceptionHandler {
    private val logger = LoggerFactory.getLogger(ApiExceptionHandler::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @ExceptionHandler(JwtTokenUnauthorizedException::class)
    fun handleThrowable(jwtTokenUnauthorizedException: JwtTokenUnauthorizedException): ResponseEntity<Ressurs<Nothing>> = RessursUtils.unauthorized("Unauthorized")

    @ExceptionHandler(RolleTilgangskontrollFeil::class)
    fun handleRolleTilgangskontrollFeil(rolleTilgangskontrollFeil: RolleTilgangskontrollFeil): ResponseEntity<Ressurs<Nothing>> = RessursUtils.rolleTilgangResponse(rolleTilgangskontrollFeil)

    @ExceptionHandler(Exception::class)
    fun handleException(exception: Exception): ResponseEntity<Ressurs<Nothing>> {
        val mostSpecificCause = NestedExceptionUtils.getMostSpecificCause(exception)

        return RessursUtils.illegalState(mostSpecificCause.message.toString(), mostSpecificCause)
    }

    @ExceptionHandler(RessursException::class)
    fun handleRessursException(ressursException: RessursException): ResponseEntity<Ressurs<Any>> = ResponseEntity.status(ressursException.httpStatus).body(ressursException.ressurs)

    @ExceptionHandler(HttpClientErrorException.Forbidden::class)
    fun handleForbidden(foriddenException: HttpClientErrorException.Forbidden): ResponseEntity<Ressurs<Nothing>> {
        val mostSpecificCause = NestedExceptionUtils.getMostSpecificCause(foriddenException)

        return RessursUtils.forbidden(mostSpecificCause.message ?: "Ikke tilgang")
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

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleInputValideringFeil(valideringFeil: MethodArgumentNotValidException): ResponseEntity<Ressurs<Nothing>> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            Ressurs.failure(
                valideringFeil.bindingResult.fieldErrors
                    .map { fieldError ->
                        fieldError.defaultMessage
                    }.joinToString(" ,"),
            ),
        )
}
