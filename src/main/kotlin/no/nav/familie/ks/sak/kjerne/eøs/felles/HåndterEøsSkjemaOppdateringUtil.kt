package no.nav.familie.ks.sak.kjerne.eøs.felles

import no.nav.familie.ks.sak.common.tidslinje.utvidelser.slåSammen
import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.EøsSkjema
import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.EøsSkjemaEntitet
import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.bareInnhold
import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.erLikBortsettFraBarn
import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.erLikBortsettFraBarnOgTom
import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.erLikBortsettFraTom
import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.manglerBarn
import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.medFjernetBarn
import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.medOverlappendeBarnOgPeriode
import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.tomBlirForkortetEllerLukketAv
import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.trekkFra
import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.utenInnholdTom

// Denne metoden sammenligner eksisterende skjemaer med skjema som er under oppdatering
// og eventuelt oppretter nytt blank skjema for å håndtere oppdatering
fun <T : EøsSkjemaEntitet<T>> T.lagBlankSkjemaEllerNull(skjemaer: List<T>): T? {
    val oppdatering = this

    // a) Henter skjema der
    // 1. tom dato går fra null til en verdi eller tom dato er satt til en tidligere dato,
    // 2. Andre feltene bortsett fra tom dato er lik
    val skjemaetDerTomForkortes =
        skjemaer.singleOrNull { skjema ->
            skjema.tomBlirForkortetEllerLukketAv(oppdatering) && skjema.erLikBortsettFraTom(oppdatering)
        }
    // b) Henter skjema der
    // 1. barn er fjernet fra skjema
    // 2. Andre feltene bortsett fra antall barnAktør er lik
    val skjemaetDerBarnFjernes =
        skjemaer.singleOrNull { skjema ->
            oppdatering.manglerBarn(skjema) && skjema.erLikBortsettFraBarn(oppdatering)
        }
    // c) Henter skjema der både punkt a og punkt b gjelder
    val skjemaetDerTomForkortesOgBarnFjernes =
        skjemaer.singleOrNull { skjema ->
            skjema.tomBlirForkortetEllerLukketAv(oppdatering) &&
                oppdatering.manglerBarn(skjema) &&
                skjema.erLikBortsettFraBarnOgTom(oppdatering)
        }

    return when {
        // oppretter et nytt blank skjema
        // 1. med fjernet barn
        // 2. med perioden som starter måneden etter ny tom dato og frem til eksisterende tom dato(kan være null)
        skjemaetDerTomForkortesOgBarnFjernes != null -> {
            oppdatering
                .medFjernetBarn(skjemaetDerTomForkortesOgBarnFjernes)
                .utenInnholdTom(skjemaetDerTomForkortesOgBarnFjernes.tom)
        }

        // oppretter et nytt blank skjema
        // med fjernet barn
        skjemaetDerBarnFjernes != null -> {
            oppdatering.medFjernetBarn(skjemaetDerBarnFjernes).utenInnhold()
        }

        // oppretter et nytt blank skjema
        // med perioden som starter måneden etter ny tom dato og frem til eksisterende tom dato(kan være null)
        skjemaetDerTomForkortes != null -> {
            oppdatering.utenInnholdTom(skjemaetDerTomForkortes.tom)
        }

        else -> {
            null
        }
    }
}

fun <T : EøsSkjema<T>> oppdaterSkjemaer(
    skjemaer: List<T>,
    oppdatering: T,
): List<T> {
    val førsteSkjemaSomOppdateres =
        skjemaer
            .filter { it.medOverlappendeBarnOgPeriode(oppdatering) != null } // Må overlappe i periode og barn
            .filter {
                it.bareInnhold() != oppdatering.bareInnhold()
            } // Må være en endring i selve innholdet i skjemaet. d.v.s oppdaterer ingenting når det bare er endringer i periode
            .firstOrNull()
            ?: return skjemaer // hvis ingenting overlapper/har ingen endring i innholdet, returnerer eksisterende skjemaer

    // oppdatertSkjema har innholdet fra oppdateringen, samt felles barn og perioder
    val oppdatertSkjema = checkNotNull(oppdatering.medOverlappendeBarnOgPeriode(førsteSkjemaSomOppdateres))

    // førsteSkjemaFratrukketOppdatering inneholder det som "blir igjen",
    // dvs det originale innholdet "utenfor" overlappende barn og periode
    val førsteSkjemaFratrukketOppdatering = førsteSkjemaSomOppdateres.trekkFra(oppdatertSkjema)

    val oppdaterteSkjemaer =
        skjemaer
            .minus(førsteSkjemaSomOppdateres)
            .plus(oppdatertSkjema)
            .plus(førsteSkjemaFratrukketOppdatering)
            .slåSammen()

    return oppdaterSkjemaer(oppdaterteSkjemaer, oppdatering)
}
