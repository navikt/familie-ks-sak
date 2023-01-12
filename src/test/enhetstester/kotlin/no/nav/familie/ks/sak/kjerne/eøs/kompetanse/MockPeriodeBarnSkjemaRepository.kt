package no.nav.familie.ks.sak.kjerne.eøs.kompetanse

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.EøsSkjemaEntitet
import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.EøsSkjemaRepository
import java.util.concurrent.atomic.AtomicLong

fun <T : EøsSkjemaEntitet<T>> mockPeriodeBarnSkjemaRepository(): EøsSkjemaRepository<T> {
    val minnebasertSkjemaRepository = MinnebasertSkjemaRepository<T>()
    val mockSkjemaRepository = mockk<EøsSkjemaRepository<T>>()

    val idSlot = slot<Long>()
    val skjemaListeSlot = slot<Iterable<T>>()

    every { mockSkjemaRepository.findByBehandlingId(capture(idSlot)) } answers {
        minnebasertSkjemaRepository.hentSkjemaer(idSlot.captured)
    }

    every { mockSkjemaRepository.getReferenceById(capture(idSlot)) } answers {
        minnebasertSkjemaRepository.hentSkjema(idSlot.captured)
    }

    every { mockSkjemaRepository.saveAll(capture(skjemaListeSlot)) } answers {
        minnebasertSkjemaRepository.save(skjemaListeSlot.captured)
    }

    every { mockSkjemaRepository.deleteAll(capture(skjemaListeSlot)) } answers {
        minnebasertSkjemaRepository.delete(skjemaListeSlot.captured)
    }

    every { mockSkjemaRepository.deleteAll() } answers {
        minnebasertSkjemaRepository.deleteAll()
    }

    return mockSkjemaRepository
}

private class MinnebasertSkjemaRepository<T> where T : EøsSkjemaEntitet<T> {

    private val løpenummer = AtomicLong()
    private fun AtomicLong.neste() = this.addAndGet(1)

    private val skjemaer = mutableMapOf<Long, T>()

    fun hentSkjemaer(behandlingId: Long): List<T> = skjemaer.values.filter { it.behandlingId == behandlingId }

    fun hentSkjema(skjemaId: Long): T =
        skjemaer[skjemaId] ?: throw IllegalArgumentException("Finner ikke skjema for id $skjemaId")

    fun save(skjemaer: Iterable<T>) = skjemaer.map { save(it) }

    private fun save(skjema: T): T {
        if (skjema.id == 0L) {
            skjema.id = løpenummer.neste()
        }

        skjemaer[skjema.id] = skjema
        return skjema
    }

    fun delete(tilSletting: Iterable<T>) = tilSletting.forEach { skjemaer.remove(it.id) }

    fun deleteAll() = skjemaer.clear()
}

fun <T : EøsSkjemaEntitet<T>> T.lagreTil(eøsSkjemaRepository: EøsSkjemaRepository<T>): T =
    eøsSkjemaRepository.saveAll(listOf(this)).first()

fun <T : EøsSkjemaEntitet<T>> List<T>.lagreTil(eøsSkjemaRepository: EøsSkjemaRepository<T>): T =
    eøsSkjemaRepository.saveAll(this).first()
