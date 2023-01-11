package no.nav.familie.ks.sak.kjerne.eøs.felles.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.NoRepositoryBean

@NoRepositoryBean
interface EøsSkjemaRepository<T : EøsSkjemaEntitet<T>> : JpaRepository<T, Long> {
    fun findByBehandlingId(behandlingId: Long): List<T>
}
