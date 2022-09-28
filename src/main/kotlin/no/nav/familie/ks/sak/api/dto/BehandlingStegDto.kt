package no.nav.familie.ks.sak.api.dto

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Målform
import java.time.LocalDate

abstract class BehandlingStegDto

// TODO bør sjekke om vi trenger bekreftEndringerViaFrontend felt
data class RegisterSøknadDto(val søknad: SøknadDTO, val bekreftEndringerViaFrontend: Boolean) : BehandlingStegDto()

data class SøknadDTO(
    val søkerMedOpplysninger: SøkerMedOpplysningerDto,
    val barnaMedOpplysninger: List<BarnMedOpplysningerDto>,
    val endringAvOpplysningerBegrunnelse: String
)

fun SøknadDTO.writeValueAsString(): String = objectMapper.writeValueAsString(this)

data class SøkerMedOpplysningerDto(
    val ident: String,
    val målform: Målform = Målform.NB
)

data class BarnMedOpplysningerDto(
    val ident: String,
    val navn: String = "",
    val fødselsdato: LocalDate? = null,
    val inkludertISøknaden: Boolean = true,
    val manueltRegistrert: Boolean = false,
    val erFolkeregistrert: Boolean = true
)
