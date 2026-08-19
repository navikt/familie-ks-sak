package no.nav.familie.ks.sak.kjerne.eøs.valutakurs

import io.mockk.MockKException
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ks.sak.api.dto.UtfyltStatus
import no.nav.familie.ks.sak.api.dto.ValutakursDto
import no.nav.familie.ks.sak.api.dto.tilValutakurs
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ks.sak.data.tilfeldigPerson
import no.nav.familie.ks.sak.integrasjon.ecb.ECBService
import no.nav.familie.ks.sak.integrasjon.norgesbank.NorgesBankService
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class ValutakursControllerTest {
    private val valutakursService = mockk<ValutakursService>()
    private val personidentService = mockk<PersonidentService>()
    private val behandlingService = mockk<BehandlingService>()
    private val ecbService = mockk<ECBService>()
    private val norgesBankService = mockk<NorgesBankService>()
    private val tilgangService = mockk<TilgangService>()
    private val featureToggleService = mockk<FeatureToggleService>()

    private val valutakursController =
        ValutakursController(
            tilgangService = tilgangService,
            valutakursService = valutakursService,
            personidentService = personidentService,
            behandlingService = behandlingService,
            ecbService = ecbService,
            norgesBankService = norgesBankService,
            featureToggleService = featureToggleService,
        )

    private val barnId = tilfeldigPerson()

    private val restValutakurs: ValutakursDto =
        ValutakursDto(1, YearMonth.of(2020, 1), null, listOf(barnId.aktør.aktivFødselsnummer()), null, null, null, UtfyltStatus.OK)

    @BeforeEach
    fun setup() {
        every { personidentService.hentAktør(any()) } returns barnId.aktør
        every { valutakursService.hentValutakurs(any()) } returns restValutakurs.tilValutakurs(listOf(barnId.aktør))
        every { norgesBankService.hentValutakurs(any(), any()) } returns BigDecimal.valueOf(0.95)
        every { featureToggleService.isEnabled(FeatureToggle.HENT_VALUTAKURS_FRA_NORGESBANK, false) } returns true
        justRun { tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(any(), any(), any(), any()) }
    }

    @Test
    fun `Test at valutakurs hentes fra Norges Bank dersom dato og valuta er satt`() {
        // Arrange
        val valutakursDato = LocalDate.of(2022, 1, 1)
        val valuta = "SEK"

        // Act & Assert
        assertThrows<MockKException> {
            valutakursController.oppdaterValutakurs(
                1,
                restValutakurs.copy(valutakursdato = valutakursDato, valutakode = valuta),
            )
        }
        verify(exactly = 1) { norgesBankService.hentValutakurs("SEK", valutakursDato) }
        verify(exactly = 1) { valutakursService.oppdaterValutakurs(any(), any()) }
    }

    @Test
    fun `Test at valutakurs ikke hentes fra Norges Bank dersom dato ikke er satt`() {
        // Arrange
        val valutakursDato = LocalDate.of(2022, 1, 1)

        // Act & Assert
        assertThrows<MockKException> {
            valutakursController.oppdaterValutakurs(
                1,
                restValutakurs.copy(valutakode = "SEK"),
            )
        }
        verify(exactly = 0) { norgesBankService.hentValutakurs("SEK", valutakursDato) }
        verify(exactly = 1) { valutakursService.oppdaterValutakurs(any(), any()) }
    }

    @Test
    fun `Test at valutakurs ikke hentes fra NorgesBank dersom valuta ikke er satt`() {
        // Arrange
        val valutakursDato = LocalDate.of(2022, 1, 1)

        // Act & Assert
        assertThrows<MockKException> {
            valutakursController.oppdaterValutakurs(
                1,
                restValutakurs.copy(valutakursdato = valutakursDato),
            )
        }
        verify(exactly = 0) { norgesBankService.hentValutakurs("SEK", valutakursDato) }
        verify(exactly = 1) { valutakursService.oppdaterValutakurs(any(), any()) }
    }

    @Test
    fun `Test at valutakurs ikke hentes fra Norges Bank dersom ISK og før 1 feb 2018`() {
        // Arrange
        val valutakursDato = LocalDate.of(2018, 1, 31)

        // Act & Assert
        assertThrows<MockKException> {
            valutakursController.oppdaterValutakurs(
                1,
                restValutakurs.copy(valutakursdato = valutakursDato, valutakode = "ISK"),
            )
        }
        verify(exactly = 0) { norgesBankService.hentValutakurs("ISK", valutakursDato) }
        verify(exactly = 1) { valutakursService.oppdaterValutakurs(any(), any()) }
    }

    @Test
    fun `Test at valutakurs hentes fra Norges Bank dersom ISK og etter 1 feb 2018`() {
        // Arrange
        val valutakursDato = LocalDate.of(2018, 2, 1)

        // Act & Assert
        assertThrows<MockKException> {
            valutakursController.oppdaterValutakurs(
                1,
                restValutakurs.copy(valutakursdato = valutakursDato, valutakode = "ISK"),
            )
        }
        verify(exactly = 1) { norgesBankService.hentValutakurs("ISK", valutakursDato) }
        verify(exactly = 1) { valutakursService.oppdaterValutakurs(any(), any()) }
    }
}
