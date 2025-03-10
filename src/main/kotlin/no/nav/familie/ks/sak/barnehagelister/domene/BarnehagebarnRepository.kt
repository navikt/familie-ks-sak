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
            SELECT bb.ident as ident, bb.fom as fom, bb.tom as tom, bb.antall_timer_i_barnehage as antallTimerIBarnehage, 
            bb.endringstype as endringstype, bb.kommune_navn as kommuneNavn, bb.kommune_nr as kommuneNr, MAX(bb.endret_tid) as endretTid,
            f.id as fagsakId, f.status as fagsakstatus
            FROM barnehagebarn bb
            INNER JOIN personident p ON bb.ident = p.foedselsnummer AND p.aktiv = true
            INNER JOIN po_person pp ON p.fk_aktoer_id = pp.fk_aktoer_id
            INNER JOIN gr_personopplysninger go ON pp.fk_gr_personopplysninger_id = go.id
            INNER JOIN behandling b ON go.fk_behandling_id = b.id AND b.aktiv = true
            INNER JOIN fagsak f ON b.fk_fagsak_id = f.id AND f.arkivert = false
            AND f.status IN (:fagsakStatuser)
            GROUP BY bb.ident, bb.fom, bb.tom, bb.antall_timer_i_barnehage, bb.endringstype, bb.kommune_navn, bb.kommune_nr, f.id, f.status""",
        nativeQuery = true,
    )
    fun findBarnehagebarn(
        fagsakStatuser: List<String>,
        pageable: Pageable,
    ): Page<BarnehagebarnDtoInterface>

    @Query(
        """
            SELECT bb.ident as ident, bb.fom as fom, bb.tom as tom, bb.antall_timer_i_barnehage as antallTimerIBarnehage, 
            bb.endringstype as endringstype, bb.kommune_navn as kommuneNavn, bb.kommune_nr as kommuneNr, MAX(bb.endret_tid) as endretTid,
            f.id as fagsakId, f.status as fagsakstatus
            FROM barnehagebarn bb
            INNER JOIN personident p ON bb.ident = p.foedselsnummer AND p.aktiv = true
            INNER JOIN po_person pp ON p.fk_aktoer_id = pp.fk_aktoer_id
            INNER JOIN gr_personopplysninger go ON pp.fk_gr_personopplysninger_id = go.id
            INNER JOIN behandling b ON go.fk_behandling_id = b.id AND b.aktiv = true
            INNER JOIN fagsak f ON b.fk_fagsak_id = f.id AND f.arkivert = false WHERE bb.ident = :ident
            AND f.status IN (:fagsakStatuser)
            GROUP BY bb.ident, bb.fom, bb.tom, bb.antall_timer_i_barnehage, bb.endringstype, bb.kommune_navn, bb.kommune_nr, f.id, f.status""",
        nativeQuery = true,
    )
    fun findBarnehagebarnByIdent(
        fagsakStatuser: List<String>,
        ident: String,
        pageable: Pageable,
    ): Page<BarnehagebarnDtoInterface>

    @Query(
        """
            SELECT bb.ident as ident, bb.fom as fom, bb.tom as tom, bb.antall_timer_i_barnehage as antallTimerIBarnehage, 
            bb.endringstype as endringstype, bb.kommune_navn as kommuneNavn, bb.kommune_nr as kommuneNr, MAX(bb.endret_tid) as endretTid,
            f.id as fagsakId, f.status as fagsakstatus
            FROM barnehagebarn bb
            INNER JOIN personident p ON bb.ident = p.foedselsnummer AND p.aktiv = true
            INNER JOIN po_person pp ON p.fk_aktoer_id = pp.fk_aktoer_id
            INNER JOIN gr_personopplysninger go ON pp.fk_gr_personopplysninger_id = go.id
            INNER JOIN behandling b ON go.fk_behandling_id = b.id AND b.aktiv = true
            INNER JOIN fagsak f ON b.fk_fagsak_id = f.id AND f.arkivert = false WHERE UPPER(bb.kommune_navn) = UPPER(:kommuneNavn)
            AND f.status IN (:fagsakStatuser)
            GROUP BY bb.ident, bb.fom, bb.tom, bb.antall_timer_i_barnehage, bb.endringstype, bb.kommune_navn, bb.kommune_nr, f.id, f.status""",
        nativeQuery = true,
    )
    fun findBarnehagebarnByKommuneNavn(
        fagsakStatuser: List<String>,
        kommuneNavn: String,
        pageable: Pageable,
    ): Page<BarnehagebarnDtoInterface>

    @Query(
        """
            SELECT bb.ident as ident, bb.fom as fom, bb.tom as tom, bb.antall_timer_i_barnehage as antallTimerIBarnehage,
            bb.endringstype as endringstype, bb.kommune_navn as kommuneNavn, bb.kommune_nr as kommuneNr, MAX(bb.endret_tid) as endretTid,
            f.id as fagsakId, f.status as fagsakstatus
            FROM barnehagebarn bb
            LEFT OUTER JOIN personident p ON bb.ident = p.foedselsnummer
            LEFT OUTER JOIN po_person pp ON p.fk_aktoer_id = pp.fk_aktoer_id
            LEFT OUTER JOIN gr_personopplysninger go ON pp.fk_gr_personopplysninger_id = go.id
            LEFT OUTER JOIN  behandling b ON go.fk_behandling_id = b.id AND b.aktiv = true
            LEFT OUTER JOIN fagsak f ON b.fk_fagsak_id = f.id and f.arkivert = false
            GROUP BY bb.ident, bb.fom, bb.tom, bb.antall_timer_i_barnehage, bb.endringstype, bb.kommune_navn, bb.kommune_nr, f.id, f.status""",
        nativeQuery = true,
    )
    fun findAlleBarnehagebarnUavhengigAvFagsak(pageable: Pageable): Page<BarnehagebarnDtoInterface>

    @Query(
        """
            SELECT bb.ident as ident, bb.fom as fom, bb.tom as tom, bb.antall_timer_i_barnehage as antallTimerIBarnehage,
            bb.endringstype as endringstype, bb.kommune_navn as kommuneNavn, bb.kommune_nr as kommuneNr, MAX(bb.endret_tid) as endretTid,
            f.id as fagsakId, f.status as fagsakstatus
            FROM barnehagebarn bb
            LEFT OUTER JOIN personident p ON bb.ident = p.foedselsnummer
            LEFT OUTER JOIN po_person pp ON p.fk_aktoer_id = pp.fk_aktoer_id
            LEFT OUTER JOIN gr_personopplysninger go ON pp.fk_gr_personopplysninger_id = go.id
            LEFT OUTER JOIN  behandling b ON go.fk_behandling_id = b.id AND b.aktiv = true
            LEFT OUTER JOIN fagsak f ON b.fk_fagsak_id = f.id and f.arkivert = false 
            WHERE bb.ident = :ident
            GROUP BY ident, fom, tom, antallTimerIBarnehage, endringstype, kommuneNavn, kommuneNr, fagsakId, fagsakstatus""",
        nativeQuery = true,
    )
    fun findBarnehagebarnByIdentUavhengigAvFagsak(
        ident: String,
        pageable: Pageable,
    ): Page<BarnehagebarnDtoInterface>

    @Query(
        """
            SELECT bb.ident as ident, bb.fom as fom, bb.tom as tom, bb.antall_timer_i_barnehage as antallTimerIBarnehage,
            bb.endringstype as endringstype, bb.kommune_navn as kommuneNavn, bb.kommune_nr as kommuneNr, MAX(bb.endret_tid) as endretTid,
            f.id as fagsakId, f.status as fagsakstatus
            FROM barnehagebarn bb
            LEFT OUTER JOIN personident p ON bb.ident = p.foedselsnummer
            LEFT OUTER JOIN po_person pp ON p.fk_aktoer_id = pp.fk_aktoer_id
            LEFT OUTER JOIN gr_personopplysninger go ON pp.fk_gr_personopplysninger_id = go.id
            LEFT OUTER JOIN  behandling b ON go.fk_behandling_id = b.id AND b.aktiv = true
            LEFT OUTER JOIN fagsak f ON b.fk_fagsak_id = f.id and f.arkivert = false 
            WHERE UPPER(bb.kommune_navn) = UPPER(:kommuneNavn)
            GROUP BY bb.ident, bb.fom, bb.tom, bb.antall_timer_i_barnehage, bb.endringstype, bb.kommune_navn, bb.kommune_nr, f.id, f.status""",
        nativeQuery = true,
    )
    fun findBarnehagebarnByKommuneNavnUavhengigAvFagsak(
        kommuneNavn: String,
        pageable: Pageable,
    ): Page<BarnehagebarnDtoInterface>

    @Query(
        """
            SELECT bb.ident as ident, bb.fom as fom, bb.tom as tom, bb.antall_timer_i_barnehage as antallTimerIBarnehage, 
            bb.endringstype as endringstype, bb.kommune_navn as kommuneNavn, bb.kommune_nr as kommuneNr, MAX(bb.endret_tid) as endretTid
            FROM barnehagebarn bb
            WHERE bb.ident in (:barna) AND UPPER(bb.kommune_navn) = UPPER(:kommuneNavn)
            GROUP BY bb.ident, bb.fom, bb.tom, bb.antall_timer_i_barnehage, bb.endringstype, bb.kommune_navn, bb.kommune_nr""",
        nativeQuery = true,
    )
    fun findBarnehagebarnByKommuneNavnInfotrygd(
        kommuneNavn: String,
        barna: Set<String>,
        pageable: Pageable,
    ): Page<BarnehagebarnInfotrygdDtoInterface>

    @Query(
        """
            SELECT bb.ident as ident, bb.fom as fom, bb.tom as tom, bb.antall_timer_i_barnehage as antallTimerIBarnehage, 
            bb.endringstype as endringstype, bb.kommune_navn as kommuneNavn, bb.kommune_nr as kommuneNr, MAX(bb.endret_tid) as endretTid
            FROM barnehagebarn bb
            WHERE UPPER(bb.kommune_navn) = UPPER(:kommuneNavn)
            GROUP BY bb.ident, bb.fom, bb.tom, bb.antall_timer_i_barnehage, bb.endringstype, bb.kommune_navn, bb.kommune_nr""",
        nativeQuery = true,
    )
    fun findBarnehagebarnByKommuneNavnInfotrygdUavhengigAvFagsak(
        kommuneNavn: String,
        pageable: Pageable,
    ): Page<BarnehagebarnInfotrygdDtoInterface>

    @Query(
        """
            SELECT bb.ident as ident, bb.fom as fom, bb.tom as tom, bb.antall_timer_i_barnehage as antallTimerIBarnehage, 
            bb.endringstype as endringstype, bb.kommune_navn as kommuneNavn, bb.kommune_nr as kommuneNr, MAX(bb.endret_tid) as endretTid
            FROM barnehagebarn bb
            WHERE bb.ident in (:barna) AND bb.ident = :ident
            GROUP BY bb.ident, bb.fom, bb.tom, bb.antall_timer_i_barnehage, bb.endringstype, bb.kommune_navn, bb.kommune_nr""",
        nativeQuery = true,
    )
    fun findBarnehagebarnByIdentInfotrygd(
        ident: String,
        barna: Set<String>,
        pageable: Pageable,
    ): Page<BarnehagebarnInfotrygdDtoInterface>

    @Query(
        """
            SELECT bb.ident as ident, bb.fom as fom, bb.tom as tom, bb.antall_timer_i_barnehage as antallTimerIBarnehage, 
            bb.endringstype as endringstype, bb.kommune_navn as kommuneNavn, bb.kommune_nr as kommuneNr, MAX(bb.endret_tid) as endretTid
            FROM barnehagebarn bb
            WHERE bb.ident = :ident
            GROUP BY bb.ident, bb.fom, bb.tom, bb.antall_timer_i_barnehage, bb.endringstype, bb.kommune_navn, bb.kommune_nr""",
        nativeQuery = true,
    )
    fun findBarnehagebarnByIdentInfotrygdUavhengigAvFagsak(
        ident: String,
        pageable: Pageable,
    ): Page<BarnehagebarnInfotrygdDtoInterface>

    @Query(
        """
            SELECT bb.ident as ident, bb.fom as fom, bb.tom as tom, bb.antall_timer_i_barnehage as antallTimerIBarnehage, 
            bb.endringstype as endringstype, bb.kommune_navn as kommuneNavn, bb.kommune_nr as kommuneNr, MAX(bb.endret_tid) as endretTid
            FROM barnehagebarn bb
            WHERE bb.ident in (:barna)
            GROUP BY bb.ident, bb.fom, bb.tom, bb.antall_timer_i_barnehage, bb.endringstype, bb.kommune_navn, bb.kommune_nr""",
        nativeQuery = true,
    )
    fun findBarnehagebarnInfotrygd(
        barna: Set<String>,
        pageable: Pageable,
    ): Page<BarnehagebarnInfotrygdDtoInterface>

    @Query(
        """
            SELECT bb.ident as ident, bb.fom as fom, bb.tom as tom, bb.antall_timer_i_barnehage as antallTimerIBarnehage, 
            bb.endringstype as endringstype, bb.kommune_navn as kommuneNavn, bb.kommune_nr as kommuneNr, MAX(bb.endret_tid) as endretTid
            FROM barnehagebarn bb.barnehagebarn
            GROUP BY bb.ident, bb.fom, bb.tom, bb.antall_timer_i_barnehage, bb.endringstype, bb.kommune_navn, bb.kommune_nr """,
        nativeQuery = true,
    )
    fun findBarnehagebarnInfotrygdUavhengigAvFagsak(pageable: Pageable): Page<BarnehagebarnInfotrygdDtoInterface>

    @Query(
        """
            SELECT DISTINCT bb.kommuneNavn FROM Barnehagebarn bb
        """,
    )
    fun hentAlleKommuner(): Set<String>
}
