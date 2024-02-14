package no.nav.familie.ks.sak.internal.endringKontantstøtteInfobrev2024

import no.nav.familie.ks.sak.integrasjon.infotrygd.InfotrygdReplikaClient
import no.nav.familie.ks.sak.integrasjon.logger
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

@Service
class DistribuerInformasjonsbrevKontantstøtteEndresInfotrygdService(
    private val taskService: TaskService,
    private val infotrygdReplikaClient: InfotrygdReplikaClient,
) {
    @Transactional
    fun hentPersonerFraInfotrygdMedBarnFødEtterAugust22(): List<String> {
        val brukereMedLøpendeKontantstøtteIInfotrygd =
            infotrygdReplikaClient.hentSøkereOgBarnForLøpendeFagsakerIInfotrygd()
        return brukereMedLøpendeKontantstøtteIInfotrygd.filter { søkerOgBarn ->
            val barnasFødselsdatoer =
                søkerOgBarn.barnIdenter.map { it.tilFødselsdato() }
            val erBarnFødtEtterSeptember22 = barnasFødselsdatoer.any { it >= YearMonth.of(2022, 9) }
            erBarnFødtEtterSeptember22
        }.map { it.søkerIdent }
    }

    @Transactional
    fun opprettTaskerForÅJournalføreOgSendeUtInformasjonsbrevKontantstøtteendringInfotrygd(søkerIdenterFraInfotrygd: Set<String>) {
        logger.info("Oppretter tasker for å sende ut informasjonsbrev om mulig endring av kontantstøtte til brukere fra infotrygd. Antall brukere = ${søkerIdenterFraInfotrygd.size}")
        søkerIdenterFraInfotrygd.map {
            val task = OpprettFagsakOgSendInformasjonsbrevKontantstøtteendringTask.lagTask(søkerIdent = it)
            taskService.save(task)
        }
    }
}

fun String.tilFødselsdato(): YearMonth {
    val erNAVSyntetisk = substring(2, 3).toInt() in 4..7
    val erSkattSyntetisk = substring(2, 3).toInt() >= 8

    val måned =
        substring(2, 4).toInt() - (
            when {
                erNAVSyntetisk -> 40
                erSkattSyntetisk -> 80
                else -> 0
            }
        )
    val år = substring(4, 6).toInt()
    val datoUtenÅrhundre = YearMonth.of(år, måned)
    val individnummer = this.substring(6, 9).toInt()
    return when {
        individnummer in 0..499 -> datoUtenÅrhundre.plusYears(1900)
        individnummer in 500..749 && år >= 54 && år <= 99 -> datoUtenÅrhundre.plusYears(1800)
        individnummer in 900..999 && år >= 40 && år <= 99 -> datoUtenÅrhundre.plusYears(1900)
        individnummer in 500..999 && år >= 0 && år <= 39 -> datoUtenÅrhundre.plusYears(2000)
        år < 25 -> datoUtenÅrhundre.plusYears(2000)
        else -> throw IllegalArgumentException()
    }
}
