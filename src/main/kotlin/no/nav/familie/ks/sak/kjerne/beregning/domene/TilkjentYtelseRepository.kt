package no.nav.familie.ks.sak.kjerne.beregning.domene

import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface TilkjentYtelseRepository : JpaRepository<TilkjentYtelse, Long> {
    @Query("SELECT ty FROM TilkjentYtelse ty JOIN ty.behandling b WHERE b.id = :behandlingId")
    fun hentTilkjentYtelseForBehandling(behandlingId: Long): TilkjentYtelse

    @Modifying
    @Query("DELETE FROM TilkjentYtelse ty WHERE ty.behandling = :behandling")
    fun slettTilkjentYtelseForBehandling(behandling: Behandling)
}
