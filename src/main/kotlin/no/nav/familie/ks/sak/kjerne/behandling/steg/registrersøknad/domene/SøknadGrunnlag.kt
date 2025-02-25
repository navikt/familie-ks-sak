package no.nav.familie.ks.sak.kjerne.behandling.steg.registrersøknad.domene

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ks.sak.api.dto.BarnMedOpplysningerDto
import no.nav.familie.ks.sak.api.mapper.SøknadGrunnlagMapper.tilSøknadDto
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import java.time.LocalDateTime

@Entity
@Table(name = "gr_soknad")
data class SøknadGrunnlag(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gr_soknad_seq_generator")
    @SequenceGenerator(name = "gr_soknad_seq_generator", sequenceName = "gr_soknad_seq", allocationSize = 50)
    val id: Long = 0,
    @Column(name = "opprettet_av", nullable = false, updatable = false)
    val opprettetAv: String = SikkerhetContext.hentSaksbehandler(),
    @Column(name = "opprettet_tid", nullable = false, updatable = false)
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "fk_behandling_id", updatable = false, nullable = false)
    val behandlingId: Long,
    @Column(name = "soknad", nullable = false, columnDefinition = "text")
    var søknad: String,
    @Column(name = "aktiv", nullable = false)
    var aktiv: Boolean = true,
) {
    fun hentUregistrerteBarn(): List<BarnMedOpplysningerDto> = tilSøknadDto().barnaMedOpplysninger.filter { !it.erFolkeregistrert && it.inkludertISøknaden }
}
