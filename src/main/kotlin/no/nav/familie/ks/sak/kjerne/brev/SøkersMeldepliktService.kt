package no.nav.familie.ks.sak.kjerne.brev

import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import org.springframework.stereotype.Service

@Service
class SøkersMeldepliktService(
    private val vilkårsvurderingService: VilkårsvurderingService,
) {
    fun skalSøkerMeldeFraOmEndringerEøsSelvstendigRett(
        vedtak: Vedtak,
    ): Boolean {
        val annenForelderOmfattetAvNorskLovgivningErSattPåBosattIRiket =
            vilkårsvurderingService
                .hentAktivVilkårsvurderingForBehandling(behandlingId = vedtak.behandling.id)
                .personResultater
                .flatMap { it.vilkårResultater }
                .any {
                    val erAnnenForelderOmfattetAvNorskLovgivning =
                        it.utdypendeVilkårsvurderinger.contains(
                            UtdypendeVilkårsvurdering.ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING,
                        )
                    erAnnenForelderOmfattetAvNorskLovgivning && it.vilkårType == Vilkår.BOSATT_I_RIKET
                }

        val passendeBehandlingsresultat =
            vedtak.behandling.resultat !in
                listOf(
                    Behandlingsresultat.AVSLÅTT,
                    Behandlingsresultat.ENDRET_OG_OPPHØRT,
                    Behandlingsresultat.OPPHØRT,
                )

        return annenForelderOmfattetAvNorskLovgivningErSattPåBosattIRiket && passendeBehandlingsresultat
    }

    fun harSøkerMeldtFraOmBarnehagePlass(
        vedtak: Vedtak,
    ): Boolean =
        vilkårsvurderingService
            .hentAktivVilkårsvurderingForBehandling(behandlingId = vedtak.behandling.id)
            .personResultater
            .flatMap { it.vilkårResultater }
            .any { it.søkerHarMeldtFraOmBarnehageplass ?: false }
}
