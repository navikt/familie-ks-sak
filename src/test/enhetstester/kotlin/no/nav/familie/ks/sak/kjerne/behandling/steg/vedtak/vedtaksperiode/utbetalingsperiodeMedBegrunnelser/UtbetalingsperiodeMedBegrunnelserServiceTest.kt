package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.utbetalingsperiodeMedBegrunnelser

import io.mockk.junit5.MockKExtension
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.tilYearMonth
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.lagPersonResultatFraVilkårResultater
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.tilForskjøvetOppfylteVilkårResultatTidslinjeMap
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
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
        val søkerPerson = lagPerson(aktør = randomAktør(), personType = PersonType.SØKER)
        val barnPerson = lagPerson(aktør = randomAktør(), personType = PersonType.BARN, fødselsdato = LocalDate.of(2021, 10, 5))
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val personopplysningGrunnlag =
            lagPersonopplysningGrunnlag(
                søkerAktør = søkerPerson.aktør,
                behandlingId = behandling.id,
                søkerPersonIdent = søkerPerson.aktør.aktivFødselsnummer(),
                barnAktør = listOf(barnPerson.aktør),
                barnasFødselsdatoer = listOf(barnPerson.fødselsdato),
            )

        val vilkårResultaterSøker =
            Vilkår
                .hentVilkårFor(søkerPerson.type)
                .map {
                    lagVilkårResultat(
                        periodeFom = mars2020.førsteDagIInneværendeMåned(),
                        periodeTom = juni2020.sisteDagIInneværendeMåned(),
                        vilkårType = it,
                        resultat = Resultat.OPPFYLT,
                        begrunnelse = "",
                        utdypendeVilkårsvurderinger = emptyList(),
                    )
                }.toSet()

        val vilkårResultaterBarn =
            Vilkår
                .hentVilkårFor(barnPerson.type)
                .map {
                    lagVilkårResultat(
                        periodeFom = mars2020.førsteDagIInneværendeMåned(),
                        periodeTom = juni2020.sisteDagIInneværendeMåned(),
                        vilkårType = it,
                        resultat = Resultat.OPPFYLT,
                        begrunnelse = "",
                        utdypendeVilkårsvurderinger = emptyList(),
                    )
                }.toSet()

        val personResultater =
            listOf(
                lagPersonResultatFraVilkårResultater(
                    aktør = søkerPerson.aktør,
                    vilkårResultater = vilkårResultaterSøker,
                ),
                lagPersonResultatFraVilkårResultater(
                    aktør = barnPerson.aktør,
                    vilkårResultater = vilkårResultaterBarn,
                ),
            )

        val førskjøvetVilkårResultatTidslinjeMap =
            personResultater.tilForskjøvetOppfylteVilkårResultatTidslinjeMap(
                personopplysningGrunnlag = personopplysningGrunnlag,
                adopsjonerIBehandling = emptyList(),
            )

        Assertions.assertEquals(2, førskjøvetVilkårResultatTidslinjeMap.size)

        val forskjøvedeVedtaksperioder = førskjøvetVilkårResultatTidslinjeMap[søkerPerson.aktør]!!.tilPerioder()

        Assertions.assertEquals(april2020, forskjøvedeVedtaksperioder.first().fom?.tilYearMonth())
        Assertions.assertEquals(mai2020, forskjøvedeVedtaksperioder.first().tom?.tilYearMonth())
    }

    @Test
    fun `lagFørskjøvetVilkårResultatTidslinjeMap - skal håndtere bor med søker-overlapp`() {
        val søker = lagPerson(personType = PersonType.SØKER, aktør = randomAktør())
        val barn = lagPerson(personType = PersonType.BARN, aktør = randomAktør(), fødselsdato = LocalDate.of(2021, 10, 5))
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val personopplysningGrunnlag =
            lagPersonopplysningGrunnlag(
                søkerAktør = søker.aktør,
                behandlingId = behandling.id,
                søkerPersonIdent = søker.aktør.aktivFødselsnummer(),
                barnAktør = listOf(barn.aktør),
                barnasFødselsdatoer = listOf(barn.fødselsdato),
            )

        val vilkårResultaterSøker =
            Vilkår
                .hentVilkårFor(søker.type)
                .map {
                    lagVilkårResultat(
                        periodeFom = mars2020.førsteDagIInneværendeMåned(),
                        periodeTom = juni2020.sisteDagIInneværendeMåned(),
                        vilkårType = it,
                        resultat = Resultat.OPPFYLT,
                        begrunnelse = "",
                        utdypendeVilkårsvurderinger = emptyList(),
                    )
                }.toSet()

        val vilkårResultaterBarn =
            Vilkår
                .hentVilkårFor(barn.type)
                .map {
                    lagVilkårResultat(
                        periodeFom = mars2020.førsteDagIInneværendeMåned(),
                        periodeTom = juni2020.sisteDagIInneværendeMåned(),
                        vilkårType = it,
                        resultat = Resultat.OPPFYLT,
                        begrunnelse = "",
                        utdypendeVilkårsvurderinger = emptyList(),
                    )
                }.toSet()
                .plus(
                    lagVilkårResultat(
                        periodeFom = null,
                        periodeTom = null,
                        vilkårType = Vilkår.BOR_MED_SØKER,
                        resultat = Resultat.IKKE_OPPFYLT,
                        begrunnelse = "",
                        utdypendeVilkårsvurderinger = emptyList(),
                        erEksplisittAvslagPåSøknad = true,
                    ),
                )

        val personResultater =
            listOf(
                lagPersonResultatFraVilkårResultater(
                    aktør = søker.aktør,
                    vilkårResultater = vilkårResultaterSøker,
                ),
                lagPersonResultatFraVilkårResultater(
                    aktør = barn.aktør,
                    vilkårResultater = vilkårResultaterBarn,
                ),
            )

        assertDoesNotThrow {
            personResultater.tilForskjøvetOppfylteVilkårResultatTidslinjeMap(
                personopplysningGrunnlag = personopplysningGrunnlag,
                adopsjonerIBehandling = emptyList(),
            )
        }
    }
}
