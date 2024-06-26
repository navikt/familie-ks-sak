package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.cucumber.StepDefinition
import no.nav.familie.ks.sak.data.lagVedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.VedtakRepository

fun mockVedtakRepository(stepDefinition: StepDefinition): VedtakRepository {
    val vedtakRepository = mockk<VedtakRepository>()
    every { vedtakRepository.findByBehandlingAndAktiv(any()) } answers {
        val behandlingId = firstArg<Long>()
        opprettEllerHentVedtak(stepDefinition, behandlingId)
    }
    every { vedtakRepository.getReferenceById(any()) } answers {
        val vedtakId = firstArg<Long>()
        stepDefinition.vedtakslister.first { it.id == vedtakId }
    }
    every { vedtakRepository.findByBehandlingAndAktivOptional(any()) } answers {
        val behandlingId = firstArg<Long>()
        opprettEllerHentVedtak(stepDefinition, behandlingId)
    }
    every { vedtakRepository.save(any()) } answers {
        val oppdatertVedtak = firstArg<Vedtak>()
        lagreVedtak(stepDefinition, oppdatertVedtak)
    }
    every { vedtakRepository.saveAndFlush(any()) } answers {
        val oppdatertVedtak = firstArg<Vedtak>()
        lagreVedtak(stepDefinition, oppdatertVedtak)
    }
    return vedtakRepository
}

private fun lagreVedtak(
    stepDefinition: StepDefinition,
    oppdatertVedtak: Vedtak,
): Vedtak {
    stepDefinition.vedtakslister =
        stepDefinition.vedtakslister.map { if (it.id == oppdatertVedtak.id) oppdatertVedtak else it }.toMutableList()
    if (oppdatertVedtak.id !in stepDefinition.vedtakslister.map { it.id }) {
        stepDefinition.vedtakslister.add(oppdatertVedtak)
    }
    return oppdatertVedtak
}

private fun opprettEllerHentVedtak(
    stepDefinition: StepDefinition,
    behandlingId: Long,
): Vedtak {
    val vedtakForBehandling =
        stepDefinition.vedtakslister.find { it.behandling.id == behandlingId }
            ?: lagVedtak(stepDefinition.behandlinger[behandlingId]!!)

    if (vedtakForBehandling !in stepDefinition.vedtakslister) {
        stepDefinition.vedtakslister.add(vedtakForBehandling)
    }

    return vedtakForBehandling
}
