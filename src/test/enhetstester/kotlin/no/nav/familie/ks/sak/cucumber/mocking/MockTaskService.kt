package no.nav.familie.ks.sak.cucumber.mocking

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.prosessering.internal.TaskService

fun mockTaskService(): TaskService {
    val taskService = mockk<TaskService>()
    every { taskService.save(any()) } answers { firstArg() }
    return taskService
}
