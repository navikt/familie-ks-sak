package no.nav.familie.ks.sak.kjerne.eøs.kompetanse

import no.nav.familie.ks.sak.api.dto.KompetanseDto
import no.nav.familie.ks.sak.api.dto.tilKompetanse
import no.nav.familie.ks.sak.kjerne.eøs.felles.EøsSkjemaService
import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.EøsSkjemaRepository
import no.nav.familie.ks.sak.kjerne.eøs.felles.endringsabonnent.EøsSkjemaEndringAbonnent
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class KompetanseService(
    kompetanseRepository: EøsSkjemaRepository<Kompetanse>,
    kompetanseEndringsAbonnenter: List<EøsSkjemaEndringAbonnent<Kompetanse>>,
    private val personidentService: PersonidentService
) {

    private val skjemaService = EøsSkjemaService(kompetanseRepository, kompetanseEndringsAbonnenter)

    fun hentKompetanse(kompetanseId: Long) = skjemaService.hentMedId(kompetanseId)

    fun hentKompetanser(behandlingId: Long) = skjemaService.hentMedBehandlingId(behandlingId)

    @Transactional
    fun oppdaterKompetanse(behandlingId: Long, oppdateresKompetanseDto: KompetanseDto) {
        val barnAktører = oppdateresKompetanseDto.barnIdenter.map { personidentService.hentAktør(it) }
        val oppdateresKompetanse = oppdateresKompetanseDto.tilKompetanse(barnAktører)

        skjemaService.endreSkjemaer(behandlingId, oppdateresKompetanse)
    }

    @Transactional
    fun slettKompetanse(kompetanseId: Long) = skjemaService.slettSkjema(kompetanseId)
}
