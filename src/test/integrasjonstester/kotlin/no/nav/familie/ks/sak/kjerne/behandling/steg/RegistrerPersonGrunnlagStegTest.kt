package no.nav.familie.ks.sak.kjerne.behandling.steg

import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagEndretUtbetalingAndel
import no.nav.familie.ks.sak.data.lagInitieltTilkjentYtelse
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.data.shouldNotBeNull
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakStatus
import no.nav.familie.ks.sak.kjerne.overgangsordning.domene.OvergangsordningAndel
import no.nav.familie.ks.sak.kjerne.overgangsordning.domene.OvergangsordningAndelRepository
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Målform
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlagRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.YearMonth

class RegistrerPersonGrunnlagStegTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var registrerPersonGrunnlagSteg: RegistrerPersonGrunnlagSteg

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    @Autowired
    private lateinit var vilkårsvurderingService: VilkårsvurderingService

    @Autowired
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    private lateinit var overgangsordningAndelRepository: OvergangsordningAndelRepository

    @Autowired
    private lateinit var endretUtbetalingAndelRepository: EndretUtbetalingAndelRepository

    @BeforeEach
    fun init() {
        opprettSøkerFagsakOgBehandling(fagsakStatus = FagsakStatus.LØPENDE)
    }

    @Test
    fun `utførSteg skal utføre REGISTRERE_PERSONGRUNNLAG steg for FGB`() {
        // Arrange
        assertDoesNotThrow { registrerPersonGrunnlagSteg.utførSteg(behandling.id) }

        // Assert
        val personopplysningGrunnlag =
            personopplysningGrunnlagRepository.findByBehandling(behandling.id).shouldNotBeNull()
        assertEquals(1, personopplysningGrunnlag.personer.size)

        val person = personopplysningGrunnlag.personer.single()
        assertEquals(behandling.fagsak.aktør, person.aktør)
        assertEquals(Målform.NB, person.målform)
        assertEquals(PersonType.SØKER, person.type)

        assertTrue(person.sivilstander.isNotEmpty())
        assertTrue(person.bostedsadresser.isNotEmpty())
        assertTrue(person.statsborgerskap.isEmpty())
    }

    @Test
    fun `utførSteg skal utføre REGISTRERE_PERSONGRUNNLAG steg for Revurdering med opprettet årsak søknad`() {
        // Arrange
        val barnAktør = lagreAktør(randomAktør())

        val gammelPersonopplysningGrunnlag =
            lagPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = behandling.fagsak.aktør.aktivFødselsnummer(),
                søkerAktør = behandling.fagsak.aktør,
                barnasIdenter = listOf(barnAktør.aktivFødselsnummer()),
                barnAktør = listOf(barnAktør),
            )

        // Act
        personopplysningGrunnlagRepository.save(gammelPersonopplysningGrunnlag)

        lagreBehandling(
            behandling.also {
                it.status = BehandlingStatus.AVSLUTTET
                it.aktiv = false
            },
        )

        val revurdering =
            behandlingRepository.saveAndFlush(
                lagBehandling(
                    fagsak = behandling.fagsak,
                    type = BehandlingType.REVURDERING,
                    opprettetÅrsak = BehandlingÅrsak.SØKNAD,
                ),
            )

        // Assert
        assertDoesNotThrow { registrerPersonGrunnlagSteg.utførSteg(revurdering.id) }

        val personopplysningGrunnlag =
            personopplysningGrunnlagRepository.findByBehandling(revurdering.id).shouldNotBeNull()
        assertEquals(1, personopplysningGrunnlag.personer.size)

        val søker = personopplysningGrunnlag.personer.single { it.type == PersonType.SØKER }
        assertEquals(revurdering.fagsak.aktør, søker.aktør)
        assertEquals(Målform.NB, søker.målform)

        assertTrue(søker.sivilstander.isNotEmpty())
        assertTrue(søker.bostedsadresser.isNotEmpty())
        assertTrue(søker.statsborgerskap.isEmpty())

        assertTrue { vilkårsvurderingService.finnAktivVilkårsvurdering(revurdering.id) == null }
    }

    @Test
    fun `utførSteg skal utføre REGISTRERE_PERSONGRUNNLAG steg for Revurdering med opprettet årsak ÅRLIG_KONTROLL`() {
        // Arrange
        val barnAktør = lagreAktør(randomAktør())

        val gammelPersonopplysningGrunnlag =
            lagPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = behandling.fagsak.aktør.aktivFødselsnummer(),
                søkerAktør = behandling.fagsak.aktør,
                barnasIdenter = listOf(randomFnr()),
                barnAktør = listOf(barnAktør),
            )

        // Act
        personopplysningGrunnlagRepository.save(gammelPersonopplysningGrunnlag)
        vilkårsvurderingService.opprettVilkårsvurdering(behandling, null)

        lagreBehandling(
            behandling.also {
                it.status = BehandlingStatus.AVSLUTTET
                it.aktiv = false
            },
        )

        val revurdering =
            behandlingRepository.saveAndFlush(
                lagBehandling(
                    fagsak = behandling.fagsak,
                    type = BehandlingType.REVURDERING,
                    opprettetÅrsak = BehandlingÅrsak.ÅRLIG_KONTROLL,
                ),
            )

        // Assert
        assertDoesNotThrow { registrerPersonGrunnlagSteg.utførSteg(revurdering.id) }

        val personopplysningGrunnlag =
            personopplysningGrunnlagRepository.findByBehandling(revurdering.id).shouldNotBeNull()
        assertEquals(1, personopplysningGrunnlag.personer.size)

        val søker = personopplysningGrunnlag.personer.single { it.type == PersonType.SØKER }
        assertEquals(revurdering.fagsak.aktør, søker.aktør)
        assertEquals(Målform.NB, søker.målform)

        assertTrue(søker.sivilstander.isNotEmpty())
        assertTrue(søker.bostedsadresser.isNotEmpty())
        assertTrue(søker.statsborgerskap.isEmpty())

        assertTrue { vilkårsvurderingService.finnAktivVilkårsvurdering(revurdering.id) != null }
    }

    @Test
    fun `utførSteg skal ta med barn med tilkjent ytelse og kopiere andelene deres i ny førstegangsbehandling`() {
        // Arrange
        opprettPersonopplysningGrunnlagOgPersonForBehandling(behandlingId = behandling.id, lagBarn = true)

        val tilkjentYtelse = lagInitieltTilkjentYtelse(behandling)
        tilkjentYtelse.andelerTilkjentYtelse.add(
            lagAndelTilkjentYtelse(
                tilkjentYtelse = tilkjentYtelse,
                behandling = behandling,
                aktør = barn,
                stønadFom = YearMonth.of(2024, 8),
                stønadTom = YearMonth.of(2024, 11),
                ytelseType = YtelseType.OVERGANGSORDNING,
            ),
        )
        tilkjentYtelseRepository.saveAndFlush(tilkjentYtelse)

        overgangsordningAndelRepository.saveAndFlush(
            OvergangsordningAndel(
                behandlingId = behandling.id,
                aktør = barn,
                fom = YearMonth.of(2024, 8),
                tom = YearMonth.of(2024, 11),
            ),
        )

        endretUtbetalingAndelRepository.saveAndFlush(
            lagEndretUtbetalingAndel(
                behandlingId = behandling.id,
                aktører = setOf(barn),
                prosent = BigDecimal.ZERO,
                periodeFom = YearMonth.of(2024, 1),
                periodeTom = YearMonth.of(2024, 3),
            ),
        )

        lagreBehandling(
            behandling.also {
                it.status = BehandlingStatus.AVSLUTTET
                it.aktiv = false
            },
        )

        val nyFørstegangsbehandling =
            behandlingRepository.saveAndFlush(
                lagBehandling(
                    fagsak = behandling.fagsak,
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                    opprettetÅrsak = BehandlingÅrsak.SØKNAD,
                ),
            )

        // Act
        registrerPersonGrunnlagSteg.utførSteg(nyFørstegangsbehandling.id)

        // Assert
        val personopplysningGrunnlag =
            personopplysningGrunnlagRepository.findByBehandling(nyFørstegangsbehandling.id).shouldNotBeNull()
        assertThat(personopplysningGrunnlag.barna.map { it.aktør }).containsExactly(barn)

        val kopierteOvergangsordningAndeler =
            overgangsordningAndelRepository.hentOvergangsordningAndelerForBehandling(nyFørstegangsbehandling.id)
        assertThat(kopierteOvergangsordningAndeler)
            .singleElement()
            .satisfies({ assertThat(it.aktør).isEqualTo(barn) })

        val kopierteEndretUtbetalingAndeler =
            endretUtbetalingAndelRepository.hentEndretUtbetalingerForBehandling(nyFørstegangsbehandling.id)
        assertThat(kopierteEndretUtbetalingAndeler)
            .singleElement()
            .satisfies({ assertThat(it.aktører).containsExactly(barn) })
    }
}
