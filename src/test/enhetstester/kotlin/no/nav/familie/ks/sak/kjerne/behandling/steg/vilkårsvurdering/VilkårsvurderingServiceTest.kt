package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ks.sak.api.dto.EndreVilkårResultatDto
import no.nav.familie.ks.sak.api.dto.VilkårResultatDto
import no.nav.familie.ks.sak.api.mapper.VilkårsvurderingMapper
import no.nav.familie.ks.sak.common.BehandlingId
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.data.fnrTilFødselsdato
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.lagPersonResultat
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.data.lagVilkårsvurdering
import no.nav.familie.ks.sak.data.lagVilkårsvurderingMedSøkersVilkår
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.integrasjon.sanity.SanityService
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelseType
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityResultat
import no.nav.familie.ks.sak.kjerne.adopsjon.AdopsjonService
import no.nav.familie.ks.sak.kjerne.adopsjon.AdopsjonValidator
import no.nav.familie.ks.sak.kjerne.behandling.TilbakestillBehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.BegrunnelseType
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.NasjonalEllerFellesBegrunnelse
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal
import java.time.LocalDate
import org.hamcrest.CoreMatchers.`is` as Is

class VilkårsvurderingServiceTest {
    private val vilkårsvurderingRepository: VilkårsvurderingRepository = mockk()
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService = mockk()
    private val sanityService: SanityService = mockk()
    private val personidentService: PersonidentService = mockk()
    private val adopsjonService: AdopsjonService = mockk()
    private val adopsjonValidator: AdopsjonValidator = mockk()
    private val tilbakestillBehandlingService = mockk<TilbakestillBehandlingService>(relaxUnitFun = true)
    private val vilkårsvurderingService =
        VilkårsvurderingService(
            vilkårsvurderingRepository,
            personopplysningGrunnlagService,
            sanityService,
            personidentService,
            adopsjonService,
            adopsjonValidator,
            tilbakestillBehandlingService,
        )

    private val søker = randomAktør()

    private val fagsak = lagFagsak(søker)

    private val behandling = lagBehandling(fagsak, opprettetÅrsak = BehandlingÅrsak.SØKNAD)

    @BeforeEach
    fun setUp() {
        every { adopsjonService.hentAlleAdopsjonerForBehandling(any()) } returns emptyList()
    }

    @Test
    fun `opprettVilkårsvurdering - skal opprette tom vilkårsvurdering dersom det ikke finnes tidligere vedtatte behandlinger på fagsak`() {
        // Arrange
        val barn1 = randomAktør()
        val barn2 = randomAktør()

        val lagretVilkårsvurderingSlot = slot<Vilkårsvurdering>()
        every { vilkårsvurderingRepository.finnAktivForBehandling(any()) } returns null
        every { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(any()) } returns
            // Act
            lagPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = søker.aktivFødselsnummer(),
                barnasIdenter = listOf(barn1.aktivFødselsnummer(), barn2.aktivFødselsnummer()),
            )
        every { vilkårsvurderingRepository.save(capture(lagretVilkårsvurderingSlot)) } returns mockk()

        vilkårsvurderingService.opprettVilkårsvurdering(behandling, null)

        val lagretVilkårsvurdering = lagretVilkårsvurderingSlot.captured

        // Assert
        assertEquals(3, lagretVilkårsvurdering.personResultater.size)
        assertThat(
            lagretVilkårsvurdering.personResultater
                .find {
                    it.aktør.aktivFødselsnummer() === søker.aktivFødselsnummer()
                }?.vilkårResultater
                ?.map { it.vilkårType },
            containsInAnyOrder(Vilkår.BOSATT_I_RIKET, Vilkår.MEDLEMSKAP),
        )
        assertThat(
            lagretVilkårsvurdering.personResultater
                .find {
                    it.aktør.aktivFødselsnummer() === barn1.aktivFødselsnummer()
                }?.vilkårResultater
                ?.map { it.vilkårType },
            containsInAnyOrder(
                Vilkår.BOSATT_I_RIKET,
                Vilkår.BARNEHAGEPLASS,
                Vilkår.BOR_MED_SØKER,
                Vilkår.MEDLEMSKAP_ANNEN_FORELDER,
                Vilkår.BARNETS_ALDER,
            ),
        )

        // autoutfylling
        val barnetsAlderVilkårer =
            lagretVilkårsvurdering.personResultater
                .filter { !it.erSøkersResultater() }
                .flatMap { it.vilkårResultater.filter { it.vilkårType == Vilkår.BARNETS_ALDER } }

        val barnehageVilkårer =
            lagretVilkårsvurdering.personResultater
                .filter { !it.erSøkersResultater() }
                .flatMap { it.vilkårResultater.filter { vilkår -> vilkår.vilkårType == Vilkår.BARNEHAGEPLASS } }

        assertTrue {
            barnetsAlderVilkårer.all {
                it.erAutomatiskVurdert &&
                    it.resultat == Resultat.OPPFYLT &&
                    it.begrunnelse == "Vurdert og satt automatisk"
            }
        }
        val barn1FødselsDato = fnrTilFødselsdato(barn1.aktivFødselsnummer())
        assertTrue {
            barnetsAlderVilkårer.any {
                it.periodeFom == barn1FødselsDato.plusYears(1) &&
                    it.periodeTom == barn1FødselsDato.plusYears(2)
            }
        }
        assertTrue { barnehageVilkårer.any { it.periodeFom == barn1FødselsDato } }

        val barn2FødselsDato = fnrTilFødselsdato(barn2.aktivFødselsnummer())
        assertTrue {
            barnetsAlderVilkårer.any {
                it.periodeFom == barn2FødselsDato.plusYears(1) &&
                    it.periodeTom == barn2FødselsDato.plusYears(2)
            }
        }
        assertTrue { barnehageVilkårer.any { it.periodeFom == barn2FødselsDato } }

        val medlemskapVilkår =
            lagretVilkårsvurdering.personResultater
                .single { it.erSøkersResultater() }
                .vilkårResultater
                .single { it.vilkårType == Vilkår.MEDLEMSKAP }
        assertEquals(fnrTilFødselsdato(søker.aktivFødselsnummer()).plusYears(5), medlemskapVilkår.periodeFom)
    }

    @Test
    fun `opprettVilkårsvurdering - skal opprette tom vilkårsvurdering og deaktivere eksisterende dersom det ikke finnes tidligere vedtatte behandlinger på fagsak`() {
        // Arrange
        val barn1 = randomAktør()
        val barn2 = randomAktør()
        val lagretDeaktivertVilkårsvurderingSlot = slot<Vilkårsvurdering>()
        val lagretVilkårsvurderingSlot = slot<Vilkårsvurdering>()
        every { vilkårsvurderingRepository.finnAktivForBehandling(any()) } returns
            // Act
            lagVilkårsvurderingMedSøkersVilkår(
                søkerAktør = søker,
                behandling = behandling,
                resultat = Resultat.OPPFYLT,
            )
        every { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(any()) } returns
            lagPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = søker.aktivFødselsnummer(),
                barnasIdenter = listOf(barn1.aktivFødselsnummer(), barn2.aktivFødselsnummer()),
            )
        every { vilkårsvurderingRepository.saveAndFlush(capture(lagretDeaktivertVilkårsvurderingSlot)) } returns mockk()
        every { vilkårsvurderingRepository.save(capture(lagretVilkårsvurderingSlot)) } returns mockk()

        vilkårsvurderingService.opprettVilkårsvurdering(behandling, null)

        val lagretVilkårsvurdering = lagretVilkårsvurderingSlot.captured
        val lagretDeaktivertVilkårsvurdering = lagretDeaktivertVilkårsvurderingSlot.captured

        // Assert
        assertEquals(3, lagretVilkårsvurdering.personResultater.size)
        assertFalse(lagretDeaktivertVilkårsvurdering.aktiv)
    }

    @Test
    fun `hentVilkårsbegrunnelser - skal returnere et map med begrunnelsestyper mappet mot liste av begrunnelser`() {
        // Arrange
        every { sanityService.hentSanityBegrunnelser() } returns
            // Act
            listOf(
                SanityBegrunnelse(
                    NasjonalEllerFellesBegrunnelse.INNVILGET_IKKE_BARNEHAGE.sanityApiNavn,
                    "innvilgetIkkeBarnehage",
                    SanityBegrunnelseType.STANDARD,
                    Vilkår.entries,
                    rolle = emptyList(),
                    triggere = emptyList(),
                    utdypendeVilkårsvurderinger = emptyList(),
                    hjemler = emptyList(),
                    endretUtbetalingsperiode = emptyList(),
                    endringsårsaker = emptyList(),
                    støtterFritekst = false,
                    skalAlltidVises = false,
                    ikkeIBruk = false,
                    resultat = SanityResultat.INNVILGET,
                ),
            )

        val vilkårsbegrunnelser = vilkårsvurderingService.hentVilkårsbegrunnelser()

        // TODO: Endre denne testen når vi får lagt inn riktige Begrunnelser og EØSBegrunnelser
        // Assert
        assertEquals(9, vilkårsbegrunnelser.size)
        assertEquals(0, vilkårsbegrunnelser[BegrunnelseType.AVSLAG]?.size)
    }

    @Test
    fun `hentAktivVilkårsvurderingForBehandling - skal kaste feil hvis aktiv vilkårsvurdering for behandling ikke eksisterer`() {
        // Arrange
        every { vilkårsvurderingRepository.finnAktivForBehandling(404) } returns null

        val feil =
            // Act
            // Assert
            assertThrows<Feil> {
                vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(404)
            }

        assertThat(feil.message, Is("Fant ikke vilkårsvurdering knyttet til behandling=404"))
    }

    @Test
    fun `hentAktivVilkårsvurderingForBehandling - skal returnere aktiv vilkårsvurdering for behandling`() {
        // Arrange
        val mocketVilkårsvurdering = mockk<Vilkårsvurdering>()

        every { vilkårsvurderingRepository.finnAktivForBehandling(404) } returns mocketVilkårsvurdering

        val hentetVilkårsvurdering = vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(404)

        // Assert
        assertThat(mocketVilkårsvurdering, Is(hentetVilkårsvurdering))
        // Act
    }

    @Test
    fun `slettVilkårPåBehandling - skal kaste feil dersom vilkåret som forsøkes å slettes ikke finnes`() {
        // Arrange
        val vilkårsvurdering =
            // Act
            lagVilkårsvurderingMedSøkersVilkår(
                søkerAktør = søker,
                behandling = behandling,
                resultat = Resultat.OPPFYLT,
            )

        every { personidentService.hentAktør(any()) } returns søker
        every { vilkårsvurderingRepository.finnAktivForBehandling(200) } returns vilkårsvurdering

        val feil =
            // Assert
            assertThrows<Feil> {
                vilkårsvurderingService.slettVilkårPåBehandling(200, 404, søker)
            }

        assertThat(feil.message, Is("Prøver å slette et vilkår som ikke finnes"))
        assertThat(feil.frontendFeilmelding, Is("Vilkåret du prøver å slette finnes ikke i systemet."))
    }

    @Test
    fun `skal lagre endring av kun begrunnelse uten å tilbakestille behandling`() {
        // Arrange
        val vilkårsvurdering = opprettVilkårsvurderingForEndring()
        val personResultat = vilkårsvurdering.personResultater.single()
        val vilkårResultatDto = VilkårsvurderingMapper.lagPersonResultatRespons(personResultat).vilkårResultater.single()

        // Act
        vilkårsvurderingService.endreVilkårPåBehandling(
            behandling.id,
            EndreVilkårResultatDto(
                personIdent = personResultat.aktør.aktivFødselsnummer(),
                adopsjonsdato = null,
                endretVilkårResultat = vilkårResultatDto.copy(begrunnelse = "Ny begrunnelse"),
            ),
        )

        // Assert
        assertThat(personResultat.vilkårResultater.single().begrunnelse).isEqualTo("Ny begrunnelse")
        verify(exactly = 0) { tilbakestillBehandlingService.tilbakestillBehandlingTilVilkårsvurdering(any()) }
    }

    @Test
    fun `skal ikke tilbakestille behandling når vilkåret er uendret`() {
        // Arrange
        val vilkårsvurdering = opprettVilkårsvurderingForEndring()
        val personResultat = vilkårsvurdering.personResultater.single()
        val vilkårResultatDto = VilkårsvurderingMapper.lagPersonResultatRespons(personResultat).vilkårResultater.single()

        // Act
        vilkårsvurderingService.endreVilkårPåBehandling(
            behandling.id,
            EndreVilkårResultatDto(personResultat.aktør.aktivFødselsnummer(), null, vilkårResultatDto),
        )

        // Assert
        verify(exactly = 0) { tilbakestillBehandlingService.tilbakestillBehandlingTilVilkårsvurdering(any()) }
    }

    @ParameterizedTest
    @MethodSource("endringerSomPåvirkerResultat")
    fun `skal tilbakestille behandling når endringen påvirker resultatet`(
        endring: (VilkårResultatDto) -> VilkårResultatDto,
    ) {
        // Arrange
        val vilkårsvurdering = opprettVilkårsvurderingForEndring()
        val personResultat = vilkårsvurdering.personResultater.single()
        val vilkårResultatDto = endring(VilkårsvurderingMapper.lagPersonResultatRespons(personResultat).vilkårResultater.single())

        // Act
        vilkårsvurderingService.endreVilkårPåBehandling(
            behandling.id,
            EndreVilkårResultatDto(personResultat.aktør.aktivFødselsnummer(), null, vilkårResultatDto),
        )

        // Assert
        verify(exactly = 1) { tilbakestillBehandlingService.tilbakestillBehandlingTilVilkårsvurdering(behandling.id) }
    }

    @Test
    fun `skal ikke tilbakestille behandling når null timer erstattes med nullverdi`() {
        // Arrange
        val vilkårsvurdering = opprettVilkårsvurderingForEndring()
        val personResultat = vilkårsvurdering.personResultater.single()
        val vilkårResultatDto = VilkårsvurderingMapper.lagPersonResultatRespons(personResultat).vilkårResultater.single()

        // Act
        vilkårsvurderingService.endreVilkårPåBehandling(
            behandling.id,
            EndreVilkårResultatDto(
                personResultat.aktør.aktivFødselsnummer(),
                null,
                vilkårResultatDto.copy(antallTimer = BigDecimal(0)),
            ),
        )

        // Assert
        assertThat(personResultat.vilkårResultater.single().antallTimer).isNull()
        verify(exactly = 0) { tilbakestillBehandlingService.tilbakestillBehandlingTilVilkårsvurdering(any()) }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `skal bare tilbakestille behandling når adopsjonsdato endres`(endreAdopsjonsdato: Boolean) {
        // Arrange
        val vilkårsvurdering = opprettVilkårsvurderingForEndring(Vilkår.BARNETS_ALDER)
        val personResultat = vilkårsvurdering.personResultater.single()
        val vilkårResultatDto = VilkårsvurderingMapper.lagPersonResultatRespons(personResultat).vilkårResultater.single()
        val eksisterendeAdopsjonsdato = LocalDate.of(2023, 6, 1)
        val nyAdopsjonsdato = if (endreAdopsjonsdato) eksisterendeAdopsjonsdato.plusMonths(1) else eksisterendeAdopsjonsdato

        every { adopsjonService.finnAdopsjonForAktørIBehandling(personResultat.aktør, BehandlingId(behandling.id)) } returns
            mockk {
                every { adopsjonsdato } returns eksisterendeAdopsjonsdato
            }
        every { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandling.id) } returns
            lagPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                barnasIdenter = listOf(personResultat.aktør.aktivFødselsnummer()),
                barnAktør = listOf(personResultat.aktør),
            )
        justRun { adopsjonValidator.validerGyldigAdopsjonstilstandForBarnetsAlderVilkår(any(), any(), any(), any()) }
        justRun { adopsjonService.oppdaterAdopsjonsdato(BehandlingId(behandling.id), personResultat.aktør, nyAdopsjonsdato) }

        // Act
        vilkårsvurderingService.endreVilkårPåBehandling(
            behandling.id,
            EndreVilkårResultatDto(
                personResultat.aktør.aktivFødselsnummer(),
                nyAdopsjonsdato,
                vilkårResultatDto.copy(begrunnelse = "Ny begrunnelse"),
            ),
        )

        // Assert
        verify(exactly = if (endreAdopsjonsdato) 1 else 0) {
            tilbakestillBehandlingService.tilbakestillBehandlingTilVilkårsvurdering(behandling.id)
        }
        verify(exactly = 1) {
            adopsjonService.oppdaterAdopsjonsdato(BehandlingId(behandling.id), personResultat.aktør, nyAdopsjonsdato)
        }
    }

    private fun opprettVilkårsvurderingForEndring(vilkårType: Vilkår = Vilkår.BARNEHAGEPLASS): Vilkårsvurdering {
        val barn = randomAktør()
        val vilkårsvurdering =
            lagVilkårsvurdering(behandling = behandling) { vilkårsvurdering ->
                setOf(
                    lagPersonResultat(
                        vilkårsvurdering = vilkårsvurdering,
                        aktør = barn,
                        lagVilkårResultater = { personResultat ->
                            setOf(
                                lagVilkårResultat(
                                    id = 1L,
                                    personResultat = personResultat,
                                    behandlingId = behandling.id,
                                    vilkårType = vilkårType,
                                    periodeFom = LocalDate.of(2024, 1, 1),
                                    periodeTom = LocalDate.of(2024, 12, 31),
                                    begrunnelse = "Opprinnelig begrunnelse",
                                    utdypendeVilkårsvurderinger = if (vilkårType == Vilkår.BARNETS_ALDER) listOf(UtdypendeVilkårsvurdering.ADOPSJON) else emptyList(),
                                ),
                            )
                        },
                    ),
                )
            }

        every { vilkårsvurderingRepository.finnAktivForBehandling(behandling.id) } returns vilkårsvurdering
        every { personidentService.hentAktør(barn.aktivFødselsnummer()) } returns barn
        return vilkårsvurdering
    }

    companion object {
        @JvmStatic
        fun endringerSomPåvirkerResultat(): List<Arguments> {
            val endringer: Map<String, (VilkårResultatDto) -> VilkårResultatDto> =
                mapOf(
                    "periodeFom" to { it.copy(periodeFom = LocalDate.of(2024, 2, 1)) },
                    "periodeTom" to { it.copy(periodeTom = null) },
                    "resultat" to { it.copy(resultat = Resultat.IKKE_OPPFYLT) },
                    "erEksplisittAvslagPåSøknad" to { it.copy(erEksplisittAvslagPåSøknad = true) },
                    "vurderesEtter" to { it.copy(vurderesEtter = Regelverk.EØS_FORORDNINGEN) },
                    "utdypendeVilkårsvurderinger" to { it.copy(utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.SOMMERFERIE)) },
                    "avslagBegrunnelser" to { it.copy(avslagBegrunnelser = listOf(NasjonalEllerFellesBegrunnelse.AVSLAG_FULLTIDSPLASS_I_BARNEHAGE)) },
                    "antallTimer" to { it.copy(antallTimer = BigDecimal("20")) },
                    "søkerHarMeldtFraOmBarnehageplass" to { it.copy(søkerHarMeldtFraOmBarnehageplass = true) },
                )
            return endringer.map { (navn, endring) -> Arguments.of(named(navn, endring)) }
        }
    }
}
