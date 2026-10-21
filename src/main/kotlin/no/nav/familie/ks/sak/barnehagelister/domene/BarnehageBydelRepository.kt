package no.nav.familie.ks.sak.barnehagelister.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional

interface BarnehageBydelRepository : JpaRepository<BarnehageBydel, Long> {
    fun findByBarnehageOrgNummer(barnehageOrgNummer: String): BarnehageBydel?

    /**
     * Lagrer bydelen til barnehagen. Finnes barnehagen fra før, gjøres ingenting.
     */
    @Transactional
    @Modifying
    @Query(
        """
        INSERT INTO barnehage_bydel (id, barnehage_org_nummer, bydel_org_nummer, endret_av, endret_tid)
        VALUES (nextval('barnehage_bydel_seq'), :barnehageOrgNummer, :bydelOrgNummer, 'VL', localtimestamp)
        ON CONFLICT (barnehage_org_nummer) DO NOTHING
        """,
        nativeQuery = true,
    )
    fun lagreHvisIkkeFinnes(
        barnehageOrgNummer: String,
        bydelOrgNummer: String,
    )
}
