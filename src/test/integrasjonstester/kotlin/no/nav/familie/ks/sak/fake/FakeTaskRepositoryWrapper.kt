package no.nav.familie.ks.sak.fake

import no.nav.familie.ks.sak.config.TaskRepositoryWrapper
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService

class FakeTaskRepositoryWrapper(
    taskService: TaskService,
) : TaskRepositoryWrapper(taskService) {
    val lagredeTasker = mutableListOf<Task>()

    override fun save(task: Task): Task {
        lagredeTasker.add(task)
        return task
    }

    override fun findAll(): List<Task> = lagredeTasker

    fun reset() {
        lagredeTasker.clear()
    }
}
