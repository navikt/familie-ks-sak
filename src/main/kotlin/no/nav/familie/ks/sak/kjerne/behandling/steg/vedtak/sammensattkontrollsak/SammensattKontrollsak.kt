package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.sammensattkontrollsak

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ks.sak.api.dto.SammensattKontrollsakDto
import no.nav.familie.ks.sak.common.entitet.BaseEntitet

@Entity(name = "SammensattKontrollsak")
@Table(name = "sammensatt_kontrollsak")
data class SammensattKontrollsak(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sammensatt_kontrollsak_seq_generator")
    @SequenceGenerator(
        name = "sammensatt_kontrollsak_seq_generator",
        sequenceName = "sammensatt_kontrollsak_seq",
        allocationSize = 50,
    )
    val id: Long = 0L,
    @Column(name = "fk_behandling_id", updatable = false, nullable = false)
    val behandlingId: Long,
    @Column(name = "fritekst", nullable = false)
    var fritekst: String,
) : BaseEntitet()

fun SammensattKontrollsak.tilSammensattKontrollDto(): SammensattKontrollsakDto =
    SammensattKontrollsakDto(
        id = this.id,
        behandlingId = this.behandlingId,
        fritekst = this.fritekst,
    )
