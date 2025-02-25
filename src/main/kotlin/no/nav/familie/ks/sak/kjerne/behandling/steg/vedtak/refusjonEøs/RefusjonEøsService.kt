package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.refusjonEøs

import no.nav.familie.ks.sak.api.dto.RefusjonEøsDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RefusjonEøsService(
    @Autowired
    private val refusjonEøsRepository: RefusjonEøsRepository,
    @Autowired
    private val loggService: LoggService,
) {
    private fun hentRefusjonEøs(id: Long): RefusjonEøs =
        refusjonEøsRepository.finnRefusjonEøs(id)
            ?: throw Feil("Finner ikke refusjon eøs med id=$id")

    @Transactional
    fun leggTilRefusjonEøsPeriode(
        refusjonEøs: RefusjonEøsDto,
        behandlingId: Long,
    ): Long {
        val lagretPeriode =
            refusjonEøsRepository.save(
                RefusjonEøs(
                    behandlingId = behandlingId,
                    fom = refusjonEøs.fom,
                    tom = refusjonEøs.tom,
                    refusjonsbeløp = refusjonEøs.refusjonsbeløp,
                    land = refusjonEøs.land,
                    refusjonAvklart = refusjonEøs.refusjonAvklart,
                ),
            )

        loggService.loggRefusjonEøsPeriodeLagtTil(refusjonEøs = lagretPeriode)
        return lagretPeriode.id
    }

    @Transactional
    fun fjernRefusjonEøsPeriode(
        id: Long,
        behandlingId: Long,
    ) {
        loggService.loggRefusjonEøsPeriodeFjernet(
            refusjonEøs = hentRefusjonEøs(id),
        )
        refusjonEøsRepository.deleteById(id)
    }

    fun hentRefusjonEøsPerioder(behandlingId: Long) =
        refusjonEøsRepository
            .finnRefusjonEøsForBehandling(behandlingId = behandlingId)
            .map { tilRest(it) }

    @Transactional
    fun oppdaterRefusjonEøsPeriode(
        refusjonEøs: RefusjonEøsDto,
        id: Long,
    ) = hentRefusjonEøs(id).apply {
        fom = refusjonEøs.fom
        fom = refusjonEøs.fom
        tom = refusjonEøs.tom
        refusjonsbeløp = refusjonEøs.refusjonsbeløp
        land = refusjonEøs.land
        refusjonAvklart = refusjonEøs.refusjonAvklart
    }

    private fun tilRest(it: RefusjonEøs) =
        RefusjonEøsDto(
            id = it.id,
            fom = it.fom,
            tom = it.tom,
            refusjonsbeløp = it.refusjonsbeløp,
            land = it.land,
            refusjonAvklart = it.refusjonAvklart,
        )

    fun harRefusjonEøsPåBehandling(behandlingId: Long): Boolean = refusjonEøsRepository.finnRefusjonEøsForBehandling(behandlingId).isNotEmpty()
}
