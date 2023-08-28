package no.nav.familie.ks.sak.kjerne.behandling.steg.registrersøknad.domene

import no.nav.familie.ks.sak.api.dto.BarnMedOpplysningerDto
import no.nav.familie.ks.sak.api.mapper.SøknadGrunnlagMapper.tilSøknadDto
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.SequenceGenerator
import javax.persistence.Table

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
    fun hentUregistrerteBarn(): List<BarnMedOpplysningerDto> =
        tilSøknadDto().barnaMedOpplysninger.filter { !it.erFolkeregistrert && it.inkludertISøknaden }
}
