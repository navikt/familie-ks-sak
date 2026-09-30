package no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface PersonopplysningGrunnlagRepository : JpaRepository<PersonopplysningGrunnlag, Long> {
    @Query("SELECT gr FROM PersonopplysningGrunnlag gr WHERE gr.behandlingId = :behandlingId AND gr.aktiv = true")
    fun findByBehandlingAndAktiv(behandlingId: Long): PersonopplysningGrunnlag?

    @Query("SELECT gr FROM PersonopplysningGrunnlag gr WHERE gr.behandlingId = :behandlingId AND gr.aktiv = true")
    fun hentByBehandlingAndAktiv(behandlingId: Long): PersonopplysningGrunnlag

    @Query(
        """
        SELECT new no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonEnkel(p.type, a, p.fødselsdato, d.dødsfallDato, p.målform)
        FROM Person p
        JOIN p.personopplysningGrunnlag gr
        JOIN p.aktør a
        JOIN Behandling b ON b.id = gr.behandlingId
        LEFT JOIN p.dødsfall d
        WHERE b.fagsak.id = :fagsakId 
        AND gr.aktiv = true
        AND p.type IN ('SØKER', 'BARN')
        """,
    )
    fun finnSøkerOgBarnAktørerTilFagsak(fagsakId: Long): Set<PersonEnkel>

    @Query(
        """
        SELECT gr.id FROM PersonopplysningGrunnlag gr
        WHERE gr.aktiv = false
        AND gr.id > :etterId
        AND EXISTS (
            SELECT 1 FROM PersonopplysningGrunnlag aktivtGrunnlag
            WHERE aktivtGrunnlag.behandlingId = gr.behandlingId
            AND aktivtGrunnlag.aktiv = true
        )
        ORDER BY gr.id
        """,
    )
    fun finnIderForInaktiveGrunnlagMedAktivtGrunnlagPåSammeBehandling(
        etterId: Long,
        pageable: Pageable,
    ): List<Long>

    @Query(
        """
        SELECT count(gr) FROM PersonopplysningGrunnlag gr
        WHERE gr.aktiv = false
        AND NOT EXISTS (
            SELECT 1 FROM PersonopplysningGrunnlag aktivtGrunnlag
            WHERE aktivtGrunnlag.behandlingId = gr.behandlingId
            AND aktivtGrunnlag.aktiv = true
        )
        """,
    )
    fun tellInaktiveGrunnlagUtenAktivtGrunnlagPåSammeBehandling(): Long

    @Modifying
    @Query("DELETE FROM PersonopplysningGrunnlag gr WHERE gr.id IN :grunnlagIder")
    fun slettPersonopplysningsgrunnlag(grunnlagIder: List<Long>): Int

    @Query(
        value = """
        WITH person AS (SELECT id FROM po_person WHERE fk_gr_personopplysninger_id IN (:grunnlagIder))
        SELECT 'po_person' AS tabell, count(*) AS antall FROM person
        UNION ALL SELECT 'po_statsborgerskap', count(*) FROM po_statsborgerskap WHERE fk_po_person_id IN (SELECT id FROM person)
        UNION ALL SELECT 'po_opphold', count(*) FROM po_opphold WHERE fk_po_person_id IN (SELECT id FROM person)
        UNION ALL SELECT 'po_arbeidsforhold', count(*) FROM po_arbeidsforhold WHERE fk_po_person_id IN (SELECT id FROM person)
        UNION ALL SELECT 'po_sivilstand', count(*) FROM po_sivilstand WHERE fk_po_person_id IN (SELECT id FROM person)
        UNION ALL SELECT 'po_bostedsadresse', count(*) FROM po_bostedsadresse WHERE fk_po_person_id IN (SELECT id FROM person)
        UNION ALL SELECT 'po_doedsfall', count(*) FROM po_doedsfall WHERE fk_po_person_id IN (SELECT id FROM person)
        UNION ALL SELECT 'po_oppholdsadresse', count(*) FROM po_oppholdsadresse WHERE fk_po_person_id IN (SELECT id FROM person)
        """,
        nativeQuery = true,
    )
    fun tellRaderSomSlettesMedGrunnlag(grunnlagIder: List<Long>): List<AntallRaderITabell>
}

interface AntallRaderITabell {
    val tabell: String
    val antall: Long
}
