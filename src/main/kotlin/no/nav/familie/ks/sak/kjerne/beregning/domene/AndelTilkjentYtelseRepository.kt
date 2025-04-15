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
    fun finnAndelerTilkjentYtelseForBehandlingOgBarn(
        behandlingId: Long,
        barnAktør: Aktør,
    ): List<AndelTilkjentYtelse>

    @Query(
        value = """
        SELECT aty
            FROM AndelTilkjentYtelse aty
                JOIN Behandling b ON aty.behandlingId = b.id
                JOIN Personident p ON aty.aktør.aktørId = p.aktør.aktørId
            WHERE p.fødselsnummer = :ident
                AND p.aktiv
                AND b.aktiv 
    """,
    )
    fun finnAktiveAndelerForIdent(ident: String): List<AndelTilkjentYtelse>

    @Query(value = "SELECT aty from AndelTilkjentYtelse aty WHERE aty.aktør = :aktør")
    fun finnAndelerTilkjentYtelseForAktør(aktør: Aktør): List<AndelTilkjentYtelse>

    @Query(
        value = """ SELECT aty FROM AndelTilkjentYtelse aty WHERE aty.behandlingId IN :behandlingIder AND 
            aty.stønadTom >= :avstemmingstidspunkt """,
    )
    fun finnLøpendeAndelerTilkjentYtelseForBehandlinger(
        behandlingIder: List<Long>,
        avstemmingstidspunkt: YearMonth,
    ): List<AndelTilkjentYtelse>

    @Query(
        """
        WITH andeler AS (
            SELECT
             aty.id,
             row_number() OVER (PARTITION BY aty.fk_aktoer_id ORDER BY aty.periode_offset DESC) rn
             FROM andel_tilkjent_ytelse aty
              JOIN tilkjent_ytelse ty ON ty.id = aty.tilkjent_ytelse_id
              JOIN Behandling b ON b.id = aty.fk_behandling_id
             WHERE b.fk_fagsak_id = :fagsakId
               AND ty.utbetalingsoppdrag IS NOT NULL
               AND json_extract_path_text(cast(ty.utbetalingsoppdrag as json), 'utbetalingsperiode') != '[]'
               AND aty.periode_offset IS NOT NULL
               AND b.status = 'AVSLUTTET')
        SELECT aty.* FROM andel_tilkjent_ytelse aty WHERE id IN (SELECT id FROM andeler WHERE rn = 1)
    """,
        nativeQuery = true,
    )
    fun hentSisteAndelPerIdent(fagsakId: Long): List<AndelTilkjentYtelse>
}
