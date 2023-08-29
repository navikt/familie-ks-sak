package no.nav.familie.ks.sak.kjerne.behandling.domene

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Service
class BehandlingSøknadsinfoService(
    private val behandlingSøknadsinfoRepository: BehandlingSøknadsinfoRepository,
) {

    @Transactional
    fun lagreNedSøknadsinfo(søknadsinfo: Søknadsinfo, behandling: Behandling) {
        val behandlingSøknadsinfo = BehandlingSøknadsinfo(
            behandling = behandling,
            mottattDato = søknadsinfo.mottattDato,
            journalpostId = søknadsinfo.journalpostId,
            erDigital = søknadsinfo.erDigital,
        )
        behandlingSøknadsinfoRepository.save(behandlingSøknadsinfo)
    }

    fun hentSøknadsstatistikk(fom: LocalDate, tom: LocalDate): SøknadsstatistikkForPeriode =
        behandlingSøknadsinfoRepository.hentAntallSøknaderIPeriode(fom.atStartOfDay(), tom.atTime(LocalTime.MAX))
            .let {
                SøknadsstatistikkForPeriode(
                    fom = fom,
                    tom = tom,
                    antallSøknader = it,
                    digitaliseringsgrad = (it.digitalt ?: 0) / it.totalt.toFloat(),
                )
            }
}

class Søknadsinfo(
    val journalpostId: String,
    val mottattDato: LocalDateTime,
    val erDigital: Boolean,
)

class SøknadsstatistikkForPeriode(
    val fom: LocalDate,
    val tom: LocalDate,
    val antallSøknader: AntallSøknader,
    val digitaliseringsgrad: Float,
)
