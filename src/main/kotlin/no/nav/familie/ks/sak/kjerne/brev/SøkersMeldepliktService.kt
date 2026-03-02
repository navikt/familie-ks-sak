package no.nav.familie.ks.sak.kjerne.brev

import no.nav.familie.ks.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import org.springframework.stereotype.Service
import java.time.YearMonth

@Service
class SøkersMeldepliktService(
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val featureToggleService: FeatureToggleService,
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
        inneværendeMåned: YearMonth = YearMonth.now(),
    ): Boolean {
        if (featureToggleService.isEnabled(FeatureToggle.BRUK_NY_LOGIKK_FOR_SØKERS_MELDEPLIKT)) {
            val barn = personopplysningGrunnlagService.hentBarnaThrows(vedtak.behandling.id).map { it.aktør }

            val barnMedRelevanteAndeler =
                andelTilkjentYtelseRepository
                    .finnAndelerTilkjentYtelseForBehandling(vedtak.behandling.id)
                    .filter { it.aktør in barn }
                    .filter { it.stønadTom >= inneværendeMåned }
                    .map { it.aktør }
                    .distinct()

            if (barnMedRelevanteAndeler.isEmpty()) {
                // Om det ikke finnes barn med relevante andeler anser vi det som om søker har meldt fra om barnehageplass
                return true
            }

            return vilkårsvurderingService
                .hentAktivVilkårsvurderingForBehandling(behandlingId = vedtak.behandling.id)
                .personResultater
                .filter { it.aktør in barnMedRelevanteAndeler }
                .flatMap { it.vilkårResultater }
                .filter { it.vilkårType == Vilkår.BARNEHAGEPLASS }
                .all { it.søkerHarMeldtFraOmBarnehageplass == true }
        } else {
            return vilkårsvurderingService
                .hentAktivVilkårsvurderingForBehandling(behandlingId = vedtak.behandling.id)
                .personResultater
                .flatMap { it.vilkårResultater }
                .any { it.søkerHarMeldtFraOmBarnehageplass ?: false }
        }
    }
}
