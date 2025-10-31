package no.nav.familie.ks.sak.cucumber.mocking

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.config.TaskRepositoryWrapper

fun mockTaskService(): TaskRepositoryWrapper {
    val taskService = mockk<TaskRepositoryWrapper>()
    every { taskService.save(any()) } answers { firstArg() }
    return taskService
}
