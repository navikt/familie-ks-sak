package no.nav.familie.ks.sak.kjerne.beregning.domene

import no.nav.familie.ks.sak.kjerne.personident.Aktør
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.YearMonth

interface AndelTilkjentYtelseRepository : JpaRepository<AndelTilkjentYtelse, Long> {

    @Query(value = "SELECT aty FROM AndelTilkjentYtelse aty WHERE aty.behandlingId IN :behandlingIder")
    fun finnAndelerTilkjentYtelseForBehandlinger(behandlingIder: List<Long>): List<AndelTilkjentYtelse>

    @Query(value = "SELECT aty FROM AndelTilkjentYtelse aty WHERE aty.behandlingId = :behandlingId")
    fun finnAndelerTilkjentYtelseForBehandling(behandlingId: Long): List<AndelTilkjentYtelse>

    @Query(value = "SELECT aty FROM AndelTilkjentYtelse aty WHERE aty.behandlingId = :behandlingId AND aty.aktør = :barnAktør")
    fun finnAndelerTilkjentYtelseForBehandlingOgBarn(behandlingId: Long, barnAktør: Aktør): List<AndelTilkjentYtelse>

    @Query(
        value = """ SELECT aty FROM AndelTilkjentYtelse aty WHERE aty.behandlingId IN :behandlingIder AND 
            aty.stønadTom >= :avstemmingstidspunkt """
    )
    fun finnLøpendeAndelerTilkjentYtelseForBehandlinger(
        behandlingIder: List<Long>,
        avstemmingstidspunkt: YearMonth
    ): List<AndelTilkjentYtelse>
}
