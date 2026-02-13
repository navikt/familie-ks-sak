package no.nav.familie.ks.sak.api.dto

import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.Beslutning
import no.nav.familie.ks.sak.kjerne.behandling.steg.VenteÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.registrersøknad.domene.SøknadGrunnlag
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Målform
import no.nav.familie.prosessering.domene.Task
import java.time.LocalDate

abstract class BehandlingStegDto

data class RegistrerSøknadDto(
    val søknad: SøknadDto,
    val bekreftEndringerViaFrontend: Boolean,
) : BehandlingStegDto()

data class JournalførVedtaksbrevDTO(
    val vedtakId: Long,
    val task: Task,
) : BehandlingStegDto()

data class BesluttVedtakDto(
    val beslutning: Beslutning,
    val begrunnelse: String?,
    val kontrollerteSider: List<String> = emptyList(),
) : BehandlingStegDto()

data class IverksettMotOppdragDto(
    val behandlingId: Long,
    val saksbehandlerId: String,
) : BehandlingStegDto()

data class SøknadDto(
    val søkerMedOpplysninger: SøkerMedOpplysningerDto,
    val barnaMedOpplysninger: List<BarnMedOpplysningerDto>,
    val endringAvOpplysningerBegrunnelse: String,
)

fun SøknadDto.writeValueAsString(): String = jsonMapper.writeValueAsString(this)

fun SøknadDto.tilSøknadGrunnlag(behandlingId: Long): SøknadGrunnlag =
    SøknadGrunnlag(
        behandlingId = behandlingId,
        søknad = jsonMapper.writeValueAsString(this),
    )

data class SøkerMedOpplysningerDto(
    val ident: String,
    val målform: Målform = Målform.NB,
)

data class BarnMedOpplysningerDto(
    val ident: String,
    val navn: String = "",
    val fødselsdato: LocalDate? = null,
    val inkludertISøknaden: Boolean = true,
    val erFolkeregistrert: Boolean = true,
) : BehandlingStegDto() {
    val personnummer: String? = if (ident == "") null else ident
}

data class BehandlingPåVentDto(
    val frist: LocalDate,
    val årsak: VenteÅrsak,
)

data class HenleggBehandlingDto(
    val årsak: HenleggÅrsak,
    val begrunnelse: String,
)

enum class HenleggÅrsak(
    val beskrivelse: String,
) {
    SØKNAD_TRUKKET("Søknad trukket"),
    FEILAKTIG_OPPRETTET("Behandling feilaktig opprettet"),
    TEKNISK_VEDLIKEHOLD("Teknisk vedlikehold"),
    ;

    fun tilBehandlingsresultat() =
        when (this) {
            FEILAKTIG_OPPRETTET -> Behandlingsresultat.HENLAGT_FEILAKTIG_OPPRETTET
            SØKNAD_TRUKKET -> Behandlingsresultat.HENLAGT_SØKNAD_TRUKKET
            TEKNISK_VEDLIKEHOLD -> Behandlingsresultat.HENLAGT_TEKNISK_VEDLIKEHOLD
        }
}

data class TilbakekrevingRequestDto(
    val valg: Tilbakekrevingsvalg,
    val varsel: String? = null,
    val begrunnelse: String,
    val tilbakekrevingsbehandlingId: String? = null,
) : BehandlingStegDto()
