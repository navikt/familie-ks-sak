package no.nav.familie.ks.sak.kjerne.brev.begrunnelser

import io.mockk.mockk
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagKompetanse
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.lagPersonResultatFraVilkårResultater
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.lagUtbetalingsperiodeDetalj
import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.integrasjon.sanity.domene.BarnetsBostedsland
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelseType
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityResultat
import no.nav.familie.ks.sak.integrasjon.sanity.domene.Trigger
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.tilForskjøvetOppfylteVilkårResultatTidslinjeMap
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.KompetanseAktivitet
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.UtfyltKompetanse
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.tilIKompetanse
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class BegrunnelserForPeriodeContextTest {
    private fun Int.jan(år: Int) = LocalDate.of(år, 1, this)

    private fun Int.feb(år: Int) = LocalDate.of(år, 2, this)

    private fun jan(år: Int) = YearMonth.of(år, 1)

    private fun feb(år: Int) = YearMonth.of(år, 2)

    private val barn = lagPerson(personType = PersonType.BARN, aktør = randomAktør(), fødselsdato = LocalDate.of(2022, 3, 10))
    private val søker = lagPerson(personType = PersonType.SØKER, aktør = randomAktør())
    private val barnAktør = barn.aktør
    private val søkerAktør = søker.aktør
    private val personopplysningGrunnlag =
        lagPersonopplysningGrunnlag(
            barnAktør = listOf(barnAktør),
            barnasFødselsdatoer = listOf(barn.fødselsdato),
            søkerPersonIdent = søkerAktør.aktivFødselsnummer(),
            søkerAktør = søkerAktør,
        )
    private val vilkårOppfyltFom = LocalDate.now().minusMonths(8)
    private val vilkårOppfyltTom = LocalDate.now()

    @Test
    fun `hentGyldigeBegrunnelserForVedtaksperiode - skal returnere standardbegrunnelsen INNVILGET_IKKE_BARNEHAGE når antall timer barnehage er 0 eller mer enn 33 og barnet ikke er adoptert`() {
        val personResultatBarn =
            PersonResultat(
                aktør = barnAktør,
                vilkårsvurdering = mockk(),
                vilkårResultater =
                    lagVilkårResultaterForVilkårTyper(
                        vilkårTyper = Vilkår.hentVilkårFor(PersonType.BARN),
                        fom = vilkårOppfyltFom,
                        tom = vilkårOppfyltTom,
                    ),
            )
        val personResultatSøker =
            PersonResultat(
                aktør = søkerAktør,
                vilkårsvurdering = mockk(),
                vilkårResultater =
                    lagVilkårResultaterForVilkårTyper(
                        vilkårTyper = Vilkår.hentVilkårFor(PersonType.SØKER),
                        fom = vilkårOppfyltFom,
                        tom = vilkårOppfyltTom,
                    ),
            )
        val personResultater =
            listOf(
                personResultatBarn,
                personResultatSøker,
            )

        val begrunnelser =
            lagFinnGyldigeBegrunnelserForPeriodeContext(
                personResultater,
                lagSanitybegrunnelser(),
                barnAktør,
            ).hentGyldigeBegrunnelserForVedtaksperiode()

        assertEquals(1, begrunnelser.size)
        assertEquals(NasjonalEllerFellesBegrunnelse.INNVILGET_IKKE_BARNEHAGE, begrunnelser.first())
    }

    @Test
    fun `hentGyldigeBegrunnelserForVedtaksperiode - skal returnere standardbegrunnelsen INNVILGET_IKKE_BARNEHAGE_ADOPSJON når antall timer barnehage er 0 eller mer enn 33 og barnet er adoptert`() {
        val personResultatBarn =
            PersonResultat(
                aktør = barnAktør,
                vilkårsvurdering = mockk(),
                vilkårResultater =
                    mutableSetOf(
                        lagVilkårResultat(
                            vilkårType = Vilkår.BARNETS_ALDER,
                            periodeFom = vilkårOppfyltFom,
                            periodeTom = vilkårOppfyltTom,
                            utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.ADOPSJON),
                        ),
                    ),
            )
        personResultatBarn.vilkårResultater.addAll(
            lagVilkårResultaterForVilkårTyper(
                setOf(
                    Vilkår.BARNEHAGEPLASS,
                    Vilkår.BOR_MED_SØKER,
                    Vilkår.MEDLEMSKAP_ANNEN_FORELDER,
                    Vilkår.BOSATT_I_RIKET,
                ),
                fom = vilkårOppfyltFom,
                tom = vilkårOppfyltTom,
            ),
        )
        val personResultatSøker =
            PersonResultat(
                aktør = søkerAktør,
                vilkårsvurdering = mockk(),
                vilkårResultater =
                    lagVilkårResultaterForVilkårTyper(
                        vilkårTyper = Vilkår.hentVilkårFor(PersonType.SØKER),
                        fom = vilkårOppfyltFom,
                        tom = vilkårOppfyltTom,
                    ),
            )
        val personResultater =
            listOf(
                personResultatBarn,
                personResultatSøker,
            )

        val begrunnelser =
            lagFinnGyldigeBegrunnelserForPeriodeContext(
                personResultater,
                lagSanitybegrunnelser(),
                barnAktør,
            ).hentGyldigeBegrunnelserForVedtaksperiode()

        assertTrue(begrunnelser.contains(NasjonalEllerFellesBegrunnelse.INNVILGET_IKKE_BARNEHAGE_ADOPSJON))
    }

    @Test
    fun `hentGyldigeBegrunnelserForVedtaksperiode - skal returnere standardbegrunnelsen INNVILGET_DELTID_BARNEHAGE når antall timer barnehage er større enn 0 og mindre enn 33 og barnet ikke er adoptert`() {
        val personResultatBarn =
            PersonResultat(
                aktør = barnAktør,
                vilkårsvurdering = mockk(),
                vilkårResultater =
                    mutableSetOf(
                        lagVilkårResultat(
                            vilkårType = Vilkår.BARNEHAGEPLASS,
                            periodeFom = vilkårOppfyltFom,
                            periodeTom = vilkårOppfyltTom,
                            antallTimer = BigDecimal.valueOf(8),
                        ),
                    ),
            )
        personResultatBarn.vilkårResultater.addAll(
            lagVilkårResultaterForVilkårTyper(
                setOf(
                    Vilkår.BARNETS_ALDER,
                    Vilkår.BOR_MED_SØKER,
                    Vilkår.MEDLEMSKAP_ANNEN_FORELDER,
                    Vilkår.BOSATT_I_RIKET,
                ),
                fom = vilkårOppfyltFom,
                tom = vilkårOppfyltTom,
            ),
        )
        val personResultatSøker =
            PersonResultat(
                aktør = søkerAktør,
                vilkårsvurdering = mockk(),
                vilkårResultater =
                    lagVilkårResultaterForVilkårTyper(
                        vilkårTyper = Vilkår.hentVilkårFor(PersonType.SØKER),
                        fom = vilkårOppfyltFom,
                        tom = vilkårOppfyltTom,
                    ),
            )
        val personResultater =
            listOf(
                personResultatBarn,
                personResultatSøker,
            )

        val begrunnelser =
            lagFinnGyldigeBegrunnelserForPeriodeContext(
                personResultater,
                lagSanitybegrunnelser(),
                barnAktør,
            ).hentGyldigeBegrunnelserForVedtaksperiode()

        assertEquals(1, begrunnelser.size)
        assertEquals(NasjonalEllerFellesBegrunnelse.INNVILGET_DELTID_BARNEHAGE, begrunnelser.first())
    }

    @Test
    fun `hentGyldigeBegrunnelserForVedtaksperiode - skal returnere standardbegrunnelsen INNVILGET_DELTID_BARNEHAGE_ADOPSJON når antall timer barnehage er større enn 0 og mindre enn 33 og barnet er adoptert`() {
        val personResultatBarn =
            PersonResultat(
                aktør = barnAktør,
                vilkårsvurdering = mockk(),
                vilkårResultater =
                    mutableSetOf(
                        lagVilkårResultat(
                            vilkårType = Vilkår.BARNEHAGEPLASS,
                            periodeFom = vilkårOppfyltFom,
                            periodeTom = vilkårOppfyltTom,
                            antallTimer = BigDecimal.valueOf(8),
                        ),
                        lagVilkårResultat(
                            vilkårType = Vilkår.BARNETS_ALDER,
                            periodeFom = vilkårOppfyltFom,
                            periodeTom = vilkårOppfyltTom,
                            utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.ADOPSJON),
                        ),
                    ),
            )
        personResultatBarn.vilkårResultater.addAll(
            lagVilkårResultaterForVilkårTyper(
                setOf(
                    Vilkår.BOR_MED_SØKER,
                    Vilkår.MEDLEMSKAP_ANNEN_FORELDER,
                    Vilkår.BOSATT_I_RIKET,
                ),
                fom = vilkårOppfyltFom,
                tom = vilkårOppfyltTom,
            ),
        )
        val personResultatSøker =
            PersonResultat(
                aktør = søkerAktør,
                vilkårsvurdering = mockk(),
                vilkårResultater =
                    lagVilkårResultaterForVilkårTyper(
                        vilkårTyper = Vilkår.hentVilkårFor(PersonType.SØKER),
                        fom = vilkårOppfyltFom,
                        tom = vilkårOppfyltTom,
                    ),
            )
        val personResultater =
            listOf(
                personResultatBarn,
                personResultatSøker,
            )

        val begrunnelser =
            lagFinnGyldigeBegrunnelserForPeriodeContext(
                personResultater,
                lagSanitybegrunnelser(),
                barnAktør,
            ).hentGyldigeBegrunnelserForVedtaksperiode()

        assertTrue(begrunnelser.contains(NasjonalEllerFellesBegrunnelse.INNVILGET_DELTID_BARNEHAGE_ADOPSJON))
    }

    @Test
    fun `hentGyldigeBegrunnelserForVedtaksperiode - skal returnere 1 begrunnelse av type Standard i tillegg til INNVILGET_BOSATT_I_NORGE når vilkåret BOSATT_I_RIKET trigger vedtaksperioden`() {
        val barn1ÅrSanityNasjonalEllerFellesBegrunnelse =
            SanityBegrunnelse(
                apiNavn = NasjonalEllerFellesBegrunnelse.INNVILGET_BOSATT_I_NORGE.sanityApiNavn,
                navnISystem = "Barn 1 år",
                type = SanityBegrunnelseType.TILLEGGSTEKST,
                vilkår = listOf(Vilkår.BARNETS_ALDER),
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
            )
        val personResultatBarn =
            PersonResultat(
                aktør = barnAktør,
                vilkårsvurdering = mockk(),
                vilkårResultater =
                    lagVilkårResultaterForVilkårTyper(
                        vilkårTyper =
                            setOf(
                                Vilkår.BARNEHAGEPLASS,
                                Vilkår.BOSATT_I_RIKET,
                                Vilkår.BOR_MED_SØKER,
                                Vilkår.MEDLEMSKAP_ANNEN_FORELDER,
                            ),
                        fom = vilkårOppfyltFom,
                        tom = vilkårOppfyltTom,
                    ),
            )
        personResultatBarn.vilkårResultater.add(
            lagVilkårResultat(
                vilkårType = Vilkår.BARNETS_ALDER,
                periodeFom = vilkårOppfyltFom.plusDays(15),
                periodeTom = vilkårOppfyltTom.minusDays(15),
            ),
        )
        val personResultatSøker =
            PersonResultat(
                aktør = søkerAktør,
                vilkårsvurdering = mockk(),
                vilkårResultater =
                    lagVilkårResultaterForVilkårTyper(
                        vilkårTyper = Vilkår.hentVilkårFor(PersonType.SØKER),
                        fom = vilkårOppfyltFom,
                        tom = vilkårOppfyltTom,
                    ),
            )
        val personResultater =
            listOf(
                personResultatBarn,
                personResultatSøker,
            )

        val begrunnelser =
            lagFinnGyldigeBegrunnelserForPeriodeContext(
                personResultater,
                lagSanitybegrunnelser() + barn1ÅrSanityNasjonalEllerFellesBegrunnelse,
                barnAktør,
            ).hentGyldigeBegrunnelserForVedtaksperiode()

        assertEquals(2, begrunnelser.size)
        assertThat(
            begrunnelser,
            containsInAnyOrder(
                NasjonalEllerFellesBegrunnelse.INNVILGET_BOSATT_I_NORGE,
                NasjonalEllerFellesBegrunnelse.INNVILGET_IKKE_BARNEHAGE,
            ),
        )
    }

    @Test
    fun `hentGyldigeBegrunnelserForVedtaksperiode - skal returnere 1 begrunnelse av type Standard i tillegg til 1 tilleggstekst som skal vises når BOSATT_I_RIKET for søker trigger vedtaksperioden`() {
        val bosattIRiketBegrunnelser =
            listOf(
                SanityBegrunnelse(
                    apiNavn = NasjonalEllerFellesBegrunnelse.INNVILGET_BOR_FAST_HOS_SØKER.sanityApiNavn,
                    navnISystem = "Søker og eller barn bosatt i riket",
                    type = SanityBegrunnelseType.TILLEGGSTEKST,
                    vilkår = listOf(Vilkår.BOSATT_I_RIKET),
                    rolle = emptyList(),
                    triggere = emptyList(),
                    utdypendeVilkårsvurderinger = emptyList(),
                    hjemler = emptyList(),
                    endringsårsaker = emptyList(),
                    endretUtbetalingsperiode = emptyList(),
                    støtterFritekst = false,
                    skalAlltidVises = false,
                    ikkeIBruk = false,
                    resultat = SanityResultat.INNVILGET,
                ),
            )
        val personResultatBarn =
            PersonResultat(
                aktør = barnAktør,
                vilkårsvurdering = mockk(),
                vilkårResultater =
                    lagVilkårResultaterForVilkårTyper(
                        vilkårTyper = Vilkår.hentVilkårFor(PersonType.BARN),
                        fom = vilkårOppfyltFom,
                        tom = vilkårOppfyltTom,
                    ),
            )

        val personResultatSøker =
            PersonResultat(
                aktør = søkerAktør,
                vilkårsvurdering = mockk(),
                vilkårResultater =
                    lagVilkårResultaterForVilkårTyper(
                        vilkårTyper =
                            setOf(
                                Vilkår.MEDLEMSKAP,
                            ),
                        fom = vilkårOppfyltFom,
                        tom = vilkårOppfyltTom,
                    ),
            )

        personResultatSøker.vilkårResultater.add(
            lagVilkårResultat(
                vilkårType = Vilkår.BOSATT_I_RIKET,
                periodeFom = vilkårOppfyltFom.plusMonths(2),
                periodeTom = vilkårOppfyltTom.minusDays(15),
            ),
        )
        val personResultater =
            listOf(
                personResultatBarn,
                personResultatSøker,
            )

        val begrunnelser =
            lagFinnGyldigeBegrunnelserForPeriodeContext(
                personResultater,
                lagSanitybegrunnelser() + bosattIRiketBegrunnelser,
                søkerAktør,
            ).hentGyldigeBegrunnelserForVedtaksperiode()

        assertEquals(2, begrunnelser.size)
        assertThat(
            begrunnelser,
            containsInAnyOrder(
                NasjonalEllerFellesBegrunnelse.INNVILGET_IKKE_BARNEHAGE,
                NasjonalEllerFellesBegrunnelse.INNVILGET_BOR_FAST_HOS_SØKER,
            ),
        )
    }

    @Nested
    inner class EØS {
        @Test
        fun `Skal kunne få opp eøs som gyldige begrunnelser dersom det er en kompetanse i perioden`() {
            val eøsBegrunnelse =
                SanityBegrunnelse(
                    apiNavn = EØSBegrunnelse.INNVILGET_PRIMÆRLAND_BARNET_BOR_I_NORGE.sanityApiNavn,
                    navnISystem = EØSBegrunnelse.INNVILGET_PRIMÆRLAND_BARNET_BOR_I_NORGE.name,
                    type = SanityBegrunnelseType.STANDARD,
                    vilkår = emptyList(),
                    rolle = emptyList(),
                    triggere = emptyList(),
                    utdypendeVilkårsvurderinger = emptyList(),
                    hjemler = emptyList(),
                    endretUtbetalingsperiode = emptyList(),
                    endringsårsaker = emptyList(),
                    støtterFritekst = false,
                    skalAlltidVises = false,
                    ikkeIBruk = false,
                    annenForeldersAktivitet = listOf(KompetanseAktivitet.ARBEIDER),
                    barnetsBostedsland = listOf(BarnetsBostedsland.NORGE),
                    kompetanseResultat = listOf(KompetanseResultat.NORGE_ER_PRIMÆRLAND),
                    hjemlerFolketrygdloven = emptyList(),
                    hjemlerEØSForordningen883 = emptyList(),
                    hjemlerEØSForordningen987 = emptyList(),
                    hjemlerSeperasjonsavtalenStorbritannina = emptyList(),
                    resultat = SanityResultat.INNVILGET,
                )

            val begrunnelseContext =
                lagBegrunnelserForPeriodeContextForEøsTester(
                    sanityBegrunnelser = listOf(eøsBegrunnelse),
                    kompetanser = listOf(lagKompetanse(fom = jan(2020), tom = jan(2020), annenForeldersAktivitet = KompetanseAktivitet.ARBEIDER, resultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND, barnetsBostedsland = "NO", barnAktører = setOf(barnAktør))),
                    vedtaksperiodeStartsTidpunkt = 1.jan(2020),
                    vedtaksperiodeSluttTidpunkt = 31.jan(2020),
                    andelerTilkjentYtelse = listOf(AndelTilkjentYtelseMedEndreteUtbetalinger(lagAndelTilkjentYtelse(fom = jan(2020), tom = jan(2020), aktør = barnAktør), endreteUtbetalingerAndeler = emptyList())),
                )
            val begrunnelser =
                begrunnelseContext.hentGyldigeBegrunnelserForVedtaksperiode()

            assertThat(begrunnelser).contains(EØSBegrunnelse.INNVILGET_PRIMÆRLAND_BARNET_BOR_I_NORGE)
        }

        @Test
        fun `Kompetanser som ikke gjelder for perioden skal ikke føre til gyldige begrunnelser`() {
            val eøsBegrunnelse =
                SanityBegrunnelse(
                    apiNavn = EØSBegrunnelse.INNVILGET_PRIMÆRLAND_BARNET_BOR_I_NORGE.sanityApiNavn,
                    navnISystem = EØSBegrunnelse.INNVILGET_PRIMÆRLAND_BARNET_BOR_I_NORGE.name,
                    type = SanityBegrunnelseType.STANDARD,
                    vilkår = emptyList(),
                    rolle = emptyList(),
                    triggere = emptyList(),
                    utdypendeVilkårsvurderinger = emptyList(),
                    hjemler = emptyList(),
                    endretUtbetalingsperiode = emptyList(),
                    endringsårsaker = emptyList(),
                    støtterFritekst = false,
                    skalAlltidVises = false,
                    ikkeIBruk = false,
                    annenForeldersAktivitet = listOf(KompetanseAktivitet.ARBEIDER),
                    barnetsBostedsland = listOf(BarnetsBostedsland.NORGE),
                    kompetanseResultat = listOf(KompetanseResultat.NORGE_ER_PRIMÆRLAND),
                    hjemlerFolketrygdloven = emptyList(),
                    hjemlerEØSForordningen883 = emptyList(),
                    hjemlerEØSForordningen987 = emptyList(),
                    hjemlerSeperasjonsavtalenStorbritannina = emptyList(),
                    resultat = SanityResultat.INNVILGET,
                )

            val begrunnelseContext =
                lagBegrunnelserForPeriodeContextForEøsTester(
                    sanityBegrunnelser = listOf(eøsBegrunnelse),
                    kompetanser = listOf(lagKompetanse(fom = jan(2020), tom = jan(2020), annenForeldersAktivitet = KompetanseAktivitet.ARBEIDER, resultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND, barnetsBostedsland = "NO", barnAktører = setOf(barnAktør))),
                    vedtaksperiodeStartsTidpunkt = 1.feb(2021),
                    vedtaksperiodeSluttTidpunkt = 28.feb(2021),
                    andelerTilkjentYtelse = listOf(AndelTilkjentYtelseMedEndreteUtbetalinger(lagAndelTilkjentYtelse(fom = jan(2020), tom = feb(2020), aktør = barnAktør), endreteUtbetalingerAndeler = emptyList())),
                )
            val begrunnelser =
                begrunnelseContext.hentGyldigeBegrunnelserForVedtaksperiode()

            assertThat(begrunnelser.filter { it.begrunnelseType != BegrunnelseType.FORTSATT_INNVILGET }.size).isEqualTo(0)
        }

        @Test
        fun `Skal kunne få opp eøs-opphør som gyldige begrunnelser dersom det er en kompetanse som slutter måneden før`() {
            val eøsBegrunnelse =
                SanityBegrunnelse(
                    apiNavn = EØSBegrunnelse.OPPHØR_EØS_STANDARD.sanityApiNavn,
                    navnISystem = EØSBegrunnelse.OPPHØR_EØS_STANDARD.name,
                    type = SanityBegrunnelseType.STANDARD,
                    vilkår = emptyList(),
                    rolle = emptyList(),
                    triggere = emptyList(),
                    utdypendeVilkårsvurderinger = emptyList(),
                    hjemler = emptyList(),
                    endretUtbetalingsperiode = emptyList(),
                    endringsårsaker = emptyList(),
                    støtterFritekst = false,
                    skalAlltidVises = false,
                    ikkeIBruk = false,
                    annenForeldersAktivitet = listOf(KompetanseAktivitet.ARBEIDER),
                    barnetsBostedsland = listOf(BarnetsBostedsland.NORGE),
                    kompetanseResultat = listOf(KompetanseResultat.NORGE_ER_PRIMÆRLAND),
                    hjemlerFolketrygdloven = emptyList(),
                    hjemlerEØSForordningen883 = emptyList(),
                    hjemlerEØSForordningen987 = emptyList(),
                    hjemlerSeperasjonsavtalenStorbritannina = emptyList(),
                    resultat = SanityResultat.OPPHØR,
                )

            val begrunnelseContext =
                lagBegrunnelserForPeriodeContextForEøsTester(
                    sanityBegrunnelser = listOf(eøsBegrunnelse),
                    vedtaksperiodetype = Vedtaksperiodetype.OPPHØR,
                    kompetanser = listOf(lagKompetanse(fom = jan(2020), tom = jan(2020), annenForeldersAktivitet = KompetanseAktivitet.ARBEIDER, resultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND, barnetsBostedsland = "NO", barnAktører = setOf(barnAktør))),
                    vedtaksperiodeStartsTidpunkt = 1.feb(2020),
                    vedtaksperiodeSluttTidpunkt = 28.feb(2020),
                    andelerTilkjentYtelse = listOf(AndelTilkjentYtelseMedEndreteUtbetalinger(lagAndelTilkjentYtelse(fom = jan(2020), tom = jan(2020), aktør = barnAktør), endreteUtbetalingerAndeler = emptyList())),
                )
            val begrunnelser =
                begrunnelseContext.hentGyldigeBegrunnelserForVedtaksperiode()

            assertThat(begrunnelser.size).isEqualTo(1)
        }

        @Test
        fun `Skal ikke få opp eøs-opphør som gyldige begrunnelser dersom det er en kompetanse som slutter måneden før når vi fremdeles har kompetanse`() {
            val eøsBegrunnelse =
                SanityBegrunnelse(
                    apiNavn = EØSBegrunnelse.OPPHØR_EØS_STANDARD.sanityApiNavn,
                    navnISystem = EØSBegrunnelse.OPPHØR_EØS_STANDARD.name,
                    type = SanityBegrunnelseType.STANDARD,
                    vilkår = emptyList(),
                    rolle = emptyList(),
                    triggere = emptyList(),
                    utdypendeVilkårsvurderinger = emptyList(),
                    hjemler = emptyList(),
                    endretUtbetalingsperiode = emptyList(),
                    endringsårsaker = emptyList(),
                    støtterFritekst = false,
                    skalAlltidVises = false,
                    ikkeIBruk = false,
                    annenForeldersAktivitet = listOf(KompetanseAktivitet.ARBEIDER),
                    barnetsBostedsland = listOf(BarnetsBostedsland.NORGE),
                    kompetanseResultat = listOf(KompetanseResultat.NORGE_ER_PRIMÆRLAND),
                    hjemlerFolketrygdloven = emptyList(),
                    hjemlerEØSForordningen883 = emptyList(),
                    hjemlerEØSForordningen987 = emptyList(),
                    hjemlerSeperasjonsavtalenStorbritannina = emptyList(),
                    resultat = SanityResultat.OPPHØR,
                )

            val begrunnelseContext =
                lagBegrunnelserForPeriodeContextForEøsTester(
                    sanityBegrunnelser = listOf(eøsBegrunnelse),
                    vedtaksperiodetype = Vedtaksperiodetype.OPPHØR,
                    kompetanser =
                        listOf(
                            lagKompetanse(fom = jan(2020), tom = jan(2020), annenForeldersAktivitet = KompetanseAktivitet.ARBEIDER, resultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND, barnetsBostedsland = "NO", barnAktører = setOf(barnAktør)),
                            lagKompetanse(fom = feb(2020), tom = feb(2020), annenForeldersAktivitet = KompetanseAktivitet.ARBEIDER, resultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND, barnetsBostedsland = "NO", barnAktører = setOf(barnAktør)),
                        ),
                    vedtaksperiodeStartsTidpunkt = 1.feb(2020),
                    vedtaksperiodeSluttTidpunkt = 28.feb(2020),
                    andelerTilkjentYtelse = listOf(AndelTilkjentYtelseMedEndreteUtbetalinger(lagAndelTilkjentYtelse(fom = jan(2020), tom = feb(2020), aktør = barnAktør), endreteUtbetalingerAndeler = emptyList())),
                )
            val begrunnelser =
                begrunnelseContext.hentGyldigeBegrunnelserForVedtaksperiode()

            assertThat(begrunnelser.size).isEqualTo(0)
        }

        @Test
        fun `Skal få opp fortsatt innvilget tekster dersom det ikke er forandring i kompetanse`() {
            val eøsBegrunnelse =
                SanityBegrunnelse(
                    apiNavn = EØSBegrunnelse.FORTSATT_INNVILGET_PRIMÆRLAND_STANDARD.sanityApiNavn,
                    navnISystem = EØSBegrunnelse.FORTSATT_INNVILGET_PRIMÆRLAND_STANDARD.name,
                    type = SanityBegrunnelseType.STANDARD,
                    vilkår = emptyList(),
                    rolle = emptyList(),
                    triggere = emptyList(),
                    utdypendeVilkårsvurderinger = emptyList(),
                    hjemler = emptyList(),
                    endretUtbetalingsperiode = emptyList(),
                    endringsårsaker = emptyList(),
                    støtterFritekst = false,
                    skalAlltidVises = false,
                    ikkeIBruk = false,
                    annenForeldersAktivitet = listOf(KompetanseAktivitet.ARBEIDER),
                    barnetsBostedsland = listOf(BarnetsBostedsland.NORGE),
                    kompetanseResultat = listOf(KompetanseResultat.NORGE_ER_PRIMÆRLAND),
                    hjemlerFolketrygdloven = emptyList(),
                    hjemlerEØSForordningen883 = emptyList(),
                    hjemlerEØSForordningen987 = emptyList(),
                    hjemlerSeperasjonsavtalenStorbritannina = emptyList(),
                    resultat = SanityResultat.FORTSATT_INNVILGET,
                )

            val begrunnelseContext =
                lagBegrunnelserForPeriodeContextForEøsTester(
                    sanityBegrunnelser = listOf(eøsBegrunnelse),
                    vedtaksperiodetype = Vedtaksperiodetype.UTBETALING,
                    kompetanser =
                        listOf(
                            lagKompetanse(fom = jan(2020), tom = feb(2020), annenForeldersAktivitet = KompetanseAktivitet.ARBEIDER, resultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND, barnetsBostedsland = "NO", barnAktører = setOf(barnAktør)),
                        ),
                    vedtaksperiodeStartsTidpunkt = 1.feb(2020),
                    vedtaksperiodeSluttTidpunkt = 28.feb(2020),
                    andelerTilkjentYtelse = listOf(AndelTilkjentYtelseMedEndreteUtbetalinger(lagAndelTilkjentYtelse(fom = jan(2020), tom = feb(2020), aktør = barnAktør), endreteUtbetalingerAndeler = emptyList())),
                )
            val begrunnelser =
                begrunnelseContext.hentGyldigeBegrunnelserForVedtaksperiode()

            assertThat(begrunnelser).contains(EØSBegrunnelse.FORTSATT_INNVILGET_PRIMÆRLAND_STANDARD)
        }

        @Test
        fun `Skal kunne få opp eøs-opphør selv om kompetanse som varer evig når det ikke er noen utbetaling på barnet`() {
            val eøsBegrunnelse =
                SanityBegrunnelse(
                    apiNavn = EØSBegrunnelse.OPPHØR_EØS_STANDARD.sanityApiNavn,
                    navnISystem = EØSBegrunnelse.OPPHØR_EØS_STANDARD.name,
                    type = SanityBegrunnelseType.STANDARD,
                    vilkår = emptyList(),
                    rolle = emptyList(),
                    triggere = emptyList(),
                    utdypendeVilkårsvurderinger = emptyList(),
                    hjemler = emptyList(),
                    endretUtbetalingsperiode = emptyList(),
                    endringsårsaker = emptyList(),
                    støtterFritekst = false,
                    skalAlltidVises = false,
                    ikkeIBruk = false,
                    annenForeldersAktivitet = listOf(KompetanseAktivitet.ARBEIDER),
                    barnetsBostedsland = listOf(BarnetsBostedsland.NORGE),
                    kompetanseResultat = listOf(KompetanseResultat.NORGE_ER_PRIMÆRLAND),
                    hjemlerFolketrygdloven = emptyList(),
                    hjemlerEØSForordningen883 = emptyList(),
                    hjemlerEØSForordningen987 = emptyList(),
                    hjemlerSeperasjonsavtalenStorbritannina = emptyList(),
                    resultat = SanityResultat.OPPHØR,
                )

            val begrunnelseContext =
                lagBegrunnelserForPeriodeContextForEøsTester(
                    sanityBegrunnelser = listOf(eøsBegrunnelse),
                    vedtaksperiodetype = Vedtaksperiodetype.OPPHØR,
                    kompetanser = listOf(lagKompetanse(fom = jan(2020), tom = null, annenForeldersAktivitet = KompetanseAktivitet.ARBEIDER, resultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND, barnetsBostedsland = "NO", barnAktører = setOf(barnAktør))),
                    vedtaksperiodeStartsTidpunkt = 1.feb(2020),
                    vedtaksperiodeSluttTidpunkt = 28.feb(2020),
                    andelerTilkjentYtelse = listOf(AndelTilkjentYtelseMedEndreteUtbetalinger(lagAndelTilkjentYtelse(fom = jan(2020), tom = jan(2020), aktør = barnAktør), endreteUtbetalingerAndeler = emptyList())),
                )
            val begrunnelser =
                begrunnelseContext.hentGyldigeBegrunnelserForVedtaksperiode()

            assertThat(begrunnelser.size).isEqualTo(1)
        }
    }

    private fun lagSanitybegrunnelser(): List<SanityBegrunnelse> =
        listOf(
            SanityBegrunnelse(
                apiNavn = NasjonalEllerFellesBegrunnelse.INNVILGET_IKKE_BARNEHAGE.sanityApiNavn,
                navnISystem = "Ikke barnehage",
                type = SanityBegrunnelseType.STANDARD,
                vilkår = listOf(Vilkår.BARNEHAGEPLASS, Vilkår.BARNETS_ALDER),
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
            SanityBegrunnelse(
                apiNavn = NasjonalEllerFellesBegrunnelse.INNVILGET_IKKE_BARNEHAGE_ADOPSJON.sanityApiNavn,
                navnISystem = "Ikke barnehage - adopsjon",
                type = SanityBegrunnelseType.STANDARD,
                vilkår = listOf(Vilkår.BARNEHAGEPLASS, Vilkår.BARNETS_ALDER),
                rolle = emptyList(),
                triggere = emptyList(),
                utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.ADOPSJON),
                hjemler = emptyList(),
                endretUtbetalingsperiode = emptyList(),
                endringsårsaker = emptyList(),
                støtterFritekst = false,
                skalAlltidVises = false,
                ikkeIBruk = false,
                resultat = SanityResultat.INNVILGET,
            ),
            SanityBegrunnelse(
                apiNavn = NasjonalEllerFellesBegrunnelse.INNVILGET_DELTID_BARNEHAGE.sanityApiNavn,
                navnISystem = "Deltid barnehage",
                type = SanityBegrunnelseType.STANDARD,
                vilkår = listOf(Vilkår.BARNEHAGEPLASS, Vilkår.BARNETS_ALDER),
                rolle = emptyList(),
                triggere = listOf(Trigger.DELTID_BARNEHAGEPLASS),
                utdypendeVilkårsvurderinger = emptyList(),
                hjemler = emptyList(),
                endretUtbetalingsperiode = emptyList(),
                endringsårsaker = emptyList(),
                støtterFritekst = false,
                skalAlltidVises = false,
                ikkeIBruk = false,
                resultat = SanityResultat.INNVILGET,
            ),
            SanityBegrunnelse(
                apiNavn = NasjonalEllerFellesBegrunnelse.INNVILGET_DELTID_BARNEHAGE_ADOPSJON.sanityApiNavn,
                navnISystem = "Deltid barnehage - adopsjon",
                type = SanityBegrunnelseType.STANDARD,
                vilkår = listOf(Vilkår.BARNEHAGEPLASS, Vilkår.BARNETS_ALDER),
                rolle = emptyList(),
                triggere = listOf(Trigger.DELTID_BARNEHAGEPLASS),
                utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.ADOPSJON),
                hjemler = emptyList(),
                endretUtbetalingsperiode = emptyList(),
                endringsårsaker = emptyList(),
                støtterFritekst = false,
                skalAlltidVises = false,
                ikkeIBruk = false,
                resultat = SanityResultat.INNVILGET,
            ),
        )

    private fun lagVilkårResultaterForVilkårTyper(
        vilkårTyper: Set<Vilkår>,
        fom: LocalDate,
        tom: LocalDate,
    ): MutableSet<VilkårResultat> =
        vilkårTyper
            .map {
                lagVilkårResultat(
                    vilkårType = it,
                    periodeFom = fom,
                    periodeTom = tom,
                )
            }.toMutableSet()

    private fun lagFinnGyldigeBegrunnelserForPeriodeContext(
        personResultater: List<PersonResultat>,
        sanityBegrunnelser: List<SanityBegrunnelse>,
        aktørSomTriggerVedtaksperiode: Aktør,
        andelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger> = emptyList(),
    ): BegrunnelserForPeriodeContext {
        // Må forskyve personresultatene for å finne riktig dato for vedtaksperiode.
        val vedtaksperiodeStartsTidpunkt =
            personResultater
                .tilForskjøvetOppfylteVilkårResultatTidslinjeMap(
                    personopplysningGrunnlag = personopplysningGrunnlag,
                    adopsjonerIBehandling = emptyList(),
                ).filterKeys { it.aktørId == aktørSomTriggerVedtaksperiode.aktørId }
                .values
                .first()
                .startsTidspunkt

        val utvidetVedtaksperiodeMedBegrunnelser =
            UtvidetVedtaksperiodeMedBegrunnelser(
                id = 0,
                fom = vedtaksperiodeStartsTidpunkt,
                tom = vedtaksperiodeStartsTidpunkt.plusDays(1),
                type = Vedtaksperiodetype.UTBETALING,
                begrunnelser = emptyList(),
                utbetalingsperiodeDetaljer =
                    listOf(
                        lagUtbetalingsperiodeDetalj(person = lagPerson(aktør = søkerAktør, personType = PersonType.SØKER)),
                        lagUtbetalingsperiodeDetalj(person = lagPerson(aktør = barnAktør, personType = PersonType.BARN)),
                    ),
                støtterFritekst = false,
            )

        return BegrunnelserForPeriodeContext(
            utvidetVedtaksperiodeMedBegrunnelser = utvidetVedtaksperiodeMedBegrunnelser,
            sanityBegrunnelser = sanityBegrunnelser,
            kompetanser = emptyList(),
            personopplysningGrunnlag = personopplysningGrunnlag,
            adopsjonerIBehandling = emptyList(),
            overgangsordningAndeler = emptyList(),
            personResultater = personResultater,
            endretUtbetalingsandeler = emptyList(),
            erFørsteVedtaksperiode = false,
            andelerTilkjentYtelse = andelerTilkjentYtelse,
        )
    }

    private fun lagBegrunnelserForPeriodeContextForEøsTester(
        sanityBegrunnelser: List<SanityBegrunnelse>,
        kompetanser: List<Kompetanse>,
        vedtaksperiodeStartsTidpunkt: LocalDate? = null,
        vedtaksperiodeSluttTidpunkt: LocalDate? = null,
        vedtaksperiodetype: Vedtaksperiodetype = Vedtaksperiodetype.UTBETALING,
        andelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger> = emptyList(),
    ): BegrunnelserForPeriodeContext {
        val utvidetVedtaksperiodeMedBegrunnelser =
            UtvidetVedtaksperiodeMedBegrunnelser(
                id = 0,
                fom = vedtaksperiodeStartsTidpunkt,
                tom = vedtaksperiodeSluttTidpunkt,
                type = vedtaksperiodetype,
                begrunnelser = emptyList(),
                utbetalingsperiodeDetaljer =
                    listOf(
                        lagUtbetalingsperiodeDetalj(person = lagPerson(aktør = søkerAktør, personType = PersonType.SØKER)),
                        lagUtbetalingsperiodeDetalj(person = lagPerson(aktør = barnAktør, personType = PersonType.BARN)),
                    ),
                støtterFritekst = false,
            )

        val vilkårResultaterForSøker =
            Vilkår
                .hentVilkårFor(søker.type)
                .map {
                    lagVilkårResultat(
                        periodeFom = søker.fødselsdato,
                        periodeTom = null,
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
                        periodeFom = barn.fødselsdato.plusYears(1),
                        periodeTom = barn.fødselsdato.plusYears(2),
                        vilkårType = it,
                        resultat = Resultat.OPPFYLT,
                        begrunnelse = "",
                        utdypendeVilkårsvurderinger = emptyList(),
                    )
                }.toSet()

        return BegrunnelserForPeriodeContext(
            utvidetVedtaksperiodeMedBegrunnelser = utvidetVedtaksperiodeMedBegrunnelser,
            sanityBegrunnelser = sanityBegrunnelser,
            kompetanser = kompetanser.map { it.tilIKompetanse() }.filterIsInstance<UtfyltKompetanse>(),
            personopplysningGrunnlag = personopplysningGrunnlag,
            adopsjonerIBehandling = emptyList(),
            overgangsordningAndeler = emptyList(),
            personResultater =
                listOf(
                    lagPersonResultatFraVilkårResultater(
                        vilkårResultater = vilkårResultaterForSøker,
                        aktør = søkerAktør,
                    ),
                    lagPersonResultatFraVilkårResultater(
                        vilkårResultater = vilkårResultaterBarn,
                        aktør = barnAktør,
                    ),
                ),
            endretUtbetalingsandeler = emptyList(),
            erFørsteVedtaksperiode = false,
            andelerTilkjentYtelse = andelerTilkjentYtelse,
        )
    }
}
