package no.nav.familie.ks.sak.cucumber.mocking

import io.mockk.mockk
import no.nav.familie.ks.sak.cucumber.StepDefinition
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository

fun mockAndelTilkjentYtelseRepository(stepDefinition: StepDefinition): AndelTilkjentYtelseRepository {
    val andelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()

    return andelTilkjentYtelseRepository
}
