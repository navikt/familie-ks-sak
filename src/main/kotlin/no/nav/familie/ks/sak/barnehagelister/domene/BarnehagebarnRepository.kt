package no.nav.familie.ks.sak.barnehagelister.domene

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.*

interface BarnehagebarnRepository : JpaRepository<Barnehagebarn, UUID> { // , JpaSpecificationExecutor<Barnehagebarn>
    fun findByIdent(ident: String): Barnehagebarn

    @Query(
        """
            SELECT DISTINCT bb.ident as ident, bb.fom as fom, bb.tom as tom, bb.antall_timer_i_barnehage as antallTimerIBarnehage, 
            bb.endringstype as endringstype, bb.kommune_navn as kommuneNavn, bb.kommune_nr as kommuneNr,
            f.id as fagsakId, f.status as fagsakstatus
            FROM barnehagebarn bb
            INNER JOIN personident p ON bb.ident = p.foedselsnummer AND p.aktiv = true
            INNER JOIN po_person pp ON p.fk_aktoer_id = pp.fk_aktoer_id
            INNER JOIN gr_personopplysninger go ON pp.fk_gr_personopplysninger_id = go.id
            INNER JOIN behandling b ON go.fk_behandling_id = b.id AND b.aktiv = true
            INNER JOIN fagsak f ON b.fk_fagsak_id = f.id AND f.arkivert = false
            AND f.status IN (:fagsakStatuser)""",
        nativeQuery = true,
    )
    fun findBarnehagebarn(fagsakStatuser: List<String>, pageable: Pageable): Page<BarnehagebarnDtoInterface>

    @Query(
        """
            SELECT DISTINCT bb.ident as ident, bb.fom as fom, bb.tom as tom, bb.antall_timer_i_barnehage as antallTimerIBarnehage, 
            bb.endringstype as endringstype, bb.kommune_navn as kommuneNavn, bb.kommune_nr as kommuneNr,
            f.id as fagsakId, f.status as fagsakstatus
            FROM barnehagebarn bb
            INNER JOIN personident p ON bb.ident = p.foedselsnummer AND p.aktiv = true
            INNER JOIN po_person pp ON p.fk_aktoer_id = pp.fk_aktoer_id
            INNER JOIN gr_personopplysninger go ON pp.fk_gr_personopplysninger_id = go.id
            INNER JOIN behandling b ON go.fk_behandling_id = b.id AND b.aktiv = true
            INNER JOIN fagsak f ON b.fk_fagsak_id = f.id AND f.arkivert = false WHERE bb.ident = :ident
            AND f.status IN (:fagsakStatuser)
            """,
        nativeQuery = true,
    )
    fun findBarnehagebarnByIdent(
        fagsakStatuser: List<String>,
        ident: String,
        pageable: Pageable,
    ): Page<BarnehagebarnDtoInterface>

    @Query(
        """
            SELECT DISTINCT bb.ident as ident, bb.fom as fom, bb.tom as tom, bb.antall_timer_i_barnehage as antallTimerIBarnehage, 
            bb.endringstype as endringstype, bb.kommune_navn as kommuneNavn, bb.kommune_nr as kommuneNr,
            f.id as fagsakId, f.status as fagsakstatus
            FROM barnehagebarn bb
            INNER JOIN personident p ON bb.ident = p.foedselsnummer AND p.aktiv = true
            INNER JOIN po_person pp ON p.fk_aktoer_id = pp.fk_aktoer_id
            INNER JOIN gr_personopplysninger go ON pp.fk_gr_personopplysninger_id = go.id
            INNER JOIN behandling b ON go.fk_behandling_id = b.id AND b.aktiv = true
            INNER JOIN fagsak f ON b.fk_fagsak_id = f.id AND f.arkivert = false WHERE bb.kommune_navn = :kommuneNavn
            AND f.status IN (:fagsakStatuser)""",
        nativeQuery = true,
    )
    fun findBarnehagebarnByKommuneNavn(
        fagsakStatuser: List<String>,
        kommuneNavn: String,
        pageable: Pageable,
    ): Page<BarnehagebarnDtoInterface>

    @Query(
        """
           SELECT distinct bb.ident as ident, bb.fom as fom, bb.tom as tom, bb.antall_timer_i_barnehage as antallTimerIBarnehage,
                bb.endringstype as endringstype, bb.kommune_navn as kommuneNavn, bb.kommune_nr as kommuneNr,
                f.id as fagsakId, f.status as fagsakstatus
            FROM barnehagebarn bb
         left outer JOIN personident p ON bb.ident = p.foedselsnummer
         left outer JOIN po_person pp ON p.fk_aktoer_id = pp.fk_aktoer_id
         left outer JOIN gr_personopplysninger go ON pp.fk_gr_personopplysninger_id = go.id
         LEFT outer join  behandling b ON go.fk_behandling_id = b.id AND b.aktiv = true
         Left outer join fagsak f ON b.fk_fagsak_id = f.id and f.arkivert = false""",
        nativeQuery = true,
    )
    fun findAlleBarnehagebarnUavhengigAvFagsak(
        pageable: Pageable,
    ): Page<BarnehagebarnDtoInterface>

    @Query(
        """
            SELECT distinct bb.ident as ident, bb.fom as fom, bb.tom as tom, bb.antall_timer_i_barnehage as antallTimerIBarnehage,
                bb.endringstype as endringstype, bb.kommune_navn as kommuneNavn, bb.kommune_nr as kommuneNr,
                f.id as fagsakId, f.status as fagsakstatus
            FROM barnehagebarn bb
         left outer JOIN personident p ON bb.ident = p.foedselsnummer
         left outer JOIN po_person pp ON p.fk_aktoer_id = pp.fk_aktoer_id
         left outer JOIN gr_personopplysninger go ON pp.fk_gr_personopplysninger_id = go.id
         LEFT outer join  behandling b ON go.fk_behandling_id = b.id AND b.aktiv = true
         Left outer join fagsak f ON b.fk_fagsak_id = f.id and f.arkivert = false 
         WHERE bb.ident = :ident
            """,
        nativeQuery = true,
    )
    fun findBarnehagebarnByIdentUavhengigAvFagsak(
        ident: String,
        pageable: Pageable,
    ): Page<BarnehagebarnDtoInterface>

    @Query(
        """
            SELECT distinct bb.ident as ident, bb.fom as fom, bb.tom as tom, bb.antall_timer_i_barnehage as antallTimerIBarnehage,
                bb.endringstype as endringstype, bb.kommune_navn as kommuneNavn, bb.kommune_nr as kommuneNr,
                f.id as fagsakId, f.status as fagsakstatus
        FROM barnehagebarn bb
         left outer JOIN personident p ON bb.ident = p.foedselsnummer
         left outer JOIN po_person pp ON p.fk_aktoer_id = pp.fk_aktoer_id
         left outer JOIN gr_personopplysninger go ON pp.fk_gr_personopplysninger_id = go.id
         LEFT outer join  behandling b ON go.fk_behandling_id = b.id AND b.aktiv = true
         Left outer join fagsak f ON b.fk_fagsak_id = f.id and f.arkivert = false 
         WHERE bb.kommune_navn = :kommuneNavn""",
        nativeQuery = true,
    )
    fun findBarnehagebarnByKommuneNavnUavhengigAvFagsak(
        kommuneNavn: String,
        pageable: Pageable,
    ): Page<BarnehagebarnDtoInterface>

    @Query(
        """
            SELECT DISTINCT bb.ident as ident, bb.fom as fom, bb.tom as tom, bb.antall_timer_i_barnehage as antallTimerIBarnehage, 
            bb.endringstype as endringstype, bb.kommune_navn as kommuneNavn, bb.kommune_nr as kommuneNr,
            FROM barnehagebarn bb
            where bb.ident in (:barna) AND bb.kommune_navn = :kommuneNavn""",
        nativeQuery = true,
    )
    fun findBarnehagebarnByKommuneNavnInfotrygd(
        kommuneNavn: String,
        barna: List<String>,
        pageable: Pageable,
    ): Page<BarnehagebarnInfotrygdDto>

    @Query(
        """
            SELECT DISTINCT bb.ident as ident, bb.fom as fom, bb.tom as tom, bb.antall_timer_i_barnehage as antallTimerIBarnehage, 
            bb.endringstype as endringstype, bb.kommune_navn as kommuneNavn, bb.kommune_nr as kommuneNr,
            FROM barnehagebarn bb
            WHERE bb.kommune_navn = :kommuneNavn""",
        nativeQuery = true,
    )
    fun findBarnehagebarnByKommuneNavnInfotrygdUavhengigAvFagsak(
        kommuneNavn: String,
        pageable: Pageable,
    ): Page<BarnehagebarnInfotrygdDto>

    @Query(
        """
            SELECT DISTINCT bb.ident as ident, bb.fom as fom, bb.tom as tom, bb.antall_timer_i_barnehage as antallTimerIBarnehage, 
            bb.endringstype as endringstype, bb.kommune_navn as kommuneNavn, bb.kommune_nr as kommuneNr,
            FROM barnehagebarn bb
            where bb.ident in (:barna) AND bb.ident = :ident""",
        nativeQuery = true,
    )
    fun findBarnehagebarnByIdentInfotrygd(
        ident: String,
        barna: List<String>,
        pageable: Pageable,
    ): Page<BarnehagebarnInfotrygdDto>

    @Query(
        """
            SELECT DISTINCT bb.ident as ident, bb.fom as fom, bb.tom as tom, bb.antall_timer_i_barnehage as antallTimerIBarnehage, 
            bb.endringstype as endringstype, bb.kommune_navn as kommuneNavn, bb.kommune_nr as kommuneNr,
            FROM barnehagebarn bb
            WHERE bb.kommune_navn = :ident""",
        nativeQuery = true,
    )
    fun findBarnehagebarnByIdentInfotrygdUavhengigAvFagsak(
        ident: String,
        pageable: Pageable,
    ): Page<BarnehagebarnInfotrygdDto>

    @Query(
        """
            SELECT DISTINCT bb.ident as ident, bb.fom as fom, bb.tom as tom, bb.antall_timer_i_barnehage as antallTimerIBarnehage, 
            bb.endringstype as endringstype, bb.kommune_navn as kommuneNavn, bb.kommune_nr as kommuneNr,
            FROM barnehagebarn bb
            where bb.ident in (:barna)""",
        nativeQuery = true,
    )
    fun findBarnehagebarnInfotrygd(
        barna: List<String>,
        pageable: Pageable,
    ): Page<BarnehagebarnInfotrygdDto>

    @Query(
        """
            SELECT DISTINCT bb.ident as ident, bb.fom as fom, bb.tom as tom, bb.antall_timer_i_barnehage as antallTimerIBarnehage, 
            bb.endringstype as endringstype, bb.kommune_navn as kommuneNavn, bb.kommune_nr as kommuneNr,
            FROM barnehagebarn bb""",
        nativeQuery = true,
    )
    fun findBarnehagebarnInfotrygdUavhengigAvFagsak(
        pageable: Pageable,
    ): Page<BarnehagebarnInfotrygdDto>
}
