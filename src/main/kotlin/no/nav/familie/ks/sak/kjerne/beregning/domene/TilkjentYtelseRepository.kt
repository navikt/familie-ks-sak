package no.nav.familie.ks.sak.kjerne.beregning.domene

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.eøs.differanseberegning.AndelTilkjentYtelsePraktiskLikhet.erIPraksisLik
import no.nav.familie.ks.sak.kjerne.eøs.differanseberegning.AndelTilkjentYtelsePraktiskLikhet.inneholderIPraksis
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface TilkjentYtelseRepository : JpaRepository<TilkjentYtelse, Long> {
    @Query("SELECT ty FROM TilkjentYtelse ty JOIN ty.behandling b WHERE b.id = :behandlingId")
    fun hentTilkjentYtelseForBehandling(behandlingId: Long): TilkjentYtelse

    @Query("SELECT ty FROM TilkjentYtelse ty JOIN ty.behandling b WHERE b.id = :behandlingId")
    fun hentOptionalTilkjentYtelseForBehandling(behandlingId: Long): TilkjentYtelse?

    @Modifying
    @Query("DELETE FROM TilkjentYtelse ty WHERE ty.behandling = :behandling")
    fun slettTilkjentYtelseForBehandling(behandling: Behandling)

    @Query("SELECT ty FROM TilkjentYtelse ty JOIN ty.behandling b WHERE b.id = :behandlingId AND ty.utbetalingsoppdrag is not null")
    fun finnByBehandlingAndHasUtbetalingsoppdrag(behandlingId: Long): TilkjentYtelse?
}

fun TilkjentYtelseRepository.oppdaterTilkjentYtelse(
    tilkjentYtelse: TilkjentYtelse,
    oppdaterteAndeler: Collection<AndelTilkjentYtelse>,
): TilkjentYtelse {
    if (tilkjentYtelse.andelerTilkjentYtelse.erIPraksisLik(oppdaterteAndeler)) {
        return tilkjentYtelse
    }

    // Her er det viktig å beholde de originale andelene, som styres av JPA og har alt av innhold
    val skalBeholdes =
        tilkjentYtelse.andelerTilkjentYtelse
            .filter { oppdaterteAndeler.inneholderIPraksis(it) }

    val skalLeggesTil =
        oppdaterteAndeler
            .filter { !tilkjentYtelse.andelerTilkjentYtelse.inneholderIPraksis(it) }

    // Forsikring: Sjekk at det ikke oppstår eller forsvinner andeler når de sjekkes for likhet
    if (oppdaterteAndeler.size != (skalBeholdes.size + skalLeggesTil.size)) {
        throw Feil("Avvik mellom antall innsendte andeler og kalkulerte endringer")
    }

    tilkjentYtelse.andelerTilkjentYtelse.clear()
    tilkjentYtelse.andelerTilkjentYtelse.addAll(skalBeholdes + skalLeggesTil)

    return this.saveAndFlush(tilkjentYtelse)
}
