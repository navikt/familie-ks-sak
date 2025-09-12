package no.nav.familie.ks.sak.barnehagelister.domene

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface BarnehagebarnRepository : JpaRepository<Barnehagebarn, UUID> { // , JpaSpecificationExecutor<Barnehagebarn>
    fun findAllByIdent(ident: String): List<Barnehagebarn>

    @Query(
        """    
    WITH input AS (
        -- Referer til disse variablene flere ganger så de må legges inn slik
        SELECT 
        CAST(:ident AS TEXT) as ident_param,
        CAST(:kommuneNavn AS TEXT) as kommunenavn_param
    ),
    siste_iverksatte_behandling_i_løpende_fagsak AS (
    -- Finn nyeste iverksatte behandling med utbetaling for løpende og ikke-arkivert fagsak for barn
    SELECT 
        f.id AS fagsakid, 
        MAX(b.aktivert_tid) AS aktivert_tid
    FROM personident p
             JOIN andel_tilkjent_ytelse aty ON p.fk_aktoer_id = aty.fk_aktoer_id
             JOIN behandling b ON aty.fk_behandling_id = b.id
             JOIN fagsak f ON b.fk_fagsak_id = f.id
             JOIN tilkjent_ytelse ty ON b.id = ty.fk_behandling_id
             CROSS JOIN input i
    WHERE (i.ident_param IS NULL OR p.foedselsnummer = i.ident_param)
      AND p.aktiv = true
      AND f.status = 'LØPENDE'
      AND f.arkivert = false
      AND ty.utbetalingsoppdrag IS NOT NULL
    GROUP BY f.id
),
lopende_andel_i_siste_iverksatte_behandling AS (
    -- Sjekker om barnet har en løpende andel i siste iverksatte behandling 
    SELECT 
        p.foedselsnummer, 
        aty.stonad_tom + interval '1 month - 1 day' >= current_date AS lopende_andel -- stonad_tom blir satt til første dag i mnd, men vi ønsker å ha med hele måneden i filtreringen
    FROM personident p
             JOIN andel_tilkjent_ytelse aty ON p.fk_aktoer_id = aty.fk_aktoer_id
             JOIN behandling b ON aty.fk_behandling_id = b.id
             JOIN fagsak f ON b.fk_fagsak_id = f.id
             JOIN siste_iverksatte_behandling_i_løpende_fagsak iblf
                  ON f.id = iblf.fagsakid
                  AND b.aktivert_tid = iblf.aktivert_tid
    CROSS JOIN input i
    WHERE (i.ident_param IS NULL OR p.foedselsnummer = i.ident_param)
      AND p.aktiv = true
),
avvik_antall_timer_vilkar_resultat_og_barnehagebarn AS (
    SELECT 
        bb.id as barnehagebarn_id,
           CASE
               WHEN bb.antall_timer_i_barnehage < 33 -- minst 33 timer barnehageplass regnes som fulltidsplass
                   THEN vr.antall_timer != bb.antall_timer_i_barnehage
               ELSE vr.antall_timer <= 33
           END as avvik
    FROM barnehagebarn bb
             JOIN personident p ON bb.ident = p.foedselsnummer AND p.aktiv
             JOIN person_resultat pr ON pr.fk_aktoer_id = p.fk_aktoer_id
             JOIN vilkar_resultat vr
                  ON pr.id = vr.fk_person_resultat_id
                      AND vr.vilkar = 'BARNEHAGEPLASS'
                      AND bb.fom = vr.periode_fom::date
),
barnehagebarn_visning AS (
    --Må trekkes ut til egen CTE for at sortering for pageables skal fungere på felter som ikke ligger i barnehagebarn fra før
    SELECT bb.ident,
           bb.fom,
           bb.tom,
           bb.antall_timer_i_barnehage AS antallTimerBarnehage,
           bb.endringstype,
           bb.kommune_navn AS kommuneNavn,
           bb.kommune_nr AS kommuneNr,
           MAX(bb.endret_tid) AS endretTid,
           atv.avvik
    FROM barnehagebarn bb
             LEFT JOIN lopende_andel_i_siste_iverksatte_behandling lab
                       ON bb.ident = lab.foedselsnummer
             LEFT JOIN avvik_antall_timer_vilkar_resultat_og_barnehagebarn atv
                       ON bb.id = atv.barnehagebarn_id
    CROSS JOIN input i
    WHERE (NOT :kunLøpendeAndeler OR lab.lopende_andel = true)
      AND (i.ident_param IS NULL OR bb.ident = i.ident_param)
      AND (i.kommunenavn_param IS NULL OR bb.kommune_navn ILIKE i.kommunenavn_param)
    GROUP BY bb.ident, bb.fom, bb.tom, bb.antall_timer_i_barnehage, bb.endringstype,
             bb.kommune_navn, bb.kommune_nr, atv.avvik
)
SELECT ident,
       fom,
       tom,
       antallTimerBarnehage,
       endringstype,
       kommuneNavn,
       kommuneNr,
       endretTid,
       avvik
FROM barnehagebarn_visning;     
 """,
        nativeQuery = true,
    )
    fun finnBarnehagebarn(
        kunLøpendeAndeler: Boolean,
        ident: String?,
        kommuneNavn: String?,
        pageable: Pageable,
    ): Page<BarnehagebarnPaginerbar>

    @Query(
        """
            SELECT DISTINCT bb.kommuneNavn FROM Barnehagebarn bb
        """,
    )
    fun hentAlleKommuner(): Set<String>
}
