package no.nav.familie.ks.sak.kjerne.eøs.util

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.tidslinje.Tidslinje
import no.nav.familie.ks.sak.common.tidslinje.filtrerIkkeNull
import no.nav.familie.ks.sak.common.tidslinje.util.tilTidslinje
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioder
import no.nav.familie.ks.sak.cucumber.mocking.mockUnleashService
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagTestPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.tilfeldigPerson
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Regelverk.EØS_FORORDNINGEN
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Regelverk.NASJONALE_REGLER
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat.IKKE_OPPFYLT
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat.OPPFYLT
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering.DELT_BOSTED
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.beregning.AndelGenerator
import no.nav.familie.ks.sak.kjerne.beregning.BeregnAndelTilkjentYtelseService
import no.nav.familie.ks.sak.kjerne.beregning.TilkjentYtelseService
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.regelverkFørFebruar2025.RegelverkFørFebruar2025AndelGenerator
import no.nav.familie.ks.sak.kjerne.beregning.regelverkLovendringFebruar2025.RegelverkLovendringFebruar2025AndelGenerator
import no.nav.familie.ks.sak.kjerne.overgangsordning.domene.OvergangsordningAndelRepository
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import java.time.YearMonth

data class VilkårsvurderingBuilder(
    val behandling: Behandling = lagBehandling(),
    private val vilkårsvurdering: Vilkårsvurdering = Vilkårsvurdering(behandling = behandling),
) {
    val personresultater: MutableSet<PersonResultat> = mutableSetOf()
    val personer: MutableSet<Person> = mutableSetOf()

    fun forPerson(
        person: Person,
        startTidspunkt: YearMonth,
    ): PersonResultatBuilder = PersonResultatBuilder(this, startTidspunkt, person)

    fun byggVilkårsvurdering(): Vilkårsvurdering {
        vilkårsvurdering.personResultater = personresultater
        return vilkårsvurdering
    }

    fun byggPersonopplysningGrunnlag(): PersonopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, *personer.toTypedArray())

    data class PersonResultatBuilder(
        val vilkårsvurderingBuilder: VilkårsvurderingBuilder,
        val startTidspunkt: YearMonth,
        private val person: Person = tilfeldigPerson(),
        private val vilkårsresultatTidslinjer: MutableList<Tidslinje<UtdypendeVilkårRegelverkResultat?>> = mutableListOf(),
    ) {
        fun medVilkår(
            v: String,
            vararg vilkår: Vilkår,
        ): PersonResultatBuilder {
            vilkårsresultatTidslinjer.addAll(
                vilkår.map { v.tilUtdypendeVilkårRegelverkResultatTidslinje(it, startTidspunkt) },
            )
            return this
        }

        fun forPerson(
            person: Person,
            startTidspunkt: YearMonth,
        ): PersonResultatBuilder = byggPerson().forPerson(person, startTidspunkt)

        fun byggPerson(): VilkårsvurderingBuilder {
            val personResultat =
                PersonResultat(
                    vilkårsvurdering = vilkårsvurderingBuilder.vilkårsvurdering,
                    aktør = person.aktør,
                )

            val vilkårresultater =
                vilkårsresultatTidslinjer.flatMap {
                    it
                        .tilPerioder()
                        .filtrerIkkeNull()
                        .flatMap { periode -> periode.tilVilkårResultater(personResultat) }
                }

            personResultat.vilkårResultater.addAll(vilkårresultater)
            vilkårsvurderingBuilder.personresultater.add(personResultat)
            vilkårsvurderingBuilder.personer.add(person)

            return vilkårsvurderingBuilder
        }
    }

    fun byggTilkjentYtelse(): TilkjentYtelse {
        val tilkjentYtelseService =
            TilkjentYtelseService(
                beregnAndelTilkjentYtelseService =
                    BeregnAndelTilkjentYtelseService(
                        andelGeneratorLookup = AndelGenerator.Lookup(listOf(RegelverkLovendringFebruar2025AndelGenerator(), RegelverkFørFebruar2025AndelGenerator())),
                        unleashService = mockUnleashService(false),
                    ),
                overgangsordningAndelRepository = mockOvergangsordningAndelRepository(),
                unleashService = mockUnleashService(true),
            )

        return tilkjentYtelseService.beregnTilkjentYtelse(
            vilkårsvurdering = this.byggVilkårsvurdering(),
            personopplysningGrunnlag = this.byggPersonopplysningGrunnlag(),
        )
    }

    private fun mockOvergangsordningAndelRepository(): OvergangsordningAndelRepository =
        mockk<OvergangsordningAndelRepository>().apply {
            every { hentOvergangsordningAndelerForBehandling(any()) } returns emptyList()
        }
}

internal fun Periode<UtdypendeVilkårRegelverkResultat>.tilVilkårResultater(personResultat: PersonResultat): Collection<VilkårResultat> =
    listOf(
        VilkårResultat(
            personResultat = personResultat,
            vilkårType = this.verdi.vilkår,
            resultat = this.verdi.resultat!!,
            vurderesEtter = this.verdi.regelverk,
            periodeFom = this.fom,
            periodeTom = this.tom,
            begrunnelse = "En begrunnelse",
            utdypendeVilkårsvurderinger = this.verdi.utdypendeVilkårsvurderinger,
            behandlingId = personResultat.vilkårsvurdering.behandling.id,
        ),
    )

data class UtdypendeVilkårRegelverkResultat(
    val vilkår: Vilkår,
    val resultat: Resultat?,
    val regelverk: Regelverk?,
    val utdypendeVilkårsvurderinger: List<UtdypendeVilkårsvurdering> = emptyList(),
) {
    constructor(
        vilkår: Vilkår,
        resultat: Resultat?,
        regelverk: Regelverk?,
        vararg utdypendeVilkårsvurdering: UtdypendeVilkårsvurdering,
    ) : this(vilkår, resultat, regelverk, utdypendeVilkårsvurdering.toList())
}

fun String.tilUtdypendeVilkårRegelverkResultatTidslinje(
    vilkår: Vilkår,
    start: YearMonth,
) =
    this.tilTidslinje(start) { char ->
        when (char.lowercaseChar()) {
            '+' -> UtdypendeVilkårRegelverkResultat(vilkår, OPPFYLT, null)
            'n' -> UtdypendeVilkårRegelverkResultat(vilkår, OPPFYLT, NASJONALE_REGLER)
            'x' -> UtdypendeVilkårRegelverkResultat(vilkår, IKKE_OPPFYLT, null)
            'e' -> UtdypendeVilkårRegelverkResultat(vilkår, OPPFYLT, EØS_FORORDNINGEN)
            'é' -> UtdypendeVilkårRegelverkResultat(vilkår, OPPFYLT, EØS_FORORDNINGEN, DELT_BOSTED)
            'd' -> UtdypendeVilkårRegelverkResultat(vilkår, OPPFYLT, null, DELT_BOSTED)
            else -> null
        }
    }
