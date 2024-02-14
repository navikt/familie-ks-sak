package no.nav.familie.ks.sak.internal.endringKontantstøtteInfobrev2024

import no.nav.familie.kontrakter.felles.Fødselsnummer
import no.nav.familie.ks.sak.common.util.toYearMonth
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
            val barnasFødselsdatoer = søkerOgBarn.barnIdenter.map { Fødselsnummer(it).fødselsdato }
            val erBarnFødtEtterSeptember22 = barnasFødselsdatoer.any { it.toYearMonth() >= YearMonth.of(2022, 9) }
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
