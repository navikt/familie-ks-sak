package no.nav.familie.ks.sak.barnehagelister.domene

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import no.nav.familie.ks.sak.api.dto.BarnehagebarnDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class BarnehagebarnMedBehandlingRepository(
    @PersistenceContext
    private val entityManager: EntityManager,
) {

    fun hentData(sql: String, countSql: String, pageable: Pageable): Page<BarnehagebarnDto> {
        val query = entityManager.createNativeQuery(sql, BarnehagebarnDto::class.java)
        query.firstResult = pageable.pageNumber * pageable.pageSize
        query.maxResults = pageable.pageSize
        val list: List<BarnehagebarnDto> = query.resultList as List<BarnehagebarnDto>
        // val count: Long = entityManager.createNativeQuery("SELECT COUNT(*) FROM barnehagebarn").singleResult as Long
        val count: Long = entityManager.createNativeQuery(countSql).singleResult as Long
        return PageImpl<BarnehagebarnDto>(
            list,
            pageable,
            count,
        )
    }
}
