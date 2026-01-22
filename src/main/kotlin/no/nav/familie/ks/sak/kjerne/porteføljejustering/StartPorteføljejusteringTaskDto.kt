package no.nav.familie.ks.sak.kjerne.porteføljejustering

data class StartPorteføljejusteringTaskDto(
    val antallTasks: Int? = null,
    val behandlesAvApplikasjon: String? = null,
    val dryRun: Boolean = true,
)
