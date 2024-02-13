package no.nav.familie.ks.sak.internal.EndringKontantstøtte2024

import no.nav.familie.ks.sak.integrasjon.infotrygd.InfotrygdReplikaClient
import no.nav.familie.ks.sak.integrasjon.infotrygd.SøkerOgBarn
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.stereotype.Service

@Service
class DistribuerInformasjonsbrevKontantsøtteEndresInfotrygdService(
    private val taskService: TaskService,
    private val infotrygdReplikaClient: InfotrygdReplikaClient,
) {
    fun opprettTaskerForÅJournalføreOgSendeUtInformasjonsbrevKontantstøtteendringInfotrygd(erDryRun: Boolean): List<SøkerOgBarn> {
        val brukereMedLøpendeKontantstøtteIInfotrygd =
            infotrygdReplikaClient.hentAlleSøkereOgBarnidenterForLøpendeFagsakerIInfotrygd()

        if (!erDryRun) {
            val taskerForSakerIInfotrygd =
                brukereMedLøpendeKontantstøtteIInfotrygd.map {
                    OpprettFagsakOgSendInformasjonsbrevKontantstøtteendringTask.lagTask(it)
                }
            taskService.saveAll(taskerForSakerIInfotrygd)
        }

        return brukereMedLøpendeKontantstøtteIInfotrygd
    }
}
