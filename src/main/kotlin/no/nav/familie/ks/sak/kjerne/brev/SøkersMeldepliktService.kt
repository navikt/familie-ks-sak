package no.nav.familie.ks.sak.kjerne.brev

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SøkersMeldepliktService(
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val behandlingService: BehandlingService,
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val featureToggleService: FeatureToggleService,
) {
    private val logger = LoggerFactory.getLogger(SøkersMeldepliktService::class.java)

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
    ): Boolean {
        if (featureToggleService.isEnabled(FeatureToggle.BRUK_NY_LOGIKK_FOR_SØKERS_MELDEPLIKT)) {
            val behandling = vedtak.behandling
            val forrigeBehandling = behandlingService.hentForrigeBehandlingSomErVedtatt(vedtak.behandling)

            val barnIBehandling = personopplysningGrunnlagService.hentBarnaThrows(behandling.id).map { it.aktør }

            val barnIForrigeBehandling =
                if (forrigeBehandling != null) {
                    personopplysningGrunnlagService.hentBarnaThrows(forrigeBehandling.id).map { it.aktør }
                } else {
                    emptyList()
                }

            val relevanteBarn =
                // Det er mulig å ha flere førstegangsbehandlinger på en fagsak
                if (behandling.type == BehandlingType.FØRSTEGANGSBEHANDLING) {
                    barnIBehandling.minus(barnIForrigeBehandling)
                } else {
                    barnIBehandling
                }

            if (relevanteBarn.isEmpty()) {
                logger.error("Forventer minst et relevant barn ment fant ingen for behandlingId=${behandling.id}")
                throw Feil("Forventer minst et relevant barn ment fant ingen for behandlingId=${behandling.id}")
            }

            return vilkårsvurderingService
                .hentAktivVilkårsvurderingForBehandling(behandlingId = vedtak.behandling.id)
                .personResultater
                .filter { relevanteBarn.contains(it.aktør) }
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
