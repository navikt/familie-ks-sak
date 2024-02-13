package no.nav.familie.ks.sak.internal.endringKontantstøtteInfobrev2024

import no.nav.familie.ks.sak.integrasjon.infotrygd.InfotrygdReplikaClient
import no.nav.familie.ks.sak.integrasjon.infotrygd.SøkerOgBarn
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DistribuerInformasjonsbrevKontantstøtteEndresInfotrygdService(
    private val taskService: TaskService,
    private val infotrygdReplikaClient: InfotrygdReplikaClient,
) {
    @Transactional
    fun opprettTaskerForÅJournalføreOgSendeUtInformasjonsbrevKontantstøtteendringInfotrygd(erDryRun: Boolean): List<SøkerOgBarn> {
        val brukereMedLøpendeKontantstøtteIInfotrygd =
            infotrygdReplikaClient.hentSøkereOgBarnForLøpendeFagsakerIInfotrygd()

        if (!erDryRun) {
            brukereMedLøpendeKontantstøtteIInfotrygd.map {
                val task = OpprettFagsakOgSendInformasjonsbrevKontantstøtteendringTask.lagTask(søkerOgBarn = it)
                taskService.save(task)
            }
        }

        return brukereMedLøpendeKontantstøtteIInfotrygd
    }
}
