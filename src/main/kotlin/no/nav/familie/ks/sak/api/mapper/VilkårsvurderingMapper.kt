package no.nav.familie.ks.sak.api.mapper

import no.nav.familie.ks.sak.api.dto.AnnenVurderingDto
import no.nav.familie.ks.sak.api.dto.PersonResponsDto
import no.nav.familie.ks.sak.api.dto.PersonResultatResponsDto
import no.nav.familie.ks.sak.api.dto.VilkårResultatDto
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.AnnenVurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.lovverk.LovverkUtleder

object VilkårsvurderingMapper {
    fun lagPersonResultatRespons(
        personResultat: PersonResultat,
        person: PersonResponsDto,
    ) =
        PersonResultatResponsDto(
            personIdent = personResultat.aktør.aktivFødselsnummer(),
            vilkårResultater = personResultat.vilkårResultater.map { lagVilkårResultatRespons(it) },
            andreVurderinger = personResultat.andreVurderinger.map { lagAnnenVurderingRespons(it) },
            lovverk = LovverkUtleder.utledLovverkForBarn(person.fødselsdato!!, true),
        )

    private fun lagVilkårResultatRespons(vilkårResultat: VilkårResultat) =
        VilkårResultatDto(
            resultat = vilkårResultat.resultat,
            erAutomatiskVurdert = vilkårResultat.erAutomatiskVurdert,
            erEksplisittAvslagPåSøknad = vilkårResultat.erEksplisittAvslagPåSøknad,
            id = vilkårResultat.id,
            vilkårType = vilkårResultat.vilkårType,
            periodeFom = vilkårResultat.periodeFom,
            periodeTom = vilkårResultat.periodeTom,
            begrunnelse = vilkårResultat.begrunnelse,
            endretAv = vilkårResultat.endretAv,
            endretTidspunkt = vilkårResultat.endretTidspunkt,
            behandlingId = vilkårResultat.behandlingId,
            erVurdert = vilkårResultat.resultat != Resultat.IKKE_VURDERT || vilkårResultat.versjon > 0,
            avslagBegrunnelser = vilkårResultat.begrunnelser,
            vurderesEtter = vilkårResultat.vurderesEtter,
            utdypendeVilkårsvurderinger = vilkårResultat.utdypendeVilkårsvurderinger,
            antallTimer = vilkårResultat.antallTimer,
            søkerHarMeldtFraOmBarnehageplass = vilkårResultat.søkerHarMeldtFraOmBarnehageplass,
        )

    private fun lagAnnenVurderingRespons(annenVurdering: AnnenVurdering) =
        AnnenVurderingDto(
            id = annenVurdering.id,
            resultat = annenVurdering.resultat,
            type = annenVurdering.type,
            begrunnelse = annenVurdering.begrunnelse,
        )
}
