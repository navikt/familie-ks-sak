package no.nav.familie.ks.sak.kjerne.eøs.felles

import no.nav.familie.ks.sak.common.tidslinje.utvidelser.slåSammen
import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.EøsSkjemaEntitet
import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.EøsSkjemaRepository
import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.medBehandlingId
import no.nav.familie.ks.sak.kjerne.eøs.felles.endringsabonnent.EøsSkjemaEndringAbonnent

// Denne klassen inneholder felles logikk for EØS skjema operasajoner(sletting, oppdatering, henting)
// skjema betyr det som SB har lagt til ifbm EØS i behandlingsresultat side
// Her T kan være Kompetanse, Utenlandskbeløp eller Valutakurs
class EøsSkjemaService<T : EøsSkjemaEntitet<T>>(
    private val skjemaRepository: EøsSkjemaRepository<T>,
    val endringsabonnenter: List<EøsSkjemaEndringAbonnent<T>>
) {

    fun hentMedId(id: Long): T = skjemaRepository.getReferenceById(id)

    fun hentMedBehandlingId(behandlingId: Long) = skjemaRepository.findByBehandlingId(behandlingId)

    fun endreSkjemaer(behandlingId: Long, oppdatering: T) {
        val skjemaer = hentMedBehandlingId(behandlingId)
        // Oppdatering kan medføre opprettelse av et nytt blank skjema, ellers oppdatering skal lagres
        val oppdatertSkjema = oppdatering.lagBlankSkjemaEllerNull(skjemaer) ?: oppdatering
        // håndterer oppdatering av alle skjemaer pga oppdatering
        val oppdaterteSkjemaer = oppdaterSkjemaer(skjemaer, oppdatertSkjema)

        lagreDifferanseOgVarsleAbonnenter(behandlingId, skjemaer, oppdaterteSkjemaer.medBehandlingId(behandlingId))
    }

    fun slettSkjema(skjemaId: Long) {
        val skjemaTilSletting = hentMedId(skjemaId)
        val behandlingId = skjemaTilSletting.behandlingId
        val eksisterendeSkjemaer = hentMedBehandlingId(behandlingId)
        val blanktSkjema = skjemaTilSletting.utenInnhold()

        val oppdaterteKompetanser = eksisterendeSkjemaer
            .minus(skjemaTilSletting)
            .plus(blanktSkjema)
            .slåSammen()
            .medBehandlingId(behandlingId)

        lagreDifferanseOgVarsleAbonnenter(behandlingId, eksisterendeSkjemaer, oppdaterteKompetanser)
    }

    fun lagreDifferanseOgVarsleAbonnenter(behandlingId: Long, eksisterende: List<T>, oppdaterte: List<T>) {
        val skalSlettes = eksisterende - oppdaterte
        val skalLagres = oppdaterte - eksisterende

        skjemaRepository.deleteAll(skalSlettes)
        skjemaRepository.saveAll(skalLagres)

        val endringer = skalSlettes + skalLagres
        if (endringer.isNotEmpty()) {
            endringsabonnenter.forEach { it.skjemaerEndret(behandlingId, oppdaterte) }
        }
    }
}
