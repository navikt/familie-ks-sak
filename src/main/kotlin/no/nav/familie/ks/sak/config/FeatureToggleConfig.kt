package no.nav.familie.ks.sak.config

import io.getunleash.DefaultUnleash
import io.getunleash.UnleashContext
import io.getunleash.UnleashContextProvider
import io.getunleash.strategy.GradualRolloutRandomStrategy
import io.getunleash.strategy.Strategy
import io.getunleash.util.UnleashConfig
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import java.net.URI

@ConfigurationProperties("funksjonsbrytere")
class FeatureToggleConfig(
    private val enabled: Boolean,
    private val unleash: Unleash,
) {

    data class Unleash(
        val uri: URI,
        val cluster: String,
        val applicationName: String,
    )

    @Bean
    fun featureToggle(): FeatureToggleService =
        if (enabled) {
            lagUnleashFeatureToggleService()
        } else {
            logger.warn(
                "Funksjonsbryter-funksjonalitet er skrudd AV. " +
                    "Gir standardoppførsel for alle funksjonsbrytere, dvs 'false'",
            )
            lagDummyFeatureToggleService()
        }

    private fun lagUnleashFeatureToggleService(): FeatureToggleService {
        val defaultUnleash = DefaultUnleash(
            UnleashConfig.builder()
                .appName(unleash.applicationName)
                .unleashAPI(unleash.uri)
                .unleashContextProvider(lagUnleashContextProvider())
                .build(),
            ByClusterStrategy(unleash.cluster),
            ByAnsvarligSaksbehandler(),
            GradualRolloutRandomStrategy(),
        )

        return object : FeatureToggleService {
            override fun isEnabled(toggleId: String, defaultValue: Boolean): Boolean {
                return defaultUnleash.isEnabled(toggleId, defaultValue)
            }
        }
    }

    private fun lagUnleashContextProvider(): UnleashContextProvider = UnleashContextProvider {
        UnleashContext.builder()
            .appName(unleash.applicationName)
            .build()
    }

    class ByClusterStrategy(private val clusterName: String) : Strategy {

        override fun isEnabled(parameters: MutableMap<String, String>): Boolean =
            parameters["cluster"]?.contains(clusterName) ?: false

        override fun getName(): String = "byCluster"
    }

    class ByAnsvarligSaksbehandler : Strategy {

        override fun isEnabled(parameters: MutableMap<String, String>): Boolean =
            parameters["saksbehandler"]?.contains(SikkerhetContext.hentSaksbehandlerEpost()) ?: false

        override fun getName(): String = "byAnsvarligSaksbehandler"
    }

    private fun lagDummyFeatureToggleService(): FeatureToggleService {
        return object : FeatureToggleService {
            override fun isEnabled(toggleId: String, defaultValue: Boolean): Boolean {
                return false
            }
        }
    }

    companion object {
        const val TEKNISK_VEDLIKEHOLD_HENLEGGELSE = "familie-ks-sak.teknisk-vedlikehold-henleggelse.tilgangsstyring"
        const val TEKNISK_ENDRING = "familie-ks-sak.behandling.teknisk-endring"
        const val KAN_MANUELT_KORRIGERE_MED_VEDTAKSBREV = "familie-ks-sak.behandling.korreksjon-vedtaksbrev"

        private val logger = LoggerFactory.getLogger(FeatureToggleConfig::class.java)
    }
}

interface FeatureToggleService {

    fun isEnabled(toggleId: String): Boolean {
        return isEnabled(toggleId, false)
    }

    fun isNotEnabled(toggleId: String) = !isEnabled(toggleId)

    fun isEnabled(toggleId: String, defaultValue: Boolean): Boolean
}
