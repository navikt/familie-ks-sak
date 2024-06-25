import no.nav.familie.ks.sak.common.util.DATO_LOVENDRING_2024
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårRegelsett
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import java.time.LocalDate

fun lagAutomatiskGenererteVilkårForBarnetsAlder(
    personResultat: PersonResultat,
    behandling: Behandling,
    fødselsdato: LocalDate,
): List<VilkårResultat> {
    val periodeFomBarnetsAlderLov2024 = fødselsdato.plusMonths(13)
    val periodeTomBarnetsAlderLov2024 = fødselsdato.plusMonths(19)

    val erTruffetAvRegelverk2021 = periodeFomBarnetsAlderLov2024.isBefore(DATO_LOVENDRING_2024)
    val erTruffetAvRegelverk2024 = periodeTomBarnetsAlderLov2024.isAfter(DATO_LOVENDRING_2024)

    val vilkårResultatEtterRegelverk2021 =
        if (erTruffetAvRegelverk2021) {
            VilkårResultat(
                personResultat = personResultat,
                erAutomatiskVurdert = true,
                resultat = Resultat.OPPFYLT,
                vilkårType = Vilkår.BARNETS_ALDER,
                begrunnelse = "Vurdert og satt automatisk",
                behandlingId = behandling.id,
                periodeFom = fødselsdato.plusYears(1),
                periodeTom = minOf(fødselsdato.plusYears(2), DATO_LOVENDRING_2024),
                regelsett = VilkårRegelsett.LOV_AUGUST_2021,
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
                periodeFom = maxOf(fødselsdato.plusMonths(13), DATO_LOVENDRING_2024),
                periodeTom = fødselsdato.plusMonths(19),
                regelsett = VilkårRegelsett.LOV_AUGUST_2024,
            )
        } else {
            null
        }

    return listOfNotNull(
        vilkårResultatEtterRegelverk2021,
        vilkårResultatEtterRegelverk2024,
    )
}
