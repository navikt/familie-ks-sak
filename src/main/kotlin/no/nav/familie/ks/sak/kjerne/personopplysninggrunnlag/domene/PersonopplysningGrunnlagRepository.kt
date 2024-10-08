package no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene

import org.springframework.data.jpa.repository.JpaRepository
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
}
