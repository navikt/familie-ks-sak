package no.nav.familie.ks.sak.api.dto

import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.IVedtakBegrunnelse
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.Standardbegrunnelse
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.AnnenVurderingType
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class PersonResultatResponsDto(
    val personIdent: String,
    val vilkårResultater: List<VilkårResultatDto>,
    val andreVurderinger: List<AnnenVurderingDto>
)

data class EndreVilkårResultatDto(
    val personIdent: String,
    val endretVilkårResultat: VilkårResultatDto
)

data class NyttVilkårDto(
    val personIdent: String,
    val vilkårType: Vilkår
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
    val avslagBegrunnelser: List<Standardbegrunnelse>? = emptyList(),
    val vurderesEtter: Regelverk? = null,
    val antallTimer: BigDecimal? = null,
    val utdypendeVilkårsvurderinger: List<UtdypendeVilkårsvurdering> = emptyList()
) {

    fun erAvslagUtenPeriode() = erEksplisittAvslagPåSøknad == true && periodeFom == null && periodeTom == null
    fun harFremtidigTom() = periodeTom?.isAfter(LocalDate.now().sisteDagIMåned()) ?: true

    fun tilVilkårResultat(
        vilkårResultat: VilkårResultat
    ): VilkårResultat {
        return VilkårResultat(
            periodeFom = periodeFom,
            periodeTom = periodeTom,
            begrunnelse = begrunnelse,
            standardbegrunnelser = avslagBegrunnelser ?: emptyList(),
            resultat = resultat,
            erAutomatiskVurdert = false,
            erEksplisittAvslagPåSøknad = erEksplisittAvslagPåSøknad,
            behandlingId = vilkårResultat.personResultat!!.vilkårsvurdering.behandling.id,
            vurderesEtter = vurderesEtter,
            utdypendeVilkårsvurderinger = utdypendeVilkårsvurderinger,
            personResultat = vilkårResultat.personResultat,
            vilkårType = vilkårResultat.vilkårType,
            antallTimer = antallTimer
        )
    }
}

data class AnnenVurderingDto(
    val id: Long,
    val resultat: Resultat,
    val type: AnnenVurderingType,
    val begrunnelse: String?
)

data class VedtakBegrunnelseTilknyttetVilkårResponseDto(
    val id: IVedtakBegrunnelse,
    val navn: String,
    val vilkår: Vilkår?
)
