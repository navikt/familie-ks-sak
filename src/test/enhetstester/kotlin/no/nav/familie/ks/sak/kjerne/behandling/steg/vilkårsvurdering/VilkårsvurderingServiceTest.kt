package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.data.fnrTilFødselsdato
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.lagVilkårsvurderingMedSøkersVilkår
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.integrasjon.sanity.SanityService
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelseType
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityResultat
import no.nav.familie.ks.sak.kjerne.adopsjon.AdopsjonService
import no.nav.familie.ks.sak.kjerne.adopsjon.AdopsjonValidator
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.BegrunnelseType
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.NasjonalEllerFellesBegrunnelse
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.hamcrest.CoreMatchers.`is` as Is

class VilkårsvurderingServiceTest {
    private val vilkårsvurderingRepository: VilkårsvurderingRepository = mockk()
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService = mockk()
    private val sanityService: SanityService = mockk()
    private val personidentService: PersonidentService = mockk()
    private val adopsjonService: AdopsjonService = mockk()
    private val adopsjonValidator: AdopsjonValidator = mockk()
    private val vilkårsvurderingService =
        VilkårsvurderingService(
            vilkårsvurderingRepository,
            personopplysningGrunnlagService,
            sanityService,
            personidentService,
            adopsjonService,
            adopsjonValidator,
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
        val barn1 = randomAktør()
        val barn2 = randomAktør()

        val lagretVilkårsvurderingSlot = slot<Vilkårsvurdering>()
        every { vilkårsvurderingRepository.finnAktivForBehandling(any()) } returns null
        every { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(any()) } returns
            lagPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = søker.aktivFødselsnummer(),
                barnasIdenter = listOf(barn1.aktivFødselsnummer(), barn2.aktivFødselsnummer()),
            )
        every { vilkårsvurderingRepository.save(capture(lagretVilkårsvurderingSlot)) } returns mockk()

        vilkårsvurderingService.opprettVilkårsvurdering(behandling, null)

        val lagretVilkårsvurdering = lagretVilkårsvurderingSlot.captured

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
        val barn1 = randomAktør()
        val barn2 = randomAktør()
        val lagretDeaktivertVilkårsvurderingSlot = slot<Vilkårsvurdering>()
        val lagretVilkårsvurderingSlot = slot<Vilkårsvurdering>()
        every { vilkårsvurderingRepository.finnAktivForBehandling(any()) } returns
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

        assertEquals(3, lagretVilkårsvurdering.personResultater.size)
        assertFalse(lagretDeaktivertVilkårsvurdering.aktiv)
    }

    @Test
    fun `hentVilkårsbegrunnelser - skal returnere et map med begrunnelsestyper mappet mot liste av begrunnelser`() {
        every { sanityService.hentSanityBegrunnelser() } returns
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
        assertEquals(9, vilkårsbegrunnelser.size)
        assertEquals(0, vilkårsbegrunnelser[BegrunnelseType.AVSLAG]?.size)
    }

    @Test
    fun `hentAktivVilkårsvurderingForBehandling - skal kaste feil hvis aktiv vilkårsvurdering for behandling ikke eksisterer`() {
        every { vilkårsvurderingRepository.finnAktivForBehandling(404) } returns null

        val feil =
            assertThrows<Feil> {
                vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(404)
            }

        assertThat(feil.message, Is("Fant ikke vilkårsvurdering knyttet til behandling=404"))
    }

    @Test
    fun `hentAktivVilkårsvurderingForBehandling - skal returnere aktiv vilkårsvurdering for behandling`() {
        val mocketVilkårsvurdering = mockk<Vilkårsvurdering>()

        every { vilkårsvurderingRepository.finnAktivForBehandling(404) } returns mocketVilkårsvurdering

        val hentetVilkårsvurdering = vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(404)

        assertThat(mocketVilkårsvurdering, Is(hentetVilkårsvurdering))
    }

    @Test
    fun `slettVilkårPåBehandling - skal kaste feil dersom vilkåret som forsøkes å slettes ikke finnes`() {
        val vilkårsvurdering =
            lagVilkårsvurderingMedSøkersVilkår(
                søkerAktør = søker,
                behandling = behandling,
                resultat = Resultat.OPPFYLT,
            )

        every { personidentService.hentAktør(any()) } returns søker
        every { vilkårsvurderingRepository.finnAktivForBehandling(200) } returns vilkårsvurdering

        val feil =
            assertThrows<Feil> {
                vilkårsvurderingService.slettVilkårPåBehandling(200, 404, søker)
            }

        assertThat(feil.message, Is("Prøver å slette et vilkår som ikke finnes"))
        assertThat(feil.frontendFeilmelding, Is("Vilkåret du prøver å slette finnes ikke i systemet."))
    }
}
