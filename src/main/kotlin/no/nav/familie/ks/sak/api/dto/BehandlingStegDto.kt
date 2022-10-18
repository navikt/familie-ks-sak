package no.nav.familie.ks.sak.api.dto

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.ks.sak.kjerne.behandling.steg.søknad.domene.SøknadGrunnlag
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Målform
import no.nav.familie.prosessering.domene.Task
import java.time.LocalDate

abstract class BehandlingStegDto

// TODO bør sjekke om vi trenger bekreftEndringerViaFrontend felt
data class RegistrerSøknadDto(val søknad: SøknadDto, val bekreftEndringerViaFrontend: Boolean) : BehandlingStegDto()

data class JournalførVedtaksbrevDTO(val vedtakId: Long, val task: Task) : BehandlingStegDto()

data class SøknadDto(
    val søkerMedOpplysninger: SøkerMedOpplysningerDto,
    val barnaMedOpplysninger: List<BarnMedOpplysningerDto>,
    val endringAvOpplysningerBegrunnelse: String
)

fun SøknadDto.tilSøknadGrunnlag(behandlingId: Long): SøknadGrunnlag =
    SøknadGrunnlag(
        behandlingId = behandlingId,
        søknad = objectMapper.writeValueAsString(this)
    )

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
