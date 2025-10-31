package no.nav.familie.ks.sak.fake

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.ks.sak.config.TaskRepositoryWrapper
import no.nav.familie.prosessering.domene.Status
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

    override fun findByStatus(status: Status): List<Task> = lagredeTasker.filter { it.status == status }

    fun hentLagredeTaskerAvType(type: String): List<Task> = this.lagredeTasker.filter { it.type == type }

    fun reset() {
        lagredeTasker.clear()
    }
}

inline fun <reified T> List<Task>.tilPayload(): List<T> = this.map { objectMapper.readValue(it.payload) }
