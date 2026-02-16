package no.nav.familie.ks.sak.no.nav.familie.ks.sak.kjerne.beregning

import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.lagKompetanse
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.lagPersonResultat
import no.nav.familie.ks.sak.data.lagUtenlandskPeriodebeløp
import no.nav.familie.ks.sak.data.lagValutakurs
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.ks.sak.kjerne.beregning.BeregningService
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.brev.lagVilkårResultater
import no.nav.familie.ks.sak.kjerne.eøs.differanseberegning.domene.Intervall
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.KompetanseAktivitet
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.KompetanseRepository
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ks.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpRepository
import no.nav.familie.ks.sak.kjerne.eøs.valutakurs.ValutakursRepository
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

class BeregningServiceTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var beregningService: BeregningService

    @Autowired
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    private lateinit var kompetanseRepository: KompetanseRepository

    @Autowired
    private lateinit var utenlandskPeriodebeløpRepository: UtenlandskPeriodebeløpRepository

    @Autowired
    private lateinit var valutakursRepository: ValutakursRepository

    @Autowired
    private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    @Nested
    open inner class OppdaterTilkjentYtelsePåBehandlingFraVilkårsvurdering {
        @Test
        @Transactional
        open fun `Skal generere andelerTilkjentYtelse med kobling til TilkjentYtelse`() {
            // Arrange
            val søkerFødselsdato = LocalDate.of(1985, 4, 11)
            val barnFødselsdato = LocalDate.of(2023, 11, 1)

            val søkerAktør = lagreAktør(randomAktør())
            val barnAktør = lagreAktør(randomAktør())

            val fagsak = opprettOgLagreFagsak(lagFagsak(aktør = søkerAktør))

            val behandling =
                opprettOgLagreBehandling(lagBehandling(fagsak = fagsak, type = BehandlingType.FØRSTEGANGSBEHANDLING))

            val personopplysningGrunnlag =
                lagrePersonopplysningGrunnlag(personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandling.id))

            val søker =
                lagrePerson(
                    lagPerson(
                        personType = PersonType.SØKER,
                        fødselsdato = søkerFødselsdato,
                        aktør = søkerAktør,
                        personopplysningGrunnlag = personopplysningGrunnlag,
                    ),
                ).also { personopplysningGrunnlag.personer.add(it) }
            val barn =
                lagrePerson(
                    lagPerson(
                        personType = PersonType.BARN,
                        fødselsdato = barnFødselsdato,
                        aktør = barnAktør,
                        personopplysningGrunnlag = personopplysningGrunnlag,
                    ),
                ).also { personopplysningGrunnlag.personer.add(it) }

            val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)

            vilkårsvurdering.personResultater =
                setOf(
                    lagPersonResultat(
                        vilkårsvurdering = vilkårsvurdering,
                        aktør = søker.aktør,
                        lagVilkårResultater = { personResultat ->
                            // Oppretter oppfylte vilkår fra søkers fødselsdato frem til uendelig
                            lagVilkårResultater(
                                person = søker,
                                personResultat = personResultat,
                                behandlingId = behandling.id,
                            ).toSet()
                        },
                    ),
                    lagPersonResultat(
                        vilkårsvurdering = vilkårsvurdering,
                        aktør = barn.aktør,
                        lagVilkårResultater = { personResultat ->
                            // Oppretter oppfylte vilkår fra barnets 1 årsdag frem til barnets 2 årsdag.
                            lagVilkårResultater(
                                person = barn,
                                personResultat = personResultat,
                                behandlingId = behandling.id,
                            ).toSet()
                        },
                    ),
                )

            vilkårsvurderingRepository.saveAndFlush(vilkårsvurdering)

            // Act
            beregningService.oppdaterTilkjentYtelsePåBehandlingFraVilkårsvurdering(
                behandling,
                personopplysningGrunnlag,
                vilkårsvurdering,
            )

            // Assert
            val tilkjentYtelse = tilkjentYtelseRepository.hentTilkjentYtelseForBehandling(behandling.id)
            val andelerBarn =
                tilkjentYtelse.andelerTilkjentYtelse.filter { it.aktør.aktivFødselsnummer() == barn.aktør.aktivFødselsnummer() }

            assertNotNull(tilkjentYtelse)
            assertTrue(tilkjentYtelse.andelerTilkjentYtelse.isNotEmpty())
            assertThat(andelerBarn).hasSize(1)
            val andelBarn = andelerBarn.single()
            assertThat(andelBarn.stønadFom).isEqualTo(barnFødselsdato.plusYears(1).toYearMonth())
            assertThat(andelBarn.stønadTom).isEqualTo(barnFødselsdato.plusYears(2).toYearMonth())
            assertThat(andelBarn.kalkulertUtbetalingsbeløp).isEqualTo(7500)
        }

        @Test
        @Transactional
        open fun `genererTilkjentYtelseFraVilkårsvurdering - skal trigge differanseberegning`() {
            // Arrange
            val søkerFødselsdato = LocalDate.of(1985, 4, 11)
            val barnFødselsdato = LocalDate.of(2023, 11, 1)

            val søkerAktør = lagreAktør(randomAktør())
            val barnAktør = lagreAktør(randomAktør())

            val fagsak = opprettOgLagreFagsak(lagFagsak(aktør = søkerAktør))

            val behandling =
                opprettOgLagreBehandling(lagBehandling(fagsak = fagsak, type = BehandlingType.FØRSTEGANGSBEHANDLING))

            val personopplysningGrunnlag =
                lagrePersonopplysningGrunnlag(personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandling.id))

            val søker =
                lagrePerson(
                    lagPerson(
                        personType = PersonType.SØKER,
                        fødselsdato = søkerFødselsdato,
                        aktør = søkerAktør,
                        personopplysningGrunnlag = personopplysningGrunnlag,
                    ),
                ).also { personopplysningGrunnlag.personer.add(it) }
            val barn =
                lagrePerson(
                    lagPerson(
                        personType = PersonType.BARN,
                        fødselsdato = barnFødselsdato,
                        aktør = barnAktør,
                        personopplysningGrunnlag = personopplysningGrunnlag,
                    ),
                ).also { personopplysningGrunnlag.personer.add(it) }

            val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)

            vilkårsvurdering.personResultater =
                setOf(
                    lagPersonResultat(
                        vilkårsvurdering = vilkårsvurdering,
                        aktør = søker.aktør,
                        lagVilkårResultater = { personResultat ->
                            // Oppretter oppfylte vilkår fra søkers fødselsdato frem til uendelig
                            lagVilkårResultater(
                                person = søker,
                                personResultat = personResultat,
                                behandlingId = behandling.id,
                            ).toSet()
                        },
                    ),
                    lagPersonResultat(
                        vilkårsvurdering = vilkårsvurdering,
                        aktør = barn.aktør,
                        lagVilkårResultater = { personResultat ->
                            // Oppretter oppfylte vilkår fra barnets 1 årsdag frem til barnets 2 årsdag.
                            lagVilkårResultater(
                                person = barn,
                                personResultat = personResultat,
                                behandlingId = behandling.id,
                            ).toSet()
                        },
                    ),
                )

            vilkårsvurderingRepository.saveAndFlush(vilkårsvurdering)

            val kompetanseNorgeErSekundærland =
                lagKompetanse(
                    behandlingId = behandling.id,
                    fom = barnFødselsdato.plusYears(1).toYearMonth(),
                    barnAktører = setOf(barn.aktør),
                    søkersAktivitet = KompetanseAktivitet.ARBEIDER,
                    annenForeldersAktivitet = KompetanseAktivitet.ARBEIDER,
                    annenForeldersAktivitetsland = "Sverige",
                    søkersAktivitetsland = "Norge",
                    barnetsBostedsland = "Norge",
                    resultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
                )
            val utenlandskPeriodebeløp =
                lagUtenlandskPeriodebeløp(
                    behandlingId = behandling.id,
                    fom = barnFødselsdato.plusYears(1).toYearMonth(),
                    barnAktører = setOf(barn.aktør),
                    beløp = BigDecimal.valueOf(1000),
                    kalkulertUtbetalingsbeløp = BigDecimal.valueOf(1000),
                    intervall = Intervall.MÅNEDLIG,
                    valutakode = "SEK",
                    utbetalingsland = "Sverige",
                )
            val valutakurs =
                lagValutakurs(
                    behandlingId = behandling.id,
                    fom = barnFødselsdato.plusYears(1).toYearMonth(),
                    barnAktører = setOf(barn.aktør),
                    valutakursdato = LocalDate.now(),
                    valutakode = "SEK",
                    kurs = BigDecimal.valueOf(1.1),
                )

            kompetanseRepository.saveAndFlush(kompetanseNorgeErSekundærland)
            utenlandskPeriodebeløpRepository.saveAndFlush(utenlandskPeriodebeløp)
            valutakursRepository.saveAndFlush(valutakurs)

            // Act
            beregningService.oppdaterTilkjentYtelsePåBehandlingFraVilkårsvurdering(
                behandling = behandling,
                personopplysningGrunnlag = personopplysningGrunnlag,
                vilkårsvurdering = vilkårsvurdering,
            )

            // Assert
            val tilkjentYtelse = tilkjentYtelseRepository.hentTilkjentYtelseForBehandling(behandling.id)
            val andelerBarn =
                tilkjentYtelse.andelerTilkjentYtelse.filter { it.aktør.aktivFødselsnummer() == barn.aktør.aktivFødselsnummer() }
            assertNotNull(tilkjentYtelse)
            assertTrue(tilkjentYtelse.andelerTilkjentYtelse.isNotEmpty())
            assertThat(andelerBarn).hasSize(1)
            val andelBarn = andelerBarn.single()
            assertThat(andelBarn.differanseberegnetPeriodebeløp).isNotNull
            assertThat(andelBarn.stønadFom).isEqualTo(barnFødselsdato.plusYears(1).toYearMonth())
            assertThat(andelBarn.stønadTom).isEqualTo(barnFødselsdato.plusYears(2).toYearMonth())
            assertThat(andelBarn.kalkulertUtbetalingsbeløp).isEqualTo(6400) // 7500 - (1000 SEK * 1.1)
        }
    }
}
