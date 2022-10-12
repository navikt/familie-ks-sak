package no.nav.familie.ks.sak.api.dto

import no.nav.familie.ks.sak.kjerne.vedtak.IVedtakBegrunnelse
import no.nav.familie.ks.sak.kjerne.vedtak.Standardbegrunnelse
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.AnnenVurderingType
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.Vilkår
import java.time.LocalDate
import java.time.LocalDateTime

data class PersonResultatResponsDto(
    val personIdent: String,
    val vilkårResultater: List<VilkårResultatDto>,
    val andreVurderinger: List<AnnenVurderingDto>
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
    val utdypendeVilkårsvurderinger: List<UtdypendeVilkårsvurdering> = emptyList()
)

data class AnnenVurderingDto(
    val id: Long,
    val resultat: Resultat,
    val type: AnnenVurderingType,
    val begrunnelse: String?
)

data class VedtakBegrunnelseTilknyttetVilkårDto(
    val id: IVedtakBegrunnelse,
    val navn: String,
    val vilkår: Vilkår?
)
