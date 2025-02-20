package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering

import no.nav.familie.ks.sak.common.util.DATO_LOVENDRING_2024
import no.nav.familie.ks.sak.common.util.erSammeEllerEtter
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.lovverk.Lovverk
import no.nav.familie.ks.sak.kjerne.lovverk.LovverkUtleder
import java.time.LocalDate

fun lagAutomatiskGenererteVilkårForBarnetsAlder(
    personResultat: PersonResultat,
    behandlingId: Long,
    fødselsdato: LocalDate,
    adopsjonsdato: LocalDate?,
): List<VilkårResultat> {
    val lovverk = LovverkUtleder.utledLovverkForBarn(fødselsdato = fødselsdato, adopsjonsdato = adopsjonsdato)
    val erAdopsjon = adopsjonsdato != null
    return when (lovverk) {
        Lovverk.FØR_LOVENDRING_2025 -> lagAutomatiskGenererteVilkårForBarnetsAlder2021og2024(personResultat, behandlingId, fødselsdato, erAdopsjon)
        Lovverk.LOVENDRING_FEBRUAR_2025 -> lagAutomatiskGenererteVilkårForBarnetsAlder2025(personResultat, behandlingId, fødselsdato, erAdopsjon)
    }
}

private fun lagAutomatiskGenererteVilkårForBarnetsAlder2025(
    personResultat: PersonResultat,
    behandlingId: Long,
    fødselsdato: LocalDate,
    erAdopsjon: Boolean = false,
): List<VilkårResultat> =
    listOf(
        VilkårResultat(
            personResultat = personResultat,
            erAutomatiskVurdert = true,
            resultat = Resultat.OPPFYLT,
            vilkårType = Vilkår.BARNETS_ALDER,
            begrunnelse = "Vurdert og satt automatisk",
            behandlingId = behandlingId,
            periodeFom = fødselsdato.plusMonths(12),
            periodeTom = fødselsdato.plusMonths(20),
            utdypendeVilkårsvurderinger = if (erAdopsjon) listOf(UtdypendeVilkårsvurdering.ADOPSJON) else emptyList(),
        ),
    )

private fun lagAutomatiskGenererteVilkårForBarnetsAlder2021og2024(
    personResultat: PersonResultat,
    behandlingId: Long,
    fødselsdato: LocalDate,
    erAdopsjon: Boolean = false,
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
                behandlingId = behandlingId,
                periodeFom = periodeFomBarnetsAlderLov2021,
                periodeTom = minOf(periodeTomBarnetsAlderLov2021, DATO_LOVENDRING_2024.minusDays(1)),
                utdypendeVilkårsvurderinger = if (erAdopsjon) listOf(UtdypendeVilkårsvurdering.ADOPSJON) else emptyList(),
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
                behandlingId = behandlingId,
                periodeFom = maxOf(periodeFomBarnetsAlderLov2024, DATO_LOVENDRING_2024),
                periodeTom = periodeTomBarnetsAlderLov2024,
                utdypendeVilkårsvurderinger = if (erAdopsjon) listOf(UtdypendeVilkårsvurdering.ADOPSJON) else emptyList(),
            )
        } else {
            null
        }

    return listOfNotNull(
        vilkårResultatEtterRegelverk2021,
        vilkårResultatEtterRegelverk2024,
    )
}
