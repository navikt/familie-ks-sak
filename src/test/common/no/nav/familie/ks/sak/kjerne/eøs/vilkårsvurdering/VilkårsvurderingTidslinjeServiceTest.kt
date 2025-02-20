package no.nav.familie.ks.sak.kjerne.eøs.vilkårsvurdering

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.lagVilkårsvurderingMedSøkersVilkår
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.adopsjon.AdopsjonService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.brev.lagPersonResultat
import no.nav.familie.ks.sak.kjerne.fagsak.domene.Fagsak
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlagRepository
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.tilTidslinje
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import org.hamcrest.CoreMatchers.`is` as Is

internal class VilkårsvurderingTidslinjeServiceTest {
    val personopplysningGrunnlagRepository = mockk<PersonopplysningGrunnlagRepository>()
    val vilkårsvurderingService = mockk<VilkårsvurderingService>()
    val adopsjonService = mockk<AdopsjonService>()

    private lateinit var vilkårsvurderingTidslinjeService: VilkårsvurderingTidslinjeService

    @BeforeEach
    fun setUp() {
        vilkårsvurderingTidslinjeService =
            VilkårsvurderingTidslinjeService(
                personopplysningGrunnlagRepository = personopplysningGrunnlagRepository,
                vilkårsvurderingService = vilkårsvurderingService,
                adopsjonService = adopsjonService,
            )
    }

    @Test
    fun `skal forskyve fom med 1 mnd for periode med erAnnenForelderOmfattetAvNorskLovgivning`() {
        val søker = lagPerson(personType = PersonType.SØKER, aktør = randomAktør())
        val barn = lagPerson(personType = PersonType.BARN, aktør = randomAktør())
        val fagsak = Fagsak(aktør = søker.aktør)
        val behandling = lagBehandling(fagsak = fagsak, kategori = BehandlingKategori.EØS)

        val vilkårsvurdering =
            lagVilkårsvurderingMedSøkersVilkår(
                søkerAktør = søker.aktør,
                behandling = behandling,
                resultat = Resultat.OPPFYLT,
                søkerPeriodeFom = LocalDate.of(2023, 1, 2),
                søkerPeriodeTom = LocalDate.of(2023, 3, 4),
                utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING),
            )

        every { personopplysningGrunnlagRepository.hentByBehandlingAndAktiv(behandlingId = behandling.id) } returns
            lagPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = søker.aktør.aktivFødselsnummer(),
                barnasIdenter = listOf(barn.aktør.aktivFødselsnummer()),
                søkerAktør = søker.aktør,
            )

        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandlingId = behandling.id) } returns vilkårsvurdering

        val faktiskTidslinje =
            vilkårsvurderingTidslinjeService.hentAnnenForelderOmfattetAvNorskLovgivningTidslinje(
                behandlingId = behandling.id,
            )

        val forventetTidslinje =
            listOf(
                Periode(
                    verdi = true,
                    fom = LocalDate.of(2023, 2, 1),
                    tom = LocalDate.of(2023, 3, 31),
                ),
            ).tilTidslinje()

        assertThat(faktiskTidslinje, Is(forventetTidslinje))
    }

    @Test
    fun `skal ikke gi overlapp feil dersom tom i forrige periode og fom i neste periode er i samme måned`() {
        val søker = lagPerson(personType = PersonType.SØKER, aktør = randomAktør())
        val barn = lagPerson(personType = PersonType.BARN, aktør = randomAktør())
        val fagsak = Fagsak(aktør = søker.aktør)
        val behandling = lagBehandling(fagsak = fagsak, kategori = BehandlingKategori.EØS)

        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)

        val personResultat =
            lagPersonResultat(
                søker,
                overstyrendeVilkårResultater =
                    listOf(
                        VilkårResultat(
                            personResultat = null,
                            vilkårType = Vilkår.BOSATT_I_RIKET,
                            resultat = Resultat.OPPFYLT,
                            periodeFom = LocalDate.of(2023, 1, 2),
                            periodeTom = LocalDate.of(2023, 3, 4),
                            begrunnelse = "",
                            behandlingId = behandling.id,
                            vurderesEtter = Regelverk.EØS_FORORDNINGEN,
                            utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING),
                        ),
                        VilkårResultat(
                            personResultat = null,
                            vilkårType = Vilkår.BOSATT_I_RIKET,
                            resultat = Resultat.OPPFYLT,
                            periodeFom = LocalDate.of(2023, 3, 5),
                            periodeTom = LocalDate.of(2025, 5, 5),
                            begrunnelse = "",
                            behandlingId = behandling.id,
                            vurderesEtter = Regelverk.EØS_FORORDNINGEN,
                            utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING),
                        ),
                    ),
            )

        vilkårsvurdering.personResultater = setOf(personResultat)

        every { personopplysningGrunnlagRepository.hentByBehandlingAndAktiv(behandlingId = behandling.id) } returns
            lagPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = søker.aktør.aktivFødselsnummer(),
                barnasIdenter = listOf(barn.aktør.aktivFødselsnummer()),
                søkerAktør = søker.aktør,
            )

        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandlingId = behandling.id) } returns vilkårsvurdering

        val faktiskTidslinje =
            vilkårsvurderingTidslinjeService.hentAnnenForelderOmfattetAvNorskLovgivningTidslinje(
                behandlingId = behandling.id,
            )

        val forventetTidslinje =
            listOf(
                Periode(
                    verdi = true,
                    fom = LocalDate.of(2023, 2, 1),
                    tom = LocalDate.of(2025, 5, 31),
                ),
            ).tilTidslinje()

        assertThat(faktiskTidslinje, Is(forventetTidslinje))
    }

    @Test
    fun `skal ikke gi noen oppfylte perioder hvis vilkår kun oppfylt innenfor én måned`() {
        val søker = lagPerson(personType = PersonType.SØKER, aktør = randomAktør())
        val barn = lagPerson(personType = PersonType.BARN, aktør = randomAktør())
        val fagsak = Fagsak(aktør = søker.aktør)
        val behandling = lagBehandling(fagsak = fagsak, kategori = BehandlingKategori.EØS)

        val vilkårsvurdering =
            lagVilkårsvurderingMedSøkersVilkår(
                søkerAktør = søker.aktør,
                behandling = behandling,
                resultat = Resultat.OPPFYLT,
                søkerPeriodeFom = LocalDate.of(2023, 3, 1),
                søkerPeriodeTom = LocalDate.of(2023, 3, 25),
                utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING),
            )

        every { personopplysningGrunnlagRepository.hentByBehandlingAndAktiv(behandlingId = behandling.id) } returns
            lagPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = søker.aktør.aktivFødselsnummer(),
                barnasIdenter = listOf(barn.aktør.aktivFødselsnummer()),
                søkerAktør = søker.aktør,
            )

        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandlingId = behandling.id) } returns vilkårsvurdering

        val faktiskTidslinje =
            vilkårsvurderingTidslinjeService.hentAnnenForelderOmfattetAvNorskLovgivningTidslinje(
                behandlingId = behandling.id,
            )

        assertThat(faktiskTidslinje.innhold, Is(emptyList()))
    }

    @Test
    fun `skal gi false i perioder som er gyldige men som ikke har utdypende vilkårsvurdering ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING`() {
        val søker = lagPerson(personType = PersonType.SØKER, aktør = randomAktør())
        val barn = lagPerson(personType = PersonType.BARN, aktør = randomAktør())
        val fagsak = Fagsak(aktør = søker.aktør)
        val behandling = lagBehandling(fagsak = fagsak, kategori = BehandlingKategori.EØS)

        val vilkårsvurdering =
            lagVilkårsvurderingMedSøkersVilkår(
                søkerAktør = søker.aktør,
                behandling = behandling,
                resultat = Resultat.OPPFYLT,
                søkerPeriodeFom = LocalDate.of(2023, 1, 2),
                søkerPeriodeTom = LocalDate.of(2023, 3, 4),
            )

        every { personopplysningGrunnlagRepository.hentByBehandlingAndAktiv(behandlingId = behandling.id) } returns
            lagPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = søker.aktør.aktivFødselsnummer(),
                barnasIdenter = listOf(barn.aktør.aktivFødselsnummer()),
                søkerAktør = søker.aktør,
            )

        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandlingId = behandling.id) } returns vilkårsvurdering

        val faktiskTidslinje =
            vilkårsvurderingTidslinjeService.hentAnnenForelderOmfattetAvNorskLovgivningTidslinje(
                behandlingId = behandling.id,
            )

        val forventetTidslinje =
            listOf(
                Periode(
                    verdi = false,
                    fom = LocalDate.of(2023, 2, 1),
                    tom = LocalDate.of(2023, 3, 31),
                ),
            ).tilTidslinje()

        assertThat(faktiskTidslinje, Is(forventetTidslinje))
    }
}
