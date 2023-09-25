package no.nav.familie.ks.sak.api.dto

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.time.LocalDate
import java.time.LocalDateTime

data class BarnehagebarnRequestParams(
    val ident: String?,
    val fom: String?,
    val tom: String?,
    val endringstype: String?,
    val kommuneNavn: String?,
    val kommuneNr: String?,
    val antallTimerIBarnehage: Int?,
    val kunLøpendeFagsak: Boolean,
    val limit: Int = 50,
    val offset: Int = 0,
    val sortBy: String = "endret_tid",
    val sortAsc: Boolean = false,
) {
    fun toSql(): String {
        var sql = "SELECT bb.*, b.id as behandling_id, f.id as fagsak_id " + getFromQuery()
        sql += " ORDER BY ${getCorrectSortBy()} ${getAscDesc()}  "
        return sql.trimIndent()
    }

    fun toCountSql(): String {
        return "SELECT COUNT(*) " + getFromQuery().trimIndent()
    }

    private fun getFromQuery(): String {
        var sql = """
            FROM barnehagebarn bb
        INNER JOIN personident p ON bb.ident = p.foedselsnummer AND p.aktiv = true
        INNER JOIN po_person pp ON p.fk_aktoer_id = pp.fk_aktoer_id
                INNER JOIN gr_personopplysninger go ON pp.fk_gr_personopplysninger_id = go.id
                INNER JOIN behandling b ON go.fk_behandling_id = b.id AND b.aktiv = true
        INNER JOIN fagsak f ON b.fk_fagsak_id = f.id AND f.arkivert = false WHERE 1 = 1
        """.trimIndent()
        if (!ident.isNullOrEmpty()) {
            sql += " AND ident = '$ident'"
        }
        if (!fom.isNullOrEmpty()) {
            sql += " AND fom = '$fom'"
        }
        if (!tom.isNullOrEmpty()) {
            sql += " AND tom = '$tom'"
        }
        if (!endringstype.isNullOrEmpty()) {
            sql += " AND endringstype = '$endringstype'"
        }
        if (!kommuneNavn.isNullOrEmpty()) {
            sql += " AND kommune_navn = '$kommuneNavn'"
        }
        if (!kommuneNr.isNullOrEmpty()) {
            sql += " AND kommune_nr = '$kommuneNr'"
        }
        if (antallTimerIBarnehage != null) {
            sql += " AND antall_timer_i_barnehage = '$antallTimerIBarnehage'"
        }
        if (kunLøpendeFagsak) {
            sql += " AND f.status = 'LØPENDE'"
        }
        return sql.trimIndent()
    }

    fun getCorrectSortBy(): String {
        return when (sortBy.lowercase()) {
            "endrettidspunkt" -> "endret_tid"
            "kommunenavn" -> "kommune_navn"
            "kommunenr" -> "kommune_nr"
            "antalltimeribarnehage" -> "antall_timer_i_barnehage"
            else -> sortBy
        }
    }
    fun getAscDesc(): String {
        return if (sortAsc) "ASC" else "DESC"
    }
}

@Entity
data class BarnehagebarnDto(
    @Id
    val id: String,
    val ident: String,
    val fom: LocalDate,
    val tom: LocalDate? = null,
    @Column(name = "antall_timer_i_barnehage")
    val antallTimerIBarnehage: Double,
    val endringstype: String,
    // @Column(name = "kummune_navn")
    val kommuneNavn: String,
    val kommuneNr: String,
    val opprettetAv: String,
    @Column(name = "opprettet_tid")
    val opprettetTidspunkt: LocalDateTime,
    val endretAv: String,
    @Column(name = "endret_tid")
    val endretTidspunkt: LocalDateTime,
    val versjon: Long,
    val behandlingId: Long,
    val fagsakId: Long,
)
