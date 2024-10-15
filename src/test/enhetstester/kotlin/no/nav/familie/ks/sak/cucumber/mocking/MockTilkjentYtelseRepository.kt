package no.nav.familie.ks.sak.cucumber.mocking

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.cucumber.StepDefinition
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import java.time.LocalDate

fun mockTilkjentYtelseRepository(stepDefinition: StepDefinition): TilkjentYtelseRepository {
    val tilkjentYtelseRepository = mockk<TilkjentYtelseRepository>()
    every { tilkjentYtelseRepository.hentTilkjentYtelseForBehandling(any()) } answers {
        val behandlingId = firstArg<Long>()
        stepDefinition.andelerTilkjentYtelse[behandlingId]!!.tilTilkjentYtelse(stepDefinition)
    }
    every { tilkjentYtelseRepository.hentOptionalTilkjentYtelseForBehandling(any()) } answers {
        val behandlingId = firstArg<Long>()
        stepDefinition.andelerTilkjentYtelse[behandlingId]?.tilTilkjentYtelse(stepDefinition)
    }
    every { tilkjentYtelseRepository.slettTilkjentYtelseForBehandling(any()) } answers {
        val behandling = firstArg<Behandling>()
        stepDefinition.andelerTilkjentYtelse[behandling.id] = emptyList()
    }
    every { tilkjentYtelseRepository.save(any()) } answers {
        val tilkjentYtelse = firstArg<TilkjentYtelse>()
        stepDefinition.andelerTilkjentYtelse[tilkjentYtelse.behandling.id] =
            tilkjentYtelse.andelerTilkjentYtelse.toList()

        tilkjentYtelse
    }
    return tilkjentYtelseRepository
}

fun Collection<AndelTilkjentYtelse>.tilTilkjentYtelse(stepDefinition: StepDefinition): TilkjentYtelse {
    val behandlingId = this.map { it.behandlingId }.toSet().single()
    val behandling = stepDefinition.behandlinger[behandlingId]!!

    return TilkjentYtelse(
        andelerTilkjentYtelse = this.toMutableSet(),
        behandling = behandling,
        endretDato = LocalDate.now(),
        opprettetDato = LocalDate.now(),
    )
}
