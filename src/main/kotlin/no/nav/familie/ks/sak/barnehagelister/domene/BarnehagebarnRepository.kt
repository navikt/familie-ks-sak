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
    SELECT bb.ident                    as ident,
       bb.fom                      as fom,
       bb.tom                      as tom,
       bb.antall_timer_i_barnehage as antallTimerIBarnehage,
       bb.endringstype             as endringstype,
       bb.kommune_navn             as kommuneNavn,
       bb.kommune_nr               as kommuneNr,
       MAX(bb.endret_tid)          as endretTid,
       f.id                        as fagsakId,
       f.status                    as fagsakstatus,
       vr.antall_timer != bb.antall_timer_i_barnehage as avvik
    FROM barnehagebarn bb
        INNER JOIN personident p ON bb.ident = p.foedselsnummer 
            AND p.aktiv = true
        INNER JOIN po_person pp ON p.fk_aktoer_id = pp.fk_aktoer_id
        INNER JOIN gr_personopplysninger go ON pp.fk_gr_personopplysninger_id = go.id
        INNER JOIN behandling b ON go.fk_behandling_id = b.id 
            AND b.aktiv = true
        INNER JOIN fagsak f ON b.fk_fagsak_id = f.id 
            AND f.arkivert = false
        INNER JOIN andel_tilkjent_ytelse a on a.fk_behandling_id = b.id 
            AND pp.fk_aktoer_id = a.fk_aktoer_id
        INNER JOIN person_resultat pr ON pr.fk_aktoer_id = pp.fk_aktoer_id
        LEFT JOIN vilkar_resultat vr ON vr.fk_person_resultat_id = pr.id
            AND b.id = vr.fk_behandling_id
            AND vr.periode_fom::date = bb.fom
            AND a.stonad_fom::date = vr.periode_fom::date
    WHERE (a.stonad_tom >= current_date)
    GROUP BY bb.ident, bb.fom, bb.tom, bb.antall_timer_i_barnehage, bb.endringstype, bb.kommune_navn, bb.kommune_nr, f.id, f.status, vr.antall_timer
""",
        nativeQuery = true,
    )
    fun finnAlleBarnehagebarnMedLøpendAndel(
        pageable: Pageable,
    ): Page<BarnehagebarnDtoInterface>

    @Query(
        """
    SELECT bb.ident                                       as ident,
       bb.fom                                         as fom,
       bb.tom                                         as tom,
       bb.antall_timer_i_barnehage                    as antallTimerIBarnehage,
       bb.endringstype                                as endringstype,
       bb.kommune_navn                                as kommuneNavn,
       bb.kommune_nr                                  as kommuneNr,
       MAX(bb.endret_tid)                             as endretTid,
       f.id                                           as fagsakId,
       f.status                                       as fagsakstatus,
       bb.antall_timer_i_barnehage != vr.antall_timer as avvik
    FROM barnehagebarn bb
         LEFT OUTER JOIN personident p ON bb.ident = p.foedselsnummer
         LEFT OUTER JOIN po_person pp ON p.fk_aktoer_id = pp.fk_aktoer_id
         LEFT OUTER JOIN gr_personopplysninger go ON pp.fk_gr_personopplysninger_id = go.id
         LEFT OUTER JOIN behandling b ON go.fk_behandling_id = b.id 
            AND b.aktiv = true
         LEFT OUTER JOIN fagsak f ON b.fk_fagsak_id = f.id 
            and f.arkivert = false
         INNER JOIN andel_tilkjent_ytelse a on a.fk_behandling_id = b.id 
            AND pp.fk_aktoer_id = a.fk_aktoer_id
         INNER JOIN person_resultat pr ON pr.fk_aktoer_id = pp.fk_aktoer_id
         LEFT JOIN vilkar_resultat vr ON vr.fk_person_resultat_id = pr.id
            AND b.id = vr.fk_behandling_id
            AND vr.periode_fom::date = bb.fom
            AND a.stonad_fom::date = vr.periode_fom::date
            AND a.stonad_tom::date >= current_date
    GROUP BY bb.ident, bb.fom, bb.tom, bb.antall_timer_i_barnehage, bb.endringstype, bb.kommune_navn, bb.kommune_nr, f.id, f.status, vr.antall_timer""",
        nativeQuery = true,
    )
    fun finnAlleBarnehagebarn(pageable: Pageable): Page<BarnehagebarnDtoInterface>

    @Query(
        """
    SELECT bb.ident                    as ident,
       bb.fom                      as fom,
       bb.tom                      as tom,
       bb.antall_timer_i_barnehage as antallTimerIBarnehage,
       bb.endringstype             as endringstype,
       bb.kommune_navn             as kommuneNavn,
       bb.kommune_nr               as kommuneNr,
       MAX(bb.endret_tid)          as endretTid,
       f.id                        as fagsakId,
       f.status                    as fagsakstatus,
       bb.antall_timer_i_barnehage != vr.antall_timer AS avvik
    FROM barnehagebarn bb
         INNER JOIN personident p ON bb.ident = p.foedselsnummer 
            AND p.aktiv = true
         INNER JOIN po_person pp ON p.fk_aktoer_id = pp.fk_aktoer_id
         INNER JOIN gr_personopplysninger go ON pp.fk_gr_personopplysninger_id = go.id
         INNER JOIN behandling b ON go.fk_behandling_id = b.id 
            AND b.aktiv = true
         INNER JOIN fagsak f ON b.fk_fagsak_id = f.id 
            AND f.arkivert = false
         INNER JOIN andel_tilkjent_ytelse a on a.fk_behandling_id = b.id 
            AND pp.fk_aktoer_id = a.fk_aktoer_id
         INNER JOIN person_resultat pr ON pr.fk_aktoer_id = pp.fk_aktoer_id
         LEFT JOIN vilkar_resultat vr ON vr.fk_person_resultat_id = pr.id
            AND b.id = vr.fk_behandling_id
            AND vr.periode_fom::date = bb.fom
    WHERE (a.stonad_tom >= current_date)
        AND bb.ident = :ident
    GROUP BY bb.ident, bb.fom, bb.tom, bb.antall_timer_i_barnehage, bb.endringstype, bb.kommune_navn, bb.kommune_nr, f.id, f.status, vr.antall_timer
""",
        nativeQuery = true,
    )
    fun finnBarnehagebarnByIdentMedLøpendeAndel(
        ident: String,
        pageable: Pageable,
    ): Page<BarnehagebarnDtoInterface>

    @Query(
        """
SELECT bb.ident                    as ident,
       bb.fom                      as fom,
       bb.tom                      as tom,
       bb.antall_timer_i_barnehage as antallTimerIBarnehage,
       bb.endringstype             as endringstype,
       bb.kommune_navn             as kommuneNavn,
       bb.kommune_nr               as kommuneNr,
       MAX(bb.endret_tid)          as endretTid,
       f.id                        as fagsakId,
       f.status                    as fagsakstatus,
       bb.antall_timer_i_barnehage != vr.antall_timer AS avvik
FROM barnehagebarn bb
         LEFT OUTER JOIN personident p ON bb.ident = p.foedselsnummer
         LEFT OUTER JOIN po_person pp ON p.fk_aktoer_id = pp.fk_aktoer_id
         LEFT OUTER JOIN gr_personopplysninger go ON pp.fk_gr_personopplysninger_id = go.id
         LEFT OUTER JOIN behandling b ON go.fk_behandling_id = b.id AND b.aktiv = true
         LEFT OUTER JOIN fagsak f ON b.fk_fagsak_id = f.id and f.arkivert = false
         INNER JOIN person_resultat pr ON pr.fk_aktoer_id = pp.fk_aktoer_id
         INNER JOIN andel_tilkjent_ytelse a on a.fk_behandling_id = b.id AND pp.fk_aktoer_id = a.fk_aktoer_id
         LEFT JOIN vilkar_resultat vr ON vr.fk_person_resultat_id = pr.id
            AND b.id = vr.fk_behandling_id
            AND vr.periode_fom::date = bb.fom
            AND vr.periode_fom::date = a.stonad_fom
            AND a.stonad_tom >= current_date
WHERE bb.ident = :ident
GROUP BY bb.ident, bb.fom, bb.tom, bb.antall_timer_i_barnehage, bb.endringstype, bb.kommune_navn, bb.kommune_nr, f.id, f.status, vr.antall_timer""",
        nativeQuery = true,
    )
    fun finnBarnehagebarnByIdent(
        ident: String,
        pageable: Pageable,
    ): Page<BarnehagebarnDtoInterface>

    @Query(
        """
SELECT bb.ident                    as ident,
       bb.fom                      as fom,
       bb.tom                      as tom,
       bb.antall_timer_i_barnehage as antallTimerIBarnehage,
       bb.endringstype             as endringstype,
       bb.kommune_navn             as kommuneNavn,
       bb.kommune_nr               as kommuneNr,
       MAX(bb.endret_tid)          as endretTid,
       f.id                        as fagsakId,
       f.status                    as fagsakstatus,
       bb.antall_timer_i_barnehage != vr.antall_timer AS avvik
FROM barnehagebarn bb
         INNER JOIN personident p ON bb.ident = p.foedselsnummer AND p.aktiv = true
         INNER JOIN po_person pp ON p.fk_aktoer_id = pp.fk_aktoer_id
         INNER JOIN gr_personopplysninger go ON pp.fk_gr_personopplysninger_id = go.id
         INNER JOIN behandling b ON go.fk_behandling_id = b.id AND b.aktiv = true
         INNER JOIN fagsak f ON b.fk_fagsak_id = f.id AND f.arkivert = false
         INNER JOIN andel_tilkjent_ytelse a on a.fk_behandling_id = b.id AND pp.fk_aktoer_id = a.fk_aktoer_id
         INNER JOIN person_resultat pr ON pr.fk_aktoer_id = pp.fk_aktoer_id
         LEFT JOIN vilkar_resultat vr ON vr.fk_person_resultat_id = pr.id
            AND b.id = vr.fk_behandling_id
            AND vr.periode_fom::date = bb.fom
WHERE UPPER(bb.kommune_navn) = UPPER(:kommuneNavn)
  AND (a.stonad_tom >= current_date)
GROUP BY bb.ident, bb.fom, bb.tom, bb.antall_timer_i_barnehage, bb.endringstype, bb.kommune_navn, bb.kommune_nr, f.id, f.status, vr.antall_timer 
        """,
        nativeQuery = true,
    )
    fun finnBarnehagebarnByKommuneNavnMedLøpendeAndel(
        kommuneNavn: String,
        pageable: Pageable,
    ): Page<BarnehagebarnDtoInterface>

    @Query(
        """
SELECT bb.ident                    as ident,
       bb.fom                      as fom,
       bb.tom                      as tom,
       bb.antall_timer_i_barnehage as antallTimerIBarnehage,
       bb.endringstype             as endringstype,
       bb.kommune_navn             as kommuneNavn,
       bb.kommune_nr               as kommuneNr,
       MAX(bb.endret_tid)          as endretTid,
       f.id                        as fagsakId,
       f.status                    as fagsakstatus,
       bb.antall_timer_i_barnehage != vr.antall_timer AS avvik
FROM barnehagebarn bb
         LEFT OUTER JOIN personident p ON bb.ident = p.foedselsnummer
         LEFT OUTER JOIN po_person pp ON p.fk_aktoer_id = pp.fk_aktoer_id
         LEFT OUTER JOIN gr_personopplysninger go ON pp.fk_gr_personopplysninger_id = go.id
         LEFT OUTER JOIN behandling b ON go.fk_behandling_id = b.id AND b.aktiv = true
         LEFT OUTER JOIN fagsak f ON b.fk_fagsak_id = f.id and f.arkivert = false
         INNER JOIN andel_tilkjent_ytelse a on a.fk_behandling_id = b.id AND pp.fk_aktoer_id = a.fk_aktoer_id
         INNER JOIN person_resultat pr ON pr.fk_aktoer_id = pp.fk_aktoer_id
         LEFT JOIN vilkar_resultat vr ON vr.fk_person_resultat_id = pr.id 
            AND b.id = vr.fk_behandling_id 
            AND vr.periode_fom::date = bb.fom 
            AND vr.periode_fom::date = a.stonad_fom
            AND a.stonad_tom >= current_date
WHERE UPPER(bb.kommune_navn) = UPPER(:kommuneNavn)
GROUP BY bb.ident, bb.fom, bb.tom, bb.antall_timer_i_barnehage, bb.endringstype, bb.kommune_navn, bb.kommune_nr, f.id, f.status, vr.antall_timer""",
        nativeQuery = true,
    )
    fun finnBarnehagebarnByKommuneNavn(
        kommuneNavn: String,
        pageable: Pageable,
    ): Page<BarnehagebarnDtoInterface>

    @Query(
        """
            SELECT DISTINCT bb.kommuneNavn FROM Barnehagebarn bb
        """,
    )
    fun hentAlleKommuner(): Set<String>
}
