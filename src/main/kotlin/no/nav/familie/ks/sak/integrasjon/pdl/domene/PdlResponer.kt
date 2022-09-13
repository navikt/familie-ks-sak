package no.nav.familie.ks.sak.integrasjon.pdl.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

data class PdlBaseResponse<T>(
    val data: T,
    val errors: List<PdlError>?
) {

    fun harFeil(): Boolean {
        return errors != null && errors.isNotEmpty()
    }

    fun errorMessages(): String {
        return errors?.joinToString { it -> it.message } ?: ""
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlError(
    val message: String,
    val extensions: PdlExtensions?
)

data class PdlExtensions(val code: String?) {

    fun notFound() = code == "not_found"
}

class PdlHentIdenterResponse(val pdlIdenter: PdlIdenter?)

data class PdlIdenter(val identer: List<PdlIdent>)

data class PdlIdent(
    val ident: String,
    val historisk: Boolean,
    val gruppe: String
)
