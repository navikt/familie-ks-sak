import java.time.LocalDate
import no.nav.familie.ks.sak.common.util.DATO_LOVENDRING_2024
import no.nav.familie.ks.sak.common.util.erSammeEllerEtter
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat

fun lagAutomatiskGenererteVilkårForBarnetsAlder(
    personResultat: PersonResultat,
    behandling: Behandling,
    fødselsdato: LocalDate,
): List<VilkårResultat> {
    val periodeFomBarnetsAlderLov2024 = fødselsdato.plusMonths(13)
    val periodeTomBarnetsAlderLov2024 = fødselsdato.plusMonths(19)

    val periodeFomBarnetsAlderLov2021 = fødselsdato.plusYears(1)
    val periodeTomBarnetsAlderLov2021 = fødselsdato.plusYears(2)

    val erTruffetAvRegelverk2021 = periodeFomBarnetsAlderLov2021.isBefore(DATO_LOVENDRING_2024)
    val erTruffetAvRegelverk2024 = periodeTomBarnetsAlderLov2024.erSammeEllerEtter(DATO_LOVENDRING_2024)

    val vilkårResultatEtterRegelverk2021 =
        if (erTruffetAvRegelverk2021) {
            VilkårResultat(
                personResultat = personResultat,
                erAutomatiskVurdert = true,
                resultat = Resultat.OPPFYLT,
                vilkårType = Vilkår.BARNETS_ALDER,
                begrunnelse = "Vurdert og satt automatisk",
                behandlingId = behandling.id,
                periodeFom = periodeFomBarnetsAlderLov2021,
                periodeTom = minOf(periodeTomBarnetsAlderLov2021, DATO_LOVENDRING_2024.minusDays(1)),
            )
        } else {
            null
        }

    val vilkårResultatEtterRegelverk2024 =
        if (erTruffetAvRegelverk2024) {
            VilkårResultat(
                personResultat = personResultat,
                erAutomatiskVurdert = true,
                resultat = Resultat.OPPFYLT,
                vilkårType = Vilkår.BARNETS_ALDER,
                begrunnelse = "Vurdert og satt automatisk",
                behandlingId = behandling.id,
                periodeFom = maxOf(periodeFomBarnetsAlderLov2024, DATO_LOVENDRING_2024),
                periodeTom = periodeTomBarnetsAlderLov2024,
            )
        } else {
            null
        }

    return listOfNotNull(
        vilkårResultatEtterRegelverk2021,
        vilkårResultatEtterRegelverk2024,
    )
}
