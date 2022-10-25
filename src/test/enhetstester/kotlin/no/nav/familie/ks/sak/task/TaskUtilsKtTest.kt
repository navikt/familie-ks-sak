package no.nav.familie.ks.sak.task

import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import org.hamcrest.CoreMatchers.`is` as Is

internal class TaskUtilsKtTest {

    @Test
    fun `nesteGyldigeTriggertidForBehandlingIHverdager skal returnere neste triggertid basert på nåværende tid`() {
        val tid = LocalDateTime.of(2022, 10, 24, 0, 0)
        val tid2 = LocalDateTime.of(2022, 10, 28, 12, 30)
        val tid3 = LocalDateTime.of(2022, 10, 29, 21, 30)

        val nesteTriggerTid = nesteGyldigeTriggertidForBehandlingIHverdager(50, tid)
        val nesteTriggerTid2 = nesteGyldigeTriggertidForBehandlingIHverdager(50, tid2)
        val nesteTriggerTid3 = nesteGyldigeTriggertidForBehandlingIHverdager(50, tid3)

        assertThat(nesteTriggerTid, Is(LocalDateTime.of(2022, 10, 24, 6, 50)))
        assertThat(nesteTriggerTid2, Is(LocalDateTime.of(2022, 10, 28, 13, 20)))
        assertThat(nesteTriggerTid3, Is(LocalDateTime.of(2022, 10, 31, 6, 20)))
    }
}
