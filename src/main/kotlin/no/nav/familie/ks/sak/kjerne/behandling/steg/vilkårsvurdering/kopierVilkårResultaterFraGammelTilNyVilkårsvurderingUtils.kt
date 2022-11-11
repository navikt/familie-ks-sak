package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering

import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.AnnenVurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering

/**
 * Dersom personer i forrigeBehandlingVilkårsvurdering har vurderte vilkår i aktivtResultat vil disse flyttes til initiellVilkårsvurdering
 * (altså vil tilsvarende vilkår overskrives i initieltResultat og slettes fra aktivtResultat).
 *
 * @param initiellVilkårsvurdering - Vilkårsvurdering med vilkår basert på siste behandlignsgrunnlag. Skal bli neste aktive.
 * @param forrigeBehandlingVilkårsvurdering - Vilkårsvurdering fra forrige behandling.
 * @return vilkårsvurdering med vilkårresultater fra forrige iverksatte behandling:
 */
fun kopierVilkårResultaterFraGammelTilNyVilkårsvurdering(
    initiellVilkårsvurdering: Vilkårsvurdering,
    forrigeBehandlingVilkårsvurdering: Vilkårsvurdering
): Vilkårsvurdering {
    // OBS!! MÅ jobbe på kopier av vilkårsvurderingen her for å ikke oppdatere databasen
    // Viktig at det er vår egen implementasjon av kopier som brukes, da kotlin sin copy-funksjon er en shallow copy
    val initiellVilkårsvurderingKopi = initiellVilkårsvurdering.kopier()
    val aktivVilkårsvurderingKopi = forrigeBehandlingVilkårsvurdering.kopier(inkluderAndreVurderinger = true)

    // Identifiserer hvilke vilkår som skal legges til og hvilke som kan fjernes
    val personResultaterAktivt = aktivVilkårsvurderingKopi.personResultater.toMutableSet()
    val personResultaterOppdatert = mutableSetOf<PersonResultat>()
    initiellVilkårsvurderingKopi.personResultater.forEach { personFraInit ->
        val personTilOppdatert = PersonResultat(
            vilkårsvurdering = initiellVilkårsvurderingKopi,
            aktør = personFraInit.aktør
        )
        val personenSomFinnes = personResultaterAktivt.firstOrNull { it.aktør == personFraInit.aktør }

        if (personenSomFinnes == null) {
            // Legg til ny person
            personTilOppdatert.setSortedVilkårResultater(
                personFraInit.vilkårResultater.map { it.kopierMedParent(personTilOppdatert) }
                    .toSet()
            )
        } else {
            // Fyll inn den initierte med person fra aktiv
            oppdaterEksisterendePerson(
                personenSomFinnes = personenSomFinnes,
                personFraInit = personFraInit,
                kopieringSkjerFraForrigeBehandling = initiellVilkårsvurderingKopi.behandling.id != aktivVilkårsvurderingKopi.behandling.id,
                personTilOppdatert = personTilOppdatert,
                personResultaterAktivt = personResultaterAktivt
            )
        }
        personResultaterOppdatert.add(personTilOppdatert)
    }

    aktivVilkårsvurderingKopi.personResultater = personResultaterAktivt
    initiellVilkårsvurderingKopi.personResultater = personResultaterOppdatert

    return initiellVilkårsvurderingKopi
}

private fun oppdaterEksisterendePerson(
    personenSomFinnes: PersonResultat,
    personFraInit: PersonResultat,
    kopieringSkjerFraForrigeBehandling: Boolean,
    personTilOppdatert: PersonResultat,
    personResultaterAktivt: MutableSet<PersonResultat>
) {
    val personsVilkårAktivt = personenSomFinnes.vilkårResultater.toMutableSet()
    val personsAndreVurderingerAktivt = personenSomFinnes.andreVurderinger.toMutableSet()
    val personsVilkårOppdatert = mutableSetOf<VilkårResultat>()
    val personsAndreVurderingerOppdatert = mutableSetOf<AnnenVurdering>()

    personFraInit.vilkårResultater.forEach { vilkårFraInit ->
        val vilkårSomFinnes =
            personenSomFinnes.vilkårResultater.filter { it.vilkårType == vilkårFraInit.vilkårType }

        val vilkårSomSkalKopieresOver = vilkårSomFinnes.filtrerVilkårÅKopiere(
            kopieringSkjerFraForrigeBehandling = kopieringSkjerFraForrigeBehandling
        )
        val vilkårSomSkalFjernesFraAktivt = vilkårSomFinnes - vilkårSomSkalKopieresOver
        personsVilkårAktivt.removeAll(vilkårSomSkalFjernesFraAktivt)

        if (vilkårSomSkalKopieresOver.isEmpty()) {
            // Legg til nytt vilkår på person
            personsVilkårOppdatert.add(vilkårFraInit.kopierMedParent(personTilOppdatert))
        } else {
            /*  Vilkår er vurdert på person - flytt fra aktivt og overskriv initierte
                        ikke oppfylte eller ikke vurdert perioder skal ikke kopieres om minst en oppfylt
                        periode eksisterer. */

            personsVilkårOppdatert.addAll(
                vilkårSomSkalKopieresOver.map { it.kopierMedParent(personTilOppdatert) }
            )
            personsVilkårAktivt.removeAll(vilkårSomSkalKopieresOver)
        }
    }
    if (!kopieringSkjerFraForrigeBehandling) {
        personenSomFinnes.andreVurderinger.forEach {
            personsAndreVurderingerOppdatert.add(it.kopierMedParent(personTilOppdatert))
            personsAndreVurderingerAktivt.remove(it)
        }
    }

    personTilOppdatert.setSortedVilkårResultater(personsVilkårOppdatert.toSet())
    personTilOppdatert.setAndreVurderinger(personsAndreVurderingerOppdatert.toSet())

    // Fjern person fra aktivt dersom alle vilkår er fjernet, ellers oppdater
    if (personsVilkårAktivt.isEmpty()) {
        personResultaterAktivt.remove(personenSomFinnes)
    } else {
        personenSomFinnes.setSortedVilkårResultater(personsVilkårAktivt.toSet())
    }
}

private fun List<VilkårResultat>.filtrerVilkårÅKopiere(kopieringSkjerFraForrigeBehandling: Boolean): List<VilkårResultat> {
    return if (kopieringSkjerFraForrigeBehandling) {
        this.filter { it.resultat == Resultat.OPPFYLT }
    } else {
        this
    }
}
