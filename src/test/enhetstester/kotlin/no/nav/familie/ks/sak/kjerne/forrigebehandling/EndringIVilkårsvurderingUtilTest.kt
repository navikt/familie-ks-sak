package no.nav.familie.ks.sak.kjerne.forrigebehandling

import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIInneværendeMåned
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagVilkårsvurdering
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class EndringIVilkårsvurderingUtilTest {
    private val jan22 = YearMonth.of(2022, 1)
    private val feb22 = YearMonth.of(2022, 2)
    private val apr22 = YearMonth.of(2022, 4)
    private val mai22 = YearMonth.of(2022, 5)
    private val jun22 = YearMonth.of(2022, 6)

    @Test
    fun `Endring i vilkårsvurdering - skal ikke lage periode med endring dersom vilkårresultatene er helt like`() {
        val fødselsdato = LocalDate.of(2015, 1, 1)
        val vilkårResultater =
            setOf(
                VilkårResultat(
                    behandlingId = 0,
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = fødselsdato,
                    periodeTom = LocalDate.of(2020, 1, 1),
                    begrunnelse = "begrunnelse",
                    utdypendeVilkårsvurderinger =
                        listOf(
                            UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                        ),
                    vurderesEtter = Regelverk.NASJONALE_REGLER,
                ),
                VilkårResultat(
                    behandlingId = 0,
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.IKKE_OPPFYLT,
                    periodeFom = LocalDate.of(2020, 1, 2),
                    periodeTom = LocalDate.of(2022, 1, 1),
                    begrunnelse = "begrunnelse",
                    utdypendeVilkårsvurderinger =
                        listOf(
                            UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                        ),
                    vurderesEtter = Regelverk.NASJONALE_REGLER,
                ),
            )

        val aktør = randomAktør()

        val perioderMedEndring =
            EndringIVilkårsvurderingUtil
                .lagEndringIVilkårsvurderingTidslinje(
                    nåværendePersonResultaterForPerson = setOf(lagPersonResultatFraVilkårResultater(vilkårResultater, aktør)),
                    forrigePersonResultater = setOf(lagPersonResultatFraVilkårResultater(vilkårResultater, aktør)),
                ).tilPerioder()
                .filter { it.verdi == true }

        Assertions.assertTrue(perioderMedEndring.isEmpty())
    }

    @Test
    fun `Endring i vilkårsvurdering - skal returnere periode med endring dersom det har vært endringer i regelverk`() {
        val fødselsdato = jan22.førsteDagIInneværendeMåned()
        val nåværendeVilkårResultat =
            setOf(
                VilkårResultat(
                    behandlingId = 0,
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = fødselsdato,
                    periodeTom = mai22.sisteDagIInneværendeMåned(),
                    begrunnelse = "begrunnelse",
                    utdypendeVilkårsvurderinger =
                        listOf(
                            UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                        ),
                    vurderesEtter = Regelverk.NASJONALE_REGLER,
                ),
            )

        val forrigeVilkårResultat =
            setOf(
                VilkårResultat(
                    behandlingId = 0,
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = fødselsdato,
                    periodeTom = mai22.sisteDagIInneværendeMåned(),
                    begrunnelse = "begrunnelse",
                    utdypendeVilkårsvurderinger =
                        listOf(
                            UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                        ),
                    vurderesEtter = Regelverk.EØS_FORORDNINGEN,
                ),
            )

        val aktør = randomAktør()

        val perioderMedEndring =
            EndringIVilkårsvurderingUtil
                .lagEndringIVilkårsvurderingTidslinje(
                    nåværendePersonResultaterForPerson = setOf(lagPersonResultatFraVilkårResultater(nåværendeVilkårResultat, aktør)),
                    forrigePersonResultater = setOf(lagPersonResultatFraVilkårResultater(forrigeVilkårResultat, aktør)),
                ).tilPerioder()
                .filter { it.verdi == true }

        Assertions.assertEquals(1, perioderMedEndring.size)
        Assertions.assertEquals(feb22.førsteDagIInneværendeMåned(), perioderMedEndring.single().fom)
        Assertions.assertEquals(apr22.sisteDagIInneværendeMåned(), perioderMedEndring.single().tom)
    }

    @Test
    fun `Endring i vilkårsvurdering - skal returnere periode med endring dersom det har oppstått splitt i vilkårsvurderingen`() {
        val fødselsdato = jan22.førsteDagIInneværendeMåned()
        val forrigeVilkårResultat =
            setOf(
                VilkårResultat(
                    behandlingId = 0,
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = fødselsdato,
                    periodeTom = null,
                    begrunnelse = "",
                    utdypendeVilkårsvurderinger = listOf(),
                    vurderesEtter = Regelverk.NASJONALE_REGLER,
                ),
            )

        val nåværendeVilkårResultat =
            setOf(
                VilkårResultat(
                    behandlingId = 0,
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = fødselsdato,
                    periodeTom = mai22.atDay(7),
                    begrunnelse = "",
                    utdypendeVilkårsvurderinger = listOf(),
                    vurderesEtter = Regelverk.NASJONALE_REGLER,
                ),
                VilkårResultat(
                    behandlingId = 0,
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = mai22.atDay(8),
                    periodeTom = null,
                    begrunnelse = "",
                    utdypendeVilkårsvurderinger = listOf(),
                    vurderesEtter = Regelverk.NASJONALE_REGLER,
                ),
            )

        val aktør = randomAktør()

        val perioderMedEndring =
            EndringIVilkårsvurderingUtil
                .lagEndringIVilkårsvurderingTidslinje(
                    nåværendePersonResultaterForPerson = setOf(lagPersonResultatFraVilkårResultater(nåværendeVilkårResultat, aktør)),
                    forrigePersonResultater = setOf(lagPersonResultatFraVilkårResultater(forrigeVilkårResultat, aktør)),
                ).tilPerioder()
                .filter { it.verdi == true }

        Assertions.assertEquals(1, perioderMedEndring.size)
        Assertions.assertEquals(jun22.førsteDagIInneværendeMåned(), perioderMedEndring.single().fom)
    }

    @Test
    fun `Endring i vilkårsvurdering - skal ikke lage periode med endring hvis det kun er opphørt`() {
        val fødselsdato = LocalDate.of(2015, 1, 1)
        val nåværendeVilkårResultat =
            setOf(
                VilkårResultat(
                    behandlingId = 0,
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = fødselsdato,
                    periodeTom = LocalDate.of(2020, 1, 1),
                    begrunnelse = "begrunnelse",
                    utdypendeVilkårsvurderinger =
                        listOf(
                            UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                        ),
                    vurderesEtter = Regelverk.NASJONALE_REGLER,
                ),
            )

        val forrigeVilkårResultat =
            setOf(
                VilkårResultat(
                    behandlingId = 0,
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = fødselsdato,
                    periodeTom = null,
                    begrunnelse = "begrunnelse",
                    utdypendeVilkårsvurderinger =
                        listOf(
                            UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                        ),
                    vurderesEtter = Regelverk.NASJONALE_REGLER,
                ),
            )

        val aktør = randomAktør()

        val perioderMedEndring =
            EndringIVilkårsvurderingUtil
                .lagEndringIVilkårsvurderingTidslinje(
                    nåværendePersonResultaterForPerson = setOf(lagPersonResultatFraVilkårResultater(nåværendeVilkårResultat, aktør)),
                    forrigePersonResultater = setOf(lagPersonResultatFraVilkårResultater(forrigeVilkårResultat, aktør)),
                ).tilPerioder()
                .filter { it.verdi == true }

        Assertions.assertTrue(perioderMedEndring.isEmpty())
    }

    @Test
    fun `Endring i vilkårsvurdering - skal ikke lage periode med endring hvis eneste endring er å sette obligatoriske utdypende vilkårsvurderinger`() {
        val fødselsdato = LocalDate.of(2015, 1, 1)
        val forrigeVilkårResultat =
            setOf(
                VilkårResultat(
                    behandlingId = 0,
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = fødselsdato,
                    periodeTom = null,
                    begrunnelse = "migrering",
                    utdypendeVilkårsvurderinger = listOf(),
                    vurderesEtter = Regelverk.EØS_FORORDNINGEN,
                ),
            )

        val nåværendeVilkårResultat =
            setOf(
                VilkårResultat(
                    behandlingId = 0,
                    personResultat = null,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = fødselsdato,
                    periodeTom = null,
                    begrunnelse = "migrering",
                    utdypendeVilkårsvurderinger =
                        listOf(
                            UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                        ),
                    vurderesEtter = Regelverk.EØS_FORORDNINGEN,
                ),
            )

        val aktør = randomAktør()

        val perioderMedEndring =
            EndringIVilkårsvurderingUtil
                .lagEndringIVilkårsvurderingTidslinje(
                    nåværendePersonResultaterForPerson = setOf(lagPersonResultatFraVilkårResultater(nåværendeVilkårResultat, aktør)),
                    forrigePersonResultater = setOf(lagPersonResultatFraVilkårResultater(forrigeVilkårResultat, aktør)),
                ).tilPerioder()
                .filter { it.verdi == true }

        Assertions.assertTrue(perioderMedEndring.isEmpty())
    }

    private fun lagPersonResultatFraVilkårResultater(
        vilkårResultater: Set<VilkårResultat>,
        aktør: Aktør,
    ): PersonResultat {
        val vilkårsvurdering =
            lagVilkårsvurdering(
                behandling = lagBehandling(),
                resultat = Resultat.OPPFYLT,
                søkerAktør = randomAktør(),
            )
        val personResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = aktør)

        personResultat.setSortedVilkårResultater(vilkårResultater)

        return personResultat
    }
}
