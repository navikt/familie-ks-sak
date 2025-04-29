package no.nav.familie.ks.sak.barnehagelister.domene

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface BarnehagebarnRepository : JpaRepository<Barnehagebarn, UUID> { // , JpaSpecificationExecutor<Barnehagebarn>
    fun findAllByIdent(ident: String): MutableList<Barnehagebarn>

    @Query(
        """WITH lopende_andel_i_aktiv_behandling_for_ident AS (SELECT p.foedselsnummer, aty.stonad_tom >= current_date as lopende_andel
                                                                 FROM andel_tilkjent_ytelse aty
                                                                          JOIN behandling b ON aty.fk_behandling_id = b.id
                                                                          JOIN personident p ON aty.fk_aktoer_id = p.fk_aktoer_id
                                                                 WHERE p.aktiv
                                                                   AND b.aktiv),
     avvik_antall_timer_vilkar_resultat_og_barnehagebarn AS (SELECT bb.id                                          as barnehagebarn_id,
                                                                    vr.antall_timer != bb.antall_timer_i_barnehage as avvik
                                                              FROM barnehagebarn bb
                                                                       JOIN personident p ON bb.ident = p.foedselsnummer and p.aktiv
                                                                       JOIN person_resultat pr ON pr.fk_aktoer_id = p.fk_aktoer_id
                                                                       JOIN vilkar_resultat vr
                                                                            ON pr.id = vr.fk_person_resultat_id
                                                                                AND vr.vilkar = 'BARNEHAGEPLASS'
                                                                                AND bb.fom = vr.periode_fom::date)
SELECT bb.ident,
       bb.fom,
       bb.tom,
       bb.antall_timer_i_barnehage as antallTimerBarnehage,
       bb.endringstype             as endringstype,
       bb.kommune_navn             as kommuneNavn,
       bb.kommune_nr               as kommuneNr,
       MAX(bb.endret_tid)          as endretTid,
       atv.avvik
FROM Barnehagebarn bb
         LEFT JOIN lopende_andel_i_aktiv_behandling_for_ident ila ON bb.ident = ila.foedselsnummer
         LEFT JOIN avvik_antall_timer_vilkar_resultat_og_barnehagebarn atv on bb.id = atv.barnehagebarn_id
WHERE (NOT :kunLøpendeAndeler OR ila.lopende_andel = true)
    AND COALESCE(bb.ident = :ident, true)
    AND COALESCE(bb.kommune_navn ILIKE :kommuneNavn, true)
GROUP BY bb.ident, bb.fom, bb.tom, bb.antall_timer_i_barnehage, bb.endringstype, bb.kommune_navn, bb.kommune_nr,
         atv.avvik;
""",
        nativeQuery = true,
    )
    fun finnBarnehagebarn(
        kunLøpendeAndeler: Boolean,
        ident: String?,
        kommuneNavn: String?,
        pageable: Pageable,
    ): Page<BarnehagebarnForListe>

    @Query(
        """
            SELECT DISTINCT bb.kommuneNavn FROM Barnehagebarn bb
        """,
    )
    fun hentAlleKommuner(): Set<String>
}
