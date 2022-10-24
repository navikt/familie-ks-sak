package no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag

import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Medlemskap
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.statsborgerskap.GrStatsborgerskap
import org.springframework.stereotype.Service

@Service
class StatsborgerskapService(val integrasjonClient: IntegrasjonClient) {

    fun hentLand(landkode: String): String = integrasjonClient.hentLand(landkode)

    fun hentStatsborgerskapMedMedlemskap(statsborgerskap: Statsborgerskap, person: Person): List<GrStatsborgerskap> {
        if (statsborgerskap.iNordiskLand()) {
            return listOf(GrStatsborgerskap.fraStatsborgerskap(statsborgerskap, Medlemskap.NORDEN, person))
        }
        // TODO EØS
        return emptyList()
    }

    fun Statsborgerskap.iNordiskLand() = Norden.values().map { it.name }.contains(this.land)

    /**
     * Norge, Sverige, Finland, Danmark, Island, Grønland, Færøyene og Åland
     */
    enum class Norden {
        NOR,
        SWE,
        FIN,
        DNK,
        ISL,
        FRO,
        GRL,
        ALA
    }
}
