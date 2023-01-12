package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.feilutbetaltvaluta

import no.nav.familie.ks.sak.api.dto.FeilutbetaltValutaDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FeilutbetaltValutaService(
    private val feilutbetaltValutaRepository: FeilutbetaltValutaRepository,
    private val loggService: LoggService
) {

    fun hentFeilutbetaltValuta(id: Long): FeilutbetaltValuta =
        feilutbetaltValutaRepository.finnFeilutbetaltValuta(id)
            ?: throw Feil("Finner ikke feilutbetalt valuta med id=$id")

    @Transactional
    fun leggTilFeilutbetaltValuta(feilutbetaltValuta: FeilutbetaltValuta): FeilutbetaltValuta {
        val lagretFeilutbetaltValuta = feilutbetaltValutaRepository.save(feilutbetaltValuta)

        loggService.opprettFeilutbetaltValutaLagtTilLogg(
            feilutbetaltValuta = lagretFeilutbetaltValuta
        )

        return lagretFeilutbetaltValuta
    }

    @Transactional
    fun fjernFeilutbetaltValuta(id: Long) {
        loggService.opprettFeilutbetaltValutaFjernetLogg(feilutbetaltValuta = hentFeilutbetaltValuta(id))

        feilutbetaltValutaRepository.deleteById(id)
    }

    fun hentAlleFeilutbetaltValutaForBehandling(behandlingId: Long) =
        feilutbetaltValutaRepository.finnFeilutbetalteValutaForBehandling(behandlingId = behandlingId)

    @Transactional
    fun oppdaterFeilutbetaltValuta(oppdatertFeilutbetaltValuta: FeilutbetaltValutaDto, id: Long): FeilutbetaltValuta {
        val feilutbetaltValuta = hentFeilutbetaltValuta(id)

        feilutbetaltValuta.fom = oppdatertFeilutbetaltValuta.fom
        feilutbetaltValuta.tom = oppdatertFeilutbetaltValuta.tom
        feilutbetaltValuta.feilutbetaltBeløp = oppdatertFeilutbetaltValuta.feilutbetaltBeløp

        return feilutbetaltValuta
    }
}
