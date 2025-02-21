package no.nav.familie.ks.sak.api.dto

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.AnnenVurderingType
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.IBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.IBegrunnelseDeserializer
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class PersonResultatResponsDto(
    val personIdent: String,
    val vilkårResultater: List<VilkårResultatDto>,
    val andreVurderinger: List<AnnenVurderingDto>,
)

data class EndreVilkårResultatDto(
    val personIdent: String,
    val adopsjonsdato: LocalDate?,
    val endretVilkårResultat: VilkårResultatDto,
)

data class NyttVilkårDto(
    val personIdent: String,
    val vilkårType: Vilkår,
)

data class VilkårResultatDto(
    val id: Long,
    val vilkårType: Vilkår,
    val resultat: Resultat,
    val periodeFom: LocalDate?,
    val periodeTom: LocalDate?,
    val begrunnelse: String,
    val endretAv: String,
    val endretTidspunkt: LocalDateTime,
    val behandlingId: Long,
    val erVurdert: Boolean = false,
    val erAutomatiskVurdert: Boolean = false,
    val erEksplisittAvslagPåSøknad: Boolean? = null,
    @JsonDeserialize(using = IBegrunnelseDeserializer::class)
    val avslagBegrunnelser: List<IBegrunnelse>? = emptyList(),
    val vurderesEtter: Regelverk? = null,
    val antallTimer: BigDecimal? = null,
    val utdypendeVilkårsvurderinger: List<UtdypendeVilkårsvurdering> = emptyList(),
    val søkerHarMeldtFraOmBarnehageplass: Boolean? = null,
) {
    fun erAvslagUtenPeriode() = erEksplisittAvslagPåSøknad == true && periodeFom == null && periodeTom == null

    fun harFremtidigTom() = periodeTom?.isAfter(LocalDate.now().sisteDagIMåned()) ?: true

    fun tilVilkårResultat(vilkårResultat: VilkårResultat): VilkårResultat =
        VilkårResultat(
            periodeFom = periodeFom,
            periodeTom = periodeTom,
            begrunnelse = begrunnelse,
            begrunnelser = avslagBegrunnelser ?: emptyList(),
            resultat = resultat,
            erAutomatiskVurdert = false,
            erEksplisittAvslagPåSøknad = erEksplisittAvslagPåSøknad,
            behandlingId =
                vilkårResultat.personResultat!!
                    .vilkårsvurdering.behandling.id,
            vurderesEtter = vurderesEtter,
            utdypendeVilkårsvurderinger = utdypendeVilkårsvurderinger,
            personResultat = vilkårResultat.personResultat,
            vilkårType = vilkårResultat.vilkårType,
            // antallTimer kan ikke være 0
            antallTimer = if (antallTimer == BigDecimal(0)) null else antallTimer,
            søkerHarMeldtFraOmBarnehageplass = søkerHarMeldtFraOmBarnehageplass,
        )
}

data class AnnenVurderingDto(
    val id: Long,
    val resultat: Resultat,
    val type: AnnenVurderingType,
    val begrunnelse: String?,
)

data class VedtakBegrunnelseTilknyttetVilkårResponseDto(
    val id: IBegrunnelse,
    val navn: String,
    val vilkår: Vilkår?,
)
