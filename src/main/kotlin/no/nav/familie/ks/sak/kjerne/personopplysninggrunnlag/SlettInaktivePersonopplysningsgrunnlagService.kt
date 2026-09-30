package no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag

import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlagRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SlettInaktivePersonopplysningsgrunnlagService(
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
) {
    @Transactional
    fun slettBatchMedInaktiveGrunnlag(
        etterId: Long,
        batchStørrelse: Int,
    ): SlettetBatch {
        val grunnlagIder =
            personopplysningGrunnlagRepository.finnIderForInaktiveGrunnlagMedAktivtGrunnlagPåSammeBehandling(
                etterId = etterId,
                pageable = PageRequest.of(0, batchStørrelse),
            )
        if (grunnlagIder.isEmpty()) return SlettetBatch(grunnlagIder = emptyList(), antallSlettedeRaderPerTabell = emptyMap())

        val antallRaderSomSlettesViaCascade = personopplysningGrunnlagRepository.tellRaderSomSlettesMedGrunnlag(grunnlagIder)
        val antallSlettedeGrunnlag = personopplysningGrunnlagRepository.slettPersonopplysningsgrunnlag(grunnlagIder)

        val antallSlettedeRaderPerTabell =
            buildMap {
                put("gr_personopplysninger", antallSlettedeGrunnlag.toLong())
                antallRaderSomSlettesViaCascade.forEach { put(it.tabell, it.antall) }
            }
        return SlettetBatch(grunnlagIder = grunnlagIder, antallSlettedeRaderPerTabell = antallSlettedeRaderPerTabell)
    }

    fun tellInaktiveGrunnlagUtenAktivtGrunnlagPåSammeBehandling(): Long = personopplysningGrunnlagRepository.tellInaktiveGrunnlagUtenAktivtGrunnlagPåSammeBehandling()
}

data class SlettetBatch(
    val grunnlagIder: List<Long>,
    val antallSlettedeRaderPerTabell: Map<String, Long>,
)
