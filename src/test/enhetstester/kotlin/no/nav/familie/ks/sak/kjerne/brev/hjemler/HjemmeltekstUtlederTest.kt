package no.nav.familie.ks.sak.kjerne.brev.hjemler

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagEØSVedtaksbegrunnelse
import no.nav.familie.ks.sak.data.lagSanityBegrunnelse
import no.nav.familie.ks.sak.data.lagUtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.data.lagVedtaksbegrunnelse
import no.nav.familie.ks.sak.data.lagVilkårsvurdering
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.integrasjon.sanity.SanityService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.refusjonEøs.RefusjonEøsService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.EØSBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.NasjonalEllerFellesBegrunnelse
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Målform
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class HjemmeltekstUtlederTest {
    private val vilkårsvurderingService = mockk<VilkårsvurderingService>()
    private val personopplysningGrunnlagService = mockk<PersonopplysningGrunnlagService>()
    private val refusjonEøsService = mockk<RefusjonEøsService>()
    private val sanityService = mockk<SanityService>()
    private val hjemmeltekstUtleder: HjemmeltekstUtleder =
        HjemmeltekstUtleder(
            vilkårsvurderingService = vilkårsvurderingService,
            sanityService = sanityService,
            personopplysningGrunnlagService = personopplysningGrunnlagService,
            refusjonEøsService = refusjonEøsService,
        )

    @Test
    fun `skal returnere sorterte hjemler`() {
        // Arrange
        val søker = randomAktør()

        val behandling = lagBehandling()

        val vedtaksperioderMedBegrunnelser =
            listOf(
                lagUtvidetVedtaksperiodeMedBegrunnelser(
                    begrunnelser = listOf(NasjonalEllerFellesBegrunnelse.INNVILGET_BOSATT_I_NORGE).map { lagVedtaksbegrunnelse(it) },
                ),
                lagUtvidetVedtaksperiodeMedBegrunnelser(
                    begrunnelser = listOf(NasjonalEllerFellesBegrunnelse.INNVILGET_SATSENDRING).map { lagVedtaksbegrunnelse(it) },
                ),
            )

        every { refusjonEøsService.harRefusjonEøsPåBehandling(behandlingId = behandling.id) } returns false
        every { personopplysningGrunnlagService.hentSøkersMålform(behandlingId = behandling.id) } returns Målform.NB

        every {
            vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandlingId = behandling.id)
        } returns
            lagVilkårsvurdering(
                søkerAktør = søker,
                behandling = behandling,
                resultat = Resultat.OPPFYLT,
            )

        every {
            sanityService.hentSanityBegrunnelser()
        } returns
            listOf(
                lagSanityBegrunnelse(
                    apiNavn = NasjonalEllerFellesBegrunnelse.INNVILGET_BOSATT_I_NORGE.sanityApiNavn,
                    hjemler = listOf("11", "4", "2", "10"),
                ),
                lagSanityBegrunnelse(
                    apiNavn = NasjonalEllerFellesBegrunnelse.INNVILGET_SATSENDRING.sanityApiNavn,
                    hjemler = listOf("10"),
                ),
            )

        // Act
        val hentHjemmeltekst =
            hjemmeltekstUtleder.utledHjemmeltekst(
                behandlingId = behandling.id,
                vedtakKorrigertHjemmelSkalMedIBrev = false,
                utvidetVedtaksperioderMedBegrunnelser = vedtaksperioderMedBegrunnelser,
            )

        // Assert
        assertThat("kontantstøtteloven §§ 2, 4, 10 og 11").isEqualTo(hentHjemmeltekst)
    }

    @Test
    fun `skal ikke inkludere hjemmel 13 og 16 hvis opplysningsplikt er oppfylt`() {
        // Arrange
        val søker = randomAktør()

        val behandling = lagBehandling()

        val vedtaksperioderMedBegrunnelser =
            listOf(
                lagUtvidetVedtaksperiodeMedBegrunnelser(
                    begrunnelser = listOf(NasjonalEllerFellesBegrunnelse.INNVILGET_BOSATT_I_NORGE).map { lagVedtaksbegrunnelse(it) },
                ),
                lagUtvidetVedtaksperiodeMedBegrunnelser(
                    begrunnelser = listOf(NasjonalEllerFellesBegrunnelse.INNVILGET_SATSENDRING).map { lagVedtaksbegrunnelse(it) },
                ),
            )

        every { refusjonEøsService.harRefusjonEøsPåBehandling(behandlingId = behandling.id) } returns false
        every { personopplysningGrunnlagService.hentSøkersMålform(behandlingId = behandling.id) } returns Målform.NB

        every {
            vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandlingId = behandling.id)
        } returns
            lagVilkårsvurdering(
                søkerAktør = søker,
                behandling = behandling,
                resultat = Resultat.OPPFYLT,
            )

        every {
            sanityService.hentSanityBegrunnelser()
        } returns
            listOf(
                lagSanityBegrunnelse(
                    apiNavn = NasjonalEllerFellesBegrunnelse.INNVILGET_BOSATT_I_NORGE.sanityApiNavn,
                    hjemler = listOf("11", "4", "2", "10"),
                ),
                lagSanityBegrunnelse(
                    apiNavn = NasjonalEllerFellesBegrunnelse.INNVILGET_SATSENDRING.sanityApiNavn,
                    hjemler = listOf("10"),
                ),
            )

        // Act
        val hjemler =
            hjemmeltekstUtleder.utledHjemmeltekst(
                behandlingId = behandling.id,
                vedtakKorrigertHjemmelSkalMedIBrev = false,
                utvidetVedtaksperioderMedBegrunnelser = vedtaksperioderMedBegrunnelser,
            )

        // Assert
        assertThat(hjemler).isEqualTo("kontantstøtteloven §§ 2, 4, 10 og 11")
    }

    @Test
    fun `skal inkludere hjemmel 13 og 16 hvis opplysningsplikt ikke er oppfylt`() {
        // Arrange
        val søker = randomAktør()

        val behandling = lagBehandling()

        val vedtaksperioderMedBegrunnelser =
            listOf(
                lagUtvidetVedtaksperiodeMedBegrunnelser(
                    begrunnelser = listOf(NasjonalEllerFellesBegrunnelse.INNVILGET_BOSATT_I_NORGE).map { lagVedtaksbegrunnelse(it) },
                ),
                lagUtvidetVedtaksperiodeMedBegrunnelser(
                    begrunnelser = listOf(NasjonalEllerFellesBegrunnelse.INNVILGET_SATSENDRING).map { lagVedtaksbegrunnelse(it) },
                ),
            )

        every { refusjonEøsService.harRefusjonEøsPåBehandling(behandlingId = behandling.id) } returns false
        every { personopplysningGrunnlagService.hentSøkersMålform(behandlingId = behandling.id) } returns Målform.NB

        every {
            vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandlingId = behandling.id)
        } returns
            lagVilkårsvurdering(
                søkerAktør = søker,
                behandling = behandling,
                resultat = Resultat.IKKE_OPPFYLT,
            )

        every {
            sanityService.hentSanityBegrunnelser()
        } returns
            listOf(
                lagSanityBegrunnelse(
                    apiNavn = NasjonalEllerFellesBegrunnelse.INNVILGET_BOSATT_I_NORGE.sanityApiNavn,
                    hjemler = listOf("11", "4", "2", "10"),
                ),
                lagSanityBegrunnelse(
                    apiNavn = NasjonalEllerFellesBegrunnelse.INNVILGET_SATSENDRING.sanityApiNavn,
                    hjemler = listOf("10"),
                ),
            )

        // Act
        val hjemler =
            hjemmeltekstUtleder.utledHjemmeltekst(
                behandlingId = behandling.id,
                vedtakKorrigertHjemmelSkalMedIBrev = false,
                utvidetVedtaksperioderMedBegrunnelser = vedtaksperioderMedBegrunnelser,
            )

        // Assert
        assertThat(hjemler).isEqualTo("kontantstøtteloven §§ 2, 4, 10, 11, 13 og 16")
    }

    @Test
    fun `skal inkludere EØS-forordning 987 artikkel 60 hvis det eksisterer eøs refusjon på behandlingen`() {
        // Arrange
        val søker = randomAktør()

        val behandling = lagBehandling()

        every { refusjonEøsService.harRefusjonEøsPåBehandling(behandlingId = behandling.id) } returns true
        every { personopplysningGrunnlagService.hentSøkersMålform(behandlingId = behandling.id) } returns Målform.NB

        every {
            vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandlingId = behandling.id)
        } returns
            lagVilkårsvurdering(
                søkerAktør = søker,
                behandling = behandling,
                resultat = Resultat.OPPFYLT,
            )

        every {
            sanityService.hentSanityBegrunnelser()
        } returns emptyList()

        // Act
        val hjemler =
            hjemmeltekstUtleder.utledHjemmeltekst(
                behandlingId = behandling.id,
                vedtakKorrigertHjemmelSkalMedIBrev = false,
                utvidetVedtaksperioderMedBegrunnelser = emptyList(),
            )

        // Assert
        assertThat(hjemler).isEqualTo("EØS-forordning 987/2009 artikkel 60")
    }

    @Test
    fun `skal gi riktig formattering ved hjemler fra kontantstøtteloven og 2 EØS-forordninger`() {
        // Arrange
        val søker = randomAktør()

        val behandling = lagBehandling()

        val vedtaksperioderMedBegrunnelser =
            listOf(
                lagUtvidetVedtaksperiodeMedBegrunnelser(
                    begrunnelser = listOf(NasjonalEllerFellesBegrunnelse.INNVILGET_BOSATT_I_NORGE).map { lagVedtaksbegrunnelse(it) },
                    eøsBegrunnelser = listOf(EØSBegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR).map { lagEØSVedtaksbegrunnelse(it) },
                ),
                lagUtvidetVedtaksperiodeMedBegrunnelser(
                    begrunnelser = listOf(NasjonalEllerFellesBegrunnelse.INNVILGET_SATSENDRING).map { lagVedtaksbegrunnelse(it) },
                    eøsBegrunnelser = listOf(EØSBegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE).map { lagEØSVedtaksbegrunnelse(it) },
                ),
            )

        every { refusjonEøsService.harRefusjonEøsPåBehandling(behandlingId = behandling.id) } returns false
        every { personopplysningGrunnlagService.hentSøkersMålform(behandlingId = behandling.id) } returns Målform.NB

        every {
            vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandlingId = behandling.id)
        } returns
            lagVilkårsvurdering(
                søkerAktør = søker,
                behandling = behandling,
                resultat = Resultat.OPPFYLT,
            )

        every {
            sanityService.hentSanityBegrunnelser()
        } returns
            listOf(
                lagSanityBegrunnelse(
                    apiNavn = NasjonalEllerFellesBegrunnelse.INNVILGET_BOSATT_I_NORGE.sanityApiNavn,
                    hjemler = listOf("11", "4"),
                ),
                lagSanityBegrunnelse(
                    apiNavn = NasjonalEllerFellesBegrunnelse.INNVILGET_SATSENDRING.sanityApiNavn,
                    hjemler = listOf("10"),
                ),
                lagSanityBegrunnelse(
                    apiNavn = EØSBegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR.sanityApiNavn,
                    hjemler = listOf("4"),
                    hjemlerEøsForordningen883 = listOf("11-16"),
                ),
                lagSanityBegrunnelse(
                    apiNavn = EØSBegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE.sanityApiNavn,
                    hjemler = listOf("11"),
                    hjemlerEøsForordningen987 = listOf("58", "60"),
                ),
            )

        // Act
        val hjemler =
            hjemmeltekstUtleder.utledHjemmeltekst(
                behandlingId = behandling.id,
                vedtakKorrigertHjemmelSkalMedIBrev = false,
                utvidetVedtaksperioderMedBegrunnelser = vedtaksperioderMedBegrunnelser,
            )

        // Assert
        assertThat(hjemler).isEqualTo("kontantstøtteloven §§ 4, 10 og 11, EØS-forordning 883/2004 artikkel 11-16 og EØS-forordning 987/2009 artikkel 58 og 60")
    }

    @Test
    fun `skal gi riktig formattering ved hjemler fra Separasjonsavtale og to EØS-forordninger`() {
        // Arrange
        val søker = randomAktør()

        val behandling = lagBehandling()

        val vedtaksperioderMedBegrunnelser =
            listOf(
                lagUtvidetVedtaksperiodeMedBegrunnelser(
                    begrunnelser = listOf(NasjonalEllerFellesBegrunnelse.INNVILGET_BOSATT_I_NORGE).map { lagVedtaksbegrunnelse(it) },
                    eøsBegrunnelser = listOf(EØSBegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR).map { lagEØSVedtaksbegrunnelse(it) },
                ),
                lagUtvidetVedtaksperiodeMedBegrunnelser(
                    begrunnelser = listOf(NasjonalEllerFellesBegrunnelse.INNVILGET_SATSENDRING).map { lagVedtaksbegrunnelse(it) },
                    eøsBegrunnelser = listOf(EØSBegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE).map { lagEØSVedtaksbegrunnelse(it) },
                ),
            )

        every { refusjonEøsService.harRefusjonEøsPåBehandling(behandlingId = behandling.id) } returns false
        every { personopplysningGrunnlagService.hentSøkersMålform(behandlingId = behandling.id) } returns Målform.NB

        every {
            vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandlingId = behandling.id)
        } returns
            lagVilkårsvurdering(
                søkerAktør = søker,
                behandling = behandling,
                resultat = Resultat.OPPFYLT,
            )

        every {
            sanityService.hentSanityBegrunnelser()
        } returns
            listOf(
                lagSanityBegrunnelse(
                    apiNavn = NasjonalEllerFellesBegrunnelse.INNVILGET_BOSATT_I_NORGE.sanityApiNavn,
                    hjemler = listOf("11", "4"),
                ),
                lagSanityBegrunnelse(
                    apiNavn = NasjonalEllerFellesBegrunnelse.INNVILGET_SATSENDRING.sanityApiNavn,
                    hjemler = listOf("10"),
                ),
                lagSanityBegrunnelse(
                    apiNavn = EØSBegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR.sanityApiNavn,
                    hjemler = listOf("4"),
                    hjemlerEøsForordningen883 = listOf("11-16"),
                    hjemlerSeperasjonsavtalenStorbritannina = listOf("29"),
                ),
                lagSanityBegrunnelse(
                    apiNavn = EØSBegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE.sanityApiNavn,
                    hjemler = listOf("11"),
                    hjemlerEøsForordningen987 = listOf("58", "60"),
                ),
            )

        // Act
        val hjemler =
            hjemmeltekstUtleder.utledHjemmeltekst(
                behandlingId = behandling.id,
                vedtakKorrigertHjemmelSkalMedIBrev = false,
                utvidetVedtaksperioderMedBegrunnelser = vedtaksperioderMedBegrunnelser,
            )

        // Arrange
        assertThat(hjemler).isEqualTo("Separasjonsavtalen mellom Storbritannia og Norge artikkel 29, kontantstøtteloven §§ 4, 10 og 11, EØS-forordning 883/2004 artikkel 11-16 og EØS-forordning 987/2009 artikkel 58 og 60")
    }

    @Test
    fun `skal gi riktig formattering ved nynorsk og hjemler fra Separasjonsavtale og to EØS-forordninger`() {
        // Arrange
        val søker = randomAktør()

        val behandling = lagBehandling()

        val vedtaksperioderMedBegrunnelser =
            listOf(
                lagUtvidetVedtaksperiodeMedBegrunnelser(
                    begrunnelser = listOf(NasjonalEllerFellesBegrunnelse.INNVILGET_BOSATT_I_NORGE).map { lagVedtaksbegrunnelse(it) },
                    eøsBegrunnelser = listOf(EØSBegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR).map { lagEØSVedtaksbegrunnelse(it) },
                ),
                lagUtvidetVedtaksperiodeMedBegrunnelser(
                    begrunnelser = listOf(NasjonalEllerFellesBegrunnelse.INNVILGET_SATSENDRING).map { lagVedtaksbegrunnelse(it) },
                    eøsBegrunnelser = listOf(EØSBegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE).map { lagEØSVedtaksbegrunnelse(it) },
                ),
            )

        every { refusjonEøsService.harRefusjonEøsPåBehandling(behandlingId = behandling.id) } returns false
        every { personopplysningGrunnlagService.hentSøkersMålform(behandlingId = behandling.id) } returns Målform.NN

        every {
            vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandlingId = behandling.id)
        } returns
            lagVilkårsvurdering(
                søkerAktør = søker,
                behandling = behandling,
                resultat = Resultat.OPPFYLT,
            )

        every {
            sanityService.hentSanityBegrunnelser()
        } returns
            listOf(
                lagSanityBegrunnelse(
                    apiNavn = NasjonalEllerFellesBegrunnelse.INNVILGET_BOSATT_I_NORGE.sanityApiNavn,
                    hjemler = listOf("11", "4"),
                ),
                lagSanityBegrunnelse(
                    apiNavn = NasjonalEllerFellesBegrunnelse.INNVILGET_SATSENDRING.sanityApiNavn,
                    hjemler = listOf("10"),
                ),
                lagSanityBegrunnelse(
                    apiNavn = EØSBegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR.sanityApiNavn,
                    hjemler = listOf("4"),
                    hjemlerEøsForordningen883 = listOf("11-16"),
                    hjemlerSeperasjonsavtalenStorbritannina = listOf("29"),
                ),
                lagSanityBegrunnelse(
                    apiNavn = EØSBegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE.sanityApiNavn,
                    hjemler = listOf("11"),
                    hjemlerEøsForordningen987 = listOf("58", "60"),
                ),
            )

        // Act
        val hjemler =
            hjemmeltekstUtleder.utledHjemmeltekst(
                behandlingId = behandling.id,
                vedtakKorrigertHjemmelSkalMedIBrev = false,
                utvidetVedtaksperioderMedBegrunnelser = vedtaksperioderMedBegrunnelser,
            )

        // Assert
        assertThat(hjemler).isEqualTo("Separasjonsavtalen mellom Storbritannia og Noreg artikkel 29, kontantstøttelova §§ 4, 10 og 11, EØS-forordning 883/2004 artikkel 11-16 og EØS-forordning 987/2009 artikkel 58 og 60")
    }

    @Test
    fun `skal slå sammen hjemlene riktig når det er 3 eller flere hjemler på 'siste' hjemmeltype`() {
        // Arrange
        val søker = randomAktør()

        val behandling = lagBehandling()

        val vedtaksperioderMedBegrunnelser =
            listOf(
                lagUtvidetVedtaksperiodeMedBegrunnelser(
                    begrunnelser = listOf(NasjonalEllerFellesBegrunnelse.INNVILGET_BOSATT_I_NORGE).map { lagVedtaksbegrunnelse(it) },
                    eøsBegrunnelser = listOf(EØSBegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR).map { lagEØSVedtaksbegrunnelse(it) },
                ),
                lagUtvidetVedtaksperiodeMedBegrunnelser(
                    begrunnelser = listOf(NasjonalEllerFellesBegrunnelse.INNVILGET_SATSENDRING).map { lagVedtaksbegrunnelse(it) },
                    eøsBegrunnelser = listOf(EØSBegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE).map { lagEØSVedtaksbegrunnelse(it) },
                ),
            )

        every { refusjonEøsService.harRefusjonEøsPåBehandling(behandlingId = behandling.id) } returns false
        every { personopplysningGrunnlagService.hentSøkersMålform(behandlingId = behandling.id) } returns Målform.NN

        every {
            vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandlingId = behandling.id)
        } returns
            lagVilkårsvurdering(
                søkerAktør = søker,
                behandling = behandling,
                resultat = Resultat.OPPFYLT,
            )

        every {
            sanityService.hentSanityBegrunnelser()
        } returns
            listOf(
                lagSanityBegrunnelse(
                    apiNavn = NasjonalEllerFellesBegrunnelse.INNVILGET_BOSATT_I_NORGE.sanityApiNavn,
                    hjemler = listOf("11", "4"),
                ),
                lagSanityBegrunnelse(
                    apiNavn = NasjonalEllerFellesBegrunnelse.INNVILGET_SATSENDRING.sanityApiNavn,
                    hjemler = listOf("10"),
                ),
                lagSanityBegrunnelse(
                    apiNavn = EØSBegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR.sanityApiNavn,
                    hjemler = listOf("4"),
                    hjemlerEøsForordningen883 = listOf("2", "11-16", "67", "68"),
                    hjemlerSeperasjonsavtalenStorbritannina = listOf("29"),
                ),
                lagSanityBegrunnelse(
                    apiNavn = EØSBegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE.sanityApiNavn,
                    hjemler = listOf("11"),
                ),
            )

        // Act
        val hjemler =
            hjemmeltekstUtleder.utledHjemmeltekst(
                behandlingId = behandling.id,
                vedtakKorrigertHjemmelSkalMedIBrev = false,
                utvidetVedtaksperioderMedBegrunnelser = vedtaksperioderMedBegrunnelser,
            )

        // Assert
        assertThat(hjemler).isEqualTo("Separasjonsavtalen mellom Storbritannia og Noreg artikkel 29, kontantstøttelova §§ 4, 10 og 11 og EØS-forordning 883/2004 artikkel 2, 11-16, 67 og 68")
    }

    @Test
    fun `skal kun ta med en hjemmel 1 gang hvis flere begrunnelser er knyttet til samme hjemmel`() {
        // Arrange
        val søker = randomAktør()

        val behandling = lagBehandling()

        val vedtaksperioderMedBegrunnelser =
            listOf(
                lagUtvidetVedtaksperiodeMedBegrunnelser(
                    begrunnelser = listOf(NasjonalEllerFellesBegrunnelse.INNVILGET_BOSATT_I_NORGE).map { lagVedtaksbegrunnelse(it) },
                    eøsBegrunnelser = listOf(EØSBegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR).map { lagEØSVedtaksbegrunnelse(it) },
                ),
                lagUtvidetVedtaksperiodeMedBegrunnelser(
                    begrunnelser = listOf(NasjonalEllerFellesBegrunnelse.INNVILGET_SATSENDRING).map { lagVedtaksbegrunnelse(it) },
                    eøsBegrunnelser = listOf(EØSBegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE).map { lagEØSVedtaksbegrunnelse(it) },
                ),
            )

        every { refusjonEøsService.harRefusjonEøsPåBehandling(behandlingId = behandling.id) } returns false
        every { personopplysningGrunnlagService.hentSøkersMålform(behandlingId = behandling.id) } returns Målform.NN

        every {
            vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandlingId = behandling.id)
        } returns
            lagVilkårsvurdering(
                søkerAktør = søker,
                behandling = behandling,
                resultat = Resultat.OPPFYLT,
            )

        every {
            sanityService.hentSanityBegrunnelser()
        } returns
            listOf(
                lagSanityBegrunnelse(
                    apiNavn = NasjonalEllerFellesBegrunnelse.INNVILGET_BOSATT_I_NORGE.sanityApiNavn,
                    hjemler = listOf("11", "4"),
                ),
                lagSanityBegrunnelse(
                    apiNavn = NasjonalEllerFellesBegrunnelse.INNVILGET_SATSENDRING.sanityApiNavn,
                    hjemler = listOf("10"),
                ),
                lagSanityBegrunnelse(
                    apiNavn = EØSBegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR.sanityApiNavn,
                    hjemler = listOf("4"),
                    hjemlerEøsForordningen883 = listOf("2", "11-16", "67", "68"),
                    hjemlerSeperasjonsavtalenStorbritannina = listOf("29"),
                    hjemlerEøsForordningen987 = listOf("58"),
                ),
                lagSanityBegrunnelse(
                    apiNavn = EØSBegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE.sanityApiNavn,
                    hjemler = listOf("11"),
                    hjemlerEøsForordningen883 = listOf("2", "67", "68"),
                    hjemlerSeperasjonsavtalenStorbritannina = listOf("29"),
                    hjemlerEøsForordningen987 = listOf("58"),
                ),
            )

        // Act
        val hjemler =
            hjemmeltekstUtleder.utledHjemmeltekst(
                behandlingId = behandling.id,
                vedtakKorrigertHjemmelSkalMedIBrev = false,
                utvidetVedtaksperioderMedBegrunnelser = vedtaksperioderMedBegrunnelser,
            )

        // Assert
        assertThat(hjemler).isEqualTo("Separasjonsavtalen mellom Storbritannia og Noreg artikkel 29, kontantstøttelova §§ 4, 10 og 11, EØS-forordning 883/2004 artikkel 2, 11-16, 67 og 68 og EØS-forordning 987/2009 artikkel 58")
    }

    @Test
    fun `skal utlede hjemmeltekst for alle hjemler på bokmål`() {
        // Arrange
        val søker = randomAktør()

        val behandling = lagBehandling()

        val vedtaksperioderMedBegrunnelser =
            listOf(
                lagUtvidetVedtaksperiodeMedBegrunnelser(
                    begrunnelser = listOf(NasjonalEllerFellesBegrunnelse.INNVILGET_BOSATT_I_NORGE).map { lagVedtaksbegrunnelse(it) },
                    eøsBegrunnelser = listOf(EØSBegrunnelse.INNVILGET_PRIMÆRLAND_STANDARD).map { lagEØSVedtaksbegrunnelse(it) },
                ),
            )

        every { refusjonEøsService.harRefusjonEøsPåBehandling(behandlingId = behandling.id) } returns true
        every { personopplysningGrunnlagService.hentSøkersMålform(behandlingId = behandling.id) } returns Målform.NB

        every {
            vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandlingId = behandling.id)
        } returns
            lagVilkårsvurdering(
                søkerAktør = søker,
                behandling = behandling,
                resultat = Resultat.OPPFYLT,
            )

        every {
            sanityService.hentSanityBegrunnelser()
        } returns
            listOf(
                lagSanityBegrunnelse(
                    apiNavn = NasjonalEllerFellesBegrunnelse.INNVILGET_BOSATT_I_NORGE.sanityApiNavn,
                    hjemler = listOf("11", "4", "2", "10"),
                ),
                lagSanityBegrunnelse(
                    apiNavn = EØSBegrunnelse.INNVILGET_PRIMÆRLAND_STANDARD.sanityApiNavn,
                    hjemlerSeperasjonsavtalenStorbritannina = listOf("29"),
                    hjemler = listOf("1"),
                    hjemlerEøsForordningen883 = listOf("2", "11-16", "67", "68"),
                    hjemlerEøsForordningen987 = listOf("58"),
                ),
            )

        // Act
        val hjemler =
            hjemmeltekstUtleder.utledHjemmeltekst(
                behandlingId = behandling.id,
                vedtakKorrigertHjemmelSkalMedIBrev = true,
                utvidetVedtaksperioderMedBegrunnelser = vedtaksperioderMedBegrunnelser,
            )

        // Assert
        assertThat(hjemler).isEqualTo(
            "Separasjonsavtalen mellom Storbritannia og Norge artikkel 29, " +
                "kontantstøtteloven §§ 1, 2, 4, 10 og 11, " +
                "EØS-forordning 883/2004 artikkel 2, 11-16, 67 og 68, " +
                "EØS-forordning 987/2009 artikkel 58 og 60 og forvaltningsloven § 35",
        )
    }

    @Test
    fun `skal utlede hjemmeltekst for alle hjemler på nynorsk`() {
        // Arrange
        val søker = randomAktør()

        val behandling = lagBehandling()

        val vedtaksperioderMedBegrunnelser =
            listOf(
                lagUtvidetVedtaksperiodeMedBegrunnelser(
                    begrunnelser = listOf(NasjonalEllerFellesBegrunnelse.INNVILGET_BOSATT_I_NORGE).map { lagVedtaksbegrunnelse(it) },
                    eøsBegrunnelser = listOf(EØSBegrunnelse.INNVILGET_PRIMÆRLAND_STANDARD).map { lagEØSVedtaksbegrunnelse(it) },
                ),
            )

        every { refusjonEøsService.harRefusjonEøsPåBehandling(behandlingId = behandling.id) } returns true
        every { personopplysningGrunnlagService.hentSøkersMålform(behandlingId = behandling.id) } returns Målform.NN

        every {
            vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandlingId = behandling.id)
        } returns
            lagVilkårsvurdering(
                søkerAktør = søker,
                behandling = behandling,
                resultat = Resultat.OPPFYLT,
            )

        every {
            sanityService.hentSanityBegrunnelser()
        } returns
            listOf(
                lagSanityBegrunnelse(
                    apiNavn = NasjonalEllerFellesBegrunnelse.INNVILGET_BOSATT_I_NORGE.sanityApiNavn,
                    hjemler = listOf("11", "4", "2", "10"),
                ),
                lagSanityBegrunnelse(
                    apiNavn = EØSBegrunnelse.INNVILGET_PRIMÆRLAND_STANDARD.sanityApiNavn,
                    hjemlerSeperasjonsavtalenStorbritannina = listOf("29"),
                    hjemler = listOf("1"),
                    hjemlerEøsForordningen883 = listOf("2", "11-16", "67", "68"),
                    hjemlerEøsForordningen987 = listOf("58"),
                ),
            )

        // Act
        val hjemler =
            hjemmeltekstUtleder.utledHjemmeltekst(
                behandlingId = behandling.id,
                vedtakKorrigertHjemmelSkalMedIBrev = true,
                utvidetVedtaksperioderMedBegrunnelser = vedtaksperioderMedBegrunnelser,
            )

        // Assert
        assertThat(hjemler).isEqualTo(
            "Separasjonsavtalen mellom Storbritannia og Noreg artikkel 29, " +
                "kontantstøttelova §§ 1, 2, 4, 10 og 11, " +
                "EØS-forordning 883/2004 artikkel 2, 11-16, 67 og 68, " +
                "EØS-forordning 987/2009 artikkel 58 og 60 og forvaltningslova § 35",
        )
    }

    @Test
    fun `skal kaste exception om ingen hjemmeltekst blir utledet`() {
        // Arrange
        val søker = randomAktør()

        val behandling = lagBehandling()

        every { refusjonEøsService.harRefusjonEøsPåBehandling(behandlingId = behandling.id) } returns false
        every { personopplysningGrunnlagService.hentSøkersMålform(behandlingId = behandling.id) } returns Målform.NB

        every {
            vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandlingId = behandling.id)
        } returns
            lagVilkårsvurdering(
                søkerAktør = søker,
                behandling = behandling,
                resultat = Resultat.OPPFYLT,
            )

        every { sanityService.hentSanityBegrunnelser() } returns emptyList()

        // Act & assert
        val exception =
            assertThrows<FunksjonellFeil> {
                hjemmeltekstUtleder.utledHjemmeltekst(
                    behandlingId = behandling.id,
                    vedtakKorrigertHjemmelSkalMedIBrev = false,
                    utvidetVedtaksperioderMedBegrunnelser = emptyList(),
                )
            }
        assertThat(exception.message).isEqualTo(
            "Ingen hjemler var knyttet til begrunnelsen(e) som er valgt. " +
                "Du må velge minst én begrunnelse som er knyttet til en hjemmel.",
        )
    }

    @Test
    fun `skal utlede hjemler med overgangsordning`() {
        // Arrange
        val søker = randomAktør()

        val behandling = lagBehandling()

        val vedtaksperioderMedBegrunnelser =
            listOf(
                lagUtvidetVedtaksperiodeMedBegrunnelser(
                    begrunnelser = listOf(NasjonalEllerFellesBegrunnelse.INNVILGET_OVERGANGSORDNING).map { lagVedtaksbegrunnelse(it) },
                ),
                lagUtvidetVedtaksperiodeMedBegrunnelser(
                    begrunnelser = listOf(NasjonalEllerFellesBegrunnelse.INNVILGET_SATSENDRING).map { lagVedtaksbegrunnelse(it) },
                ),
            )

        every { refusjonEøsService.harRefusjonEøsPåBehandling(behandlingId = behandling.id) } returns false
        every { personopplysningGrunnlagService.hentSøkersMålform(behandlingId = behandling.id) } returns Målform.NB

        every {
            vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandlingId = behandling.id)
        } returns
            lagVilkårsvurdering(
                søkerAktør = søker,
                behandling = behandling,
                resultat = Resultat.OPPFYLT,
            )

        every {
            sanityService.hentSanityBegrunnelser()
        } returns
            listOf(
                lagSanityBegrunnelse(
                    apiNavn = NasjonalEllerFellesBegrunnelse.INNVILGET_OVERGANGSORDNING.sanityApiNavn,
                    hjemler = emptyList(),
                ),
                lagSanityBegrunnelse(
                    apiNavn = NasjonalEllerFellesBegrunnelse.INNVILGET_SATSENDRING.sanityApiNavn,
                    hjemler = listOf("10"),
                ),
            )

        // Act
        val hentHjemmeltekst =
            hjemmeltekstUtleder.utledHjemmeltekst(
                behandlingId = behandling.id,
                vedtakKorrigertHjemmelSkalMedIBrev = false,
                utvidetVedtaksperioderMedBegrunnelser = vedtaksperioderMedBegrunnelser,
            )

        // Assert
        assertThat("kontantstøtteloven § 10 og forskrift om overgangsregler").isEqualTo(hentHjemmeltekst)
    }
}
