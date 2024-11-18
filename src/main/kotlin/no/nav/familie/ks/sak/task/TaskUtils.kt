package no.nav.familie.ks.sak.task

import no.nav.familie.log.mdc.MDCConstants
import org.slf4j.MDC

fun <T> overstyrTaskMedNyCallId(
    callId: String,
    body: () -> T,
): T {
    val originalCallId = MDC.get(MDCConstants.MDC_CALL_ID) ?: null

    return try {
        MDC.put(MDCConstants.MDC_CALL_ID, callId)
        body()
    } finally {
        if (originalCallId == null) {
            MDC.remove(MDCConstants.MDC_CALL_ID)
        } else {
            MDC.put(MDCConstants.MDC_CALL_ID, originalCallId)
        }
    }
}
