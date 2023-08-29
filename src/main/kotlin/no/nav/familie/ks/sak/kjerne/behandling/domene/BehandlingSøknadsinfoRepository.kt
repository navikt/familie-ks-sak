package no.nav.familie.ks.sak.kjerne.behandling.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface BehandlingSøknadsinfoRepository : JpaRepository<BehandlingSøknadsinfo, Long> {

    @Query(
        """
            SELECT COUNT(*) AS totalt,
                   SUM(CAST(er_digital AS INTEGER)) AS digitalt,
                   SUM(CAST(NOT er_digital AS INTEGER)) AS papir
            FROM (SELECT DISTINCT journalpost_id, er_digital
                  FROM behandling_soknadsinfo
                  WHERE mottatt_dato >= :fomDato
                  AND mottatt_dato <= :tomDato
                  ) AS bs
    """,
        nativeQuery = true,
    )
    fun hentAntallSøknaderIPeriode(fomDato: LocalDateTime, tomDato: LocalDateTime): AntallSøknader
}

interface AntallSøknader {

    val totalt: Int
    val digitalt: Int?
    val papir: Int?
}
