package no.nav.familie.ks.sak.kjerne.eøs.kompetanse

import no.nav.familie.ks.sak.api.dto.KompetanseDto
import no.nav.familie.ks.sak.api.dto.tilKompetanse
import no.nav.familie.ks.sak.common.BehandlingId
import no.nav.familie.ks.sak.kjerne.eøs.felles.EøsSkjemaService
import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.EøsSkjemaRepository
import no.nav.familie.ks.sak.kjerne.eøs.felles.endringsabonnent.EøsSkjemaEndringAbonnent
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.UtfyltKompetanse
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.tilIKompetanse
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class KompetanseService(
    kompetanseRepository: EøsSkjemaRepository<Kompetanse>,
    kompetanseEndringsAbonnenter: List<EøsSkjemaEndringAbonnent<Kompetanse>>,
    private val personidentService: PersonidentService,
    private val tilpassKompetanserService: TilpassKompetanserService,
) {
    private val kompetanseSkjemaService = EøsSkjemaService(kompetanseRepository, kompetanseEndringsAbonnenter)

    fun hentKompetanser(behandlingId: BehandlingId) = kompetanseSkjemaService.hentMedBehandlingId(behandlingId)

    fun hentUtfylteKompetanser(behandlingId: BehandlingId) = hentKompetanser(behandlingId).map { it.tilIKompetanse() }.filterIsInstance<UtfyltKompetanse>()

    @Transactional
    fun oppdaterKompetanse(
        behandlingId: BehandlingId,
        oppdateresKompetanseDto: KompetanseDto,
    ) {
        val barnAktører = oppdateresKompetanseDto.barnIdenter.map { personidentService.hentAktør(it) }
        val oppdateresKompetanse = oppdateresKompetanseDto.tilKompetanse(barnAktører)

        kompetanseSkjemaService.endreSkjemaer(behandlingId, oppdateresKompetanse)
    }

    // Oppretter kompetanse skjema i behandlingsresultat
    // når vilkårer er vurdert etter EØS forordningen i vilkårsvurdering for det første gang
    // Tilpasser kompetanse skjema basert på endringer i vilkårsvurdering deretter
    @Transactional
    fun tilpassKompetanse(behandlingId: BehandlingId) = tilpassKompetanserService.tilpassKompetanser(behandlingId)

    @Transactional
    fun slettKompetanse(kompetanseId: Long) = kompetanseSkjemaService.slettSkjema(kompetanseId)

    @Transactional
    fun kopierOgErstattKompetanser(
        fraBehandlingId: BehandlingId,
        tilBehandlingId: BehandlingId,
    ) = kompetanseSkjemaService.kopierOgErstattSkjemaer(fraBehandlingId, tilBehandlingId)
}
