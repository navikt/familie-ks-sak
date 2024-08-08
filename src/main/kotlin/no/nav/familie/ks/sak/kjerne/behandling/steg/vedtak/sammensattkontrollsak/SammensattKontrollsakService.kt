package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.sammensattkontrollsak

import no.nav.familie.ks.sak.api.dto.OppdaterSammensattKontrollsakDto
import no.nav.familie.ks.sak.api.dto.OpprettSammensattKontrollsakDto
import no.nav.familie.ks.sak.api.dto.SlettSammensattKontrollsakDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SammensattKontrollsakService(
    private val sammensattKontrollsakRepository: SammensattKontrollsakRepository,
    private val loggService: LoggService,
) {
    fun finnSammensattKontrollsak(
        sammensattKontrollsakId: Long,
    ): SammensattKontrollsak? =
        sammensattKontrollsakRepository.finnSammensattKontrollsak(
            sammensattKontrollsakId = sammensattKontrollsakId,
        )

    fun finnSammensattKontrollsakForBehandling(
        behandlingId: Long,
    ): SammensattKontrollsak? =
        sammensattKontrollsakRepository.finnSammensattKontrollsakForBehandling(
            behandlingId = behandlingId,
        )

    @Transactional
    fun opprettSammensattKontrollsak(
        opprettSammensattKontrollsakDto: OpprettSammensattKontrollsakDto,
    ): SammensattKontrollsak {
        val lagretSammensattKontrollsak =
            sammensattKontrollsakRepository.save(
                SammensattKontrollsak(
                    behandlingId = opprettSammensattKontrollsakDto.behandlingId,
                    fritekst = opprettSammensattKontrollsakDto.fritekst,
                ),
            )
        loggService.opprettSammensattKontrollsakOpprettetLogg(
            behandlingId = lagretSammensattKontrollsak.behandlingId,
        )
        return lagretSammensattKontrollsak
    }

    @Transactional
    fun oppdaterSammensattKontrollsak(
        oppdaterSammensattKontrollsakDto: OppdaterSammensattKontrollsakDto,
    ): SammensattKontrollsak {
        val eksisterendeSammensattKontrollsak =
            sammensattKontrollsakRepository.finnSammensattKontrollsak(
                sammensattKontrollsakId = oppdaterSammensattKontrollsakDto.id,
            ) ?: throw Feil(
                "Fant ingen eksisterende sammensatt kontrollsak for id=${oppdaterSammensattKontrollsakDto.id}",
            )
        eksisterendeSammensattKontrollsak.fritekst = oppdaterSammensattKontrollsakDto.fritekst
        val lagretSammensattKontrollsak =
            sammensattKontrollsakRepository.save(
                eksisterendeSammensattKontrollsak,
            )
        loggService.opprettSammensattKontrollsakOppdatertLogg(
            behandlingId = lagretSammensattKontrollsak.behandlingId,
        )
        return lagretSammensattKontrollsak
    }

    @Transactional
    fun slettSammensattKontrollsak(
        slettSammensattKontrollsakDto: SlettSammensattKontrollsakDto,
    ) {
        val sammensattKontrollsak =
            sammensattKontrollsakRepository.finnSammensattKontrollsak(
                sammensattKontrollsakId = slettSammensattKontrollsakDto.id,
            ) ?: throw Feil(
                "Fant ingen eksisterende sammensatt kontrollsak for id=${slettSammensattKontrollsakDto.id}",
            )
        sammensattKontrollsakRepository.deleteById(
            sammensattKontrollsak.id,
        )
        loggService.opprettSammensattKontrollsakSlettetLogg(
            behandlingId = sammensattKontrollsak.behandlingId,
        )
    }
}
