package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.utbetalingsperiodeMedBegrunnelser

import io.mockk.junit5.MockKExtension
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioder
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.tilYearMonth
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.lagVilkårsvurderingMedSøkersVilkår
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.tilFørskjøvetOppfylteVilkårResultatTidslinjeMap
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import java.time.YearMonth

@ExtendWith(MockKExtension::class)
class UtbetalingsperiodeMedBegrunnelserServiceTest {

    private val mars2020 = YearMonth.of(2020, 3)
    private val april2020 = YearMonth.of(2020, 4)
    private val mai2020 = YearMonth.of(2020, 5)
    private val juni2020 = YearMonth.of(2020, 6)

    // Fom mars2020 til tom juni2020 gir utbetaling bare i april2020 og mai2020
    @Test
    fun `lagFørskjøvetVilkårResultatTidslinjeMap - skal kutte først og siste måned`() {
        val søker = randomAktør()
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val personopplysningGrunnlag =
            lagPersonopplysningGrunnlag(
                søkerAktør = søker,
                behandlingId = behandling.id,
                søkerPersonIdent = søker.aktivFødselsnummer()
            )
        val søkerPerson = lagPerson(personopplysningGrunnlag, søker, PersonType.SØKER)

        val vilkårsvurdering = lagVilkårsvurderingMedSøkersVilkår(
            søkerAktør = søkerPerson.aktør,
            behandling = behandling,
            resultat = Resultat.OPPFYLT
        )

        val personResultat = PersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = søkerPerson.aktør
        )
        val vilkårResultater = Vilkår.hentVilkårFor(søkerPerson.type).map {
            VilkårResultat(
                personResultat = personResultat,
                periodeFom = mars2020.førsteDagIInneværendeMåned(),
                periodeTom = juni2020.sisteDagIInneværendeMåned(),
                vilkårType = it,
                resultat = Resultat.OPPFYLT,
                begrunnelse = "",
                behandlingId = vilkårsvurdering.behandling.id,
                utdypendeVilkårsvurderinger = emptyList()
            )
        }

        personResultat.setSortedVilkårResultater(vilkårResultater.toSet())

        val førskjøvetVilkårResultatTidslinjeMap =
            setOf(personResultat).tilFørskjøvetOppfylteVilkårResultatTidslinjeMap(personopplysningGrunnlag)

        Assertions.assertEquals(1, førskjøvetVilkårResultatTidslinjeMap.size)

        val forskjøvedeVedtaksperioder = førskjøvetVilkårResultatTidslinjeMap[søkerPerson.aktør]!!.tilPerioder()

        Assertions.assertEquals(april2020, forskjøvedeVedtaksperioder.first().fom?.tilYearMonth())
        Assertions.assertEquals(mai2020, forskjøvedeVedtaksperioder.first().tom?.tilYearMonth())
    }

    @Test
    fun `lagFørskjøvetVilkårResultatTidslinjeMap - skal håndtere bor med søker-overlapp`() {
        val søker = randomAktør()
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val personopplysningGrunnlag =
            lagPersonopplysningGrunnlag(
                søkerAktør = søker,
                behandlingId = behandling.id,
                søkerPersonIdent = søker.aktivFødselsnummer()
            )
        val søkerPerson = lagPerson(personopplysningGrunnlag, søker, PersonType.SØKER)

        val vilkårsvurdering = lagVilkårsvurderingMedSøkersVilkår(
            søkerAktør = søkerPerson.aktør,
            behandling = behandling,
            resultat = Resultat.OPPFYLT
        )

        val personResultat = PersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = søkerPerson.aktør
        ).also {
            it.setSortedVilkårResultater(
                setOf(
                    VilkårResultat(
                        personResultat = it,
                        periodeFom = mars2020.førsteDagIInneværendeMåned(),
                        periodeTom = null,
                        vilkårType = Vilkår.BOR_MED_SØKER,
                        resultat = Resultat.OPPFYLT,
                        begrunnelse = "",
                        behandlingId = vilkårsvurdering.behandling.id
                    ),
                    VilkårResultat(
                        personResultat = it,
                        periodeFom = null,
                        periodeTom = null,
                        vilkårType = Vilkår.BOR_MED_SØKER,
                        resultat = Resultat.IKKE_OPPFYLT,
                        begrunnelse = "",
                        behandlingId = vilkårsvurdering.behandling.id,
                        erEksplisittAvslagPåSøknad = true
                    )
                )
            )
        }

        assertDoesNotThrow {
            setOf(personResultat).tilFørskjøvetOppfylteVilkårResultatTidslinjeMap(personopplysningGrunnlag)
        }
    }
}
