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
                    nåværendePersonResultat = lagPersonResultatFraVilkårResultater(vilkårResultater, aktør),
                    forrigePersonResultat = lagPersonResultatFraVilkårResultater(vilkårResultater, aktør),
                ).tilPerioder()
                .filter { it.verdi == true }

        Assertions.assertTrue(perioderMedEndring.isEmpty())

        val endringstidspunkt =
            EndringIVilkårsvurderingUtil.utledEndringstidspunktForVilkårsvurdering(
                nåværendePersonResultat = setOf(lagPersonResultatFraVilkårResultater(vilkårResultater, aktør)),
                forrigePersonResultat = setOf(lagPersonResultatFraVilkårResultater(vilkårResultater, aktør)),
            )

        Assertions.assertNull(endringstidspunkt)
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
                    nåværendePersonResultat = lagPersonResultatFraVilkårResultater(nåværendeVilkårResultat, aktør),
                    forrigePersonResultat = lagPersonResultatFraVilkårResultater(forrigeVilkårResultat, aktør),
                ).tilPerioder()
                .filter { it.verdi == true }

        Assertions.assertEquals(1, perioderMedEndring.size)
        Assertions.assertEquals(fødselsdato, perioderMedEndring.single().fom)
        Assertions.assertEquals(mai22.sisteDagIInneværendeMåned(), perioderMedEndring.single().tom)

        val endringstidspunkt =
            EndringIVilkårsvurderingUtil.utledEndringstidspunktForVilkårsvurdering(
                nåværendePersonResultat = setOf(lagPersonResultatFraVilkårResultater(nåværendeVilkårResultat, aktør)),
                forrigePersonResultat = setOf(lagPersonResultatFraVilkårResultater(forrigeVilkårResultat, aktør)),
            )

        Assertions.assertEquals(jan22, endringstidspunkt)
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
                    nåværendePersonResultat = lagPersonResultatFraVilkårResultater(nåværendeVilkårResultat, aktør),
                    forrigePersonResultat = lagPersonResultatFraVilkårResultater(forrigeVilkårResultat, aktør),
                ).tilPerioder()
                .filter { it.verdi == true }

        Assertions.assertEquals(1, perioderMedEndring.size)
        Assertions.assertEquals(mai22.atDay(8), perioderMedEndring.single().fom)

        val endringstidspunkt =
            EndringIVilkårsvurderingUtil.utledEndringstidspunktForVilkårsvurdering(
                nåværendePersonResultat = setOf(lagPersonResultatFraVilkårResultater(nåværendeVilkårResultat, aktør)),
                forrigePersonResultat = setOf(lagPersonResultatFraVilkårResultater(forrigeVilkårResultat, aktør)),
            )

        Assertions.assertEquals(mai22, endringstidspunkt)
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
                    nåværendePersonResultat = lagPersonResultatFraVilkårResultater(nåværendeVilkårResultat, aktør),
                    forrigePersonResultat = lagPersonResultatFraVilkårResultater(forrigeVilkårResultat, aktør),
                ).tilPerioder()
                .filter { it.verdi == true }

        Assertions.assertTrue(perioderMedEndring.isEmpty())

        val endringstidspunkt =
            EndringIVilkårsvurderingUtil.utledEndringstidspunktForVilkårsvurdering(
                nåværendePersonResultat = setOf(lagPersonResultatFraVilkårResultater(nåværendeVilkårResultat, aktør)),
                forrigePersonResultat = setOf(lagPersonResultatFraVilkårResultater(forrigeVilkårResultat, aktør)),
            )

        Assertions.assertNull(endringstidspunkt)
    }

    @Test
    fun `Endring i vilkårsvurdering - skal ikke lage periode med endring hvis det er barnets alder som er endret`() {
        val nåværendeVilkårResultat =
            setOf(
                VilkårResultat(
                    behandlingId = 0,
                    personResultat = null,
                    vilkårType = Vilkår.BARNETS_ALDER,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2024, 5, 9),
                    periodeTom = LocalDate.of(2024, 7, 31),
                    begrunnelse = "Test",
                    utdypendeVilkårsvurderinger = emptyList(),
                    vurderesEtter = Regelverk.NASJONALE_REGLER,
                ),
                VilkårResultat(
                    behandlingId = 0,
                    personResultat = null,
                    vilkårType = Vilkår.BARNETS_ALDER,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2024, 8, 1),
                    periodeTom = LocalDate.of(2024, 12, 9),
                    begrunnelse = "Test",
                    utdypendeVilkårsvurderinger = emptyList(),
                    vurderesEtter = Regelverk.NASJONALE_REGLER,
                ),
            )

        val forrigeVilkårResultat =
            setOf(
                VilkårResultat(
                    behandlingId = 0,
                    personResultat = null,
                    vilkårType = Vilkår.BARNETS_ALDER,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2024, 5, 9),
                    periodeTom = LocalDate.of(2025, 5, 9),
                    begrunnelse = "Test",
                    utdypendeVilkårsvurderinger = emptyList(),
                    vurderesEtter = Regelverk.NASJONALE_REGLER,
                ),
            )

        val aktør = randomAktør()

        val perioderMedEndring =
            EndringIVilkårsvurderingUtil
                .lagEndringIVilkårsvurderingTidslinje(
                    nåværendePersonResultat = lagPersonResultatFraVilkårResultater(nåværendeVilkårResultat, aktør),
                    forrigePersonResultat = lagPersonResultatFraVilkårResultater(forrigeVilkårResultat, aktør),
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
                    nåværendePersonResultat = lagPersonResultatFraVilkårResultater(nåværendeVilkårResultat, aktør),
                    forrigePersonResultat = lagPersonResultatFraVilkårResultater(forrigeVilkårResultat, aktør),
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
