package no.nav.familie.ks.sak.kjerne.avstemming.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface KonsistensavstemmingKjøreplanRepository : JpaRepository<KonsistensavstemmingKjøreplan, Long> {
    @Query("SELECT k FROM KonsistensavstemmingKjoreplan k where k.kjøredato = :dato AND k.status = 'LEDIG'")
    fun findByKjøredatoAndLedig(dato: LocalDate): KonsistensavstemmingKjøreplan?
}
