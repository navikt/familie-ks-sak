package no.nav.familie.ks.sak.kjerne.eøs.vilkårsvurdering

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.tidslinje.tilTidslinje
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.lagVilkårsvurderingMedSøkersVilkår
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.ks.sak.kjerne.fagsak.domene.Fagsak
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlagRepository
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import org.hamcrest.CoreMatchers.`is` as Is

internal class VilkårsvurderingTidslinjeServiceTest {
    val personopplysningGrunnlagRepository = mockk<PersonopplysningGrunnlagRepository>()
    val vilkårsvurderingService = mockk<VilkårsvurderingService>()
    val vilkårsvurderingRepository = mockk<VilkårsvurderingRepository>()

    private lateinit var vilkårsvurderingTidslinjeService: VilkårsvurderingTidslinjeService

    @BeforeEach
    fun setUp() {
        vilkårsvurderingTidslinjeService =
            VilkårsvurderingTidslinjeService(
                vilkårsvurderingRepository = vilkårsvurderingRepository,
                personopplysningGrunnlagRepository = personopplysningGrunnlagRepository,
                vilkårsvurderingService = vilkårsvurderingService,
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
}
