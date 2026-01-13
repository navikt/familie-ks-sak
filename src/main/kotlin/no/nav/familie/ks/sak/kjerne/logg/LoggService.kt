package no.nav.familie.ks.sak.kjerne.logg

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ks.sak.common.util.formaterIdent
import no.nav.familie.ks.sak.common.util.tilKortString
import no.nav.familie.ks.sak.common.util.tilddMMyyyy
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.config.RolleConfig
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.domene.Arbeidsfordelingsenhet
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.Beslutning
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.feilutbetaltvaluta.FeilutbetaltValuta
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.refusjonEøs.RefusjonEøs
import no.nav.familie.ks.sak.kjerne.brev.mottaker.BrevmottakerDb
import no.nav.familie.ks.sak.kjerne.korrigertetterbetaling.KorrigertEtterbetaling
import no.nav.familie.ks.sak.kjerne.logg.domene.Logg
import no.nav.familie.ks.sak.kjerne.logg.domene.LoggRepository
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.korrigertvedtak.KorrigertVedtak
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class LoggService(
    private val loggRepository: LoggRepository,
    private val rolleConfig: RolleConfig,
) {
    private val metrikkPerLoggType: Map<LoggType, Counter> =
        LoggType.entries.associateWith {
            Metrics.counter(
                "behandling.logg",
                "type",
                it.name,
                "beskrivelse",
                it.visningsnavn,
            )
        }

    fun opprettAutovedtakTilManuellBehandling(
        behandling: Behandling,
        tekst: String,
    ) {
        lagreLogg(
            Logg(
                behandlingId = behandling.id,
                type = LoggType.AUTOVEDTAK_TIL_MANUELL_BEHANDLING,
                rolle =
                    SikkerhetContext.hentRolletilgangFraSikkerhetscontext(
                        rolleConfig,
                        BehandlerRolle.SAKSBEHANDLER,
                    ),
                tekst = tekst,
            ),
        )
    }

    private fun lagreLogg(logg: Logg): Logg {
        metrikkPerLoggType[logg.type]?.increment()

        return loggRepository.save(logg)
    }

    fun hentLoggForBehandling(behandlingId: Long): List<Logg> = loggRepository.hentLoggForBehandling(behandlingId)

    fun opprettBehandlendeEnhetEndret(
        behandling: Behandling,
        fraEnhet: Arbeidsfordelingsenhet,
        tilEnhet: ArbeidsfordelingPåBehandling,
        manuellOppdatering: Boolean,
        begrunnelse: String,
    ) {
        lagreLogg(
            Logg(
                behandlingId = behandling.id,
                type = LoggType.BEHANDLENDE_ENHET_ENDRET,
                rolle =
                    SikkerhetContext.hentRolletilgangFraSikkerhetscontext(
                        rolleConfig,
                        BehandlerRolle.SAKSBEHANDLER,
                    ),
                tekst =
                    "Behandlende enhet ${if (manuellOppdatering) "manuelt" else "automatisk"} endret " +
                        "fra ${fraEnhet.enhetId} ${fraEnhet.enhetNavn} " +
                        "til ${tilEnhet.behandlendeEnhetId} ${tilEnhet.behandlendeEnhetNavn}." +
                        if (begrunnelse.isNotBlank()) "\n\n$begrunnelse" else "",
            ),
        )
    }

    fun opprettBehandlingLogg(behandling: Behandling) {
        lagreLogg(
            Logg(
                behandlingId = behandling.id,
                type = LoggType.BEHANDLING_OPPRETTET,
                tittel = "${behandling.type.visningsnavn} opprettet",
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(rolleConfig, BehandlerRolle.SAKSBEHANDLER),
            ),
        )
    }

    fun opprettRegistrertSøknadLogg(
        behandlingId: Long,
        aktivSøknadGrunnlagFinnesFraFør: Boolean,
    ) {
        lagreLogg(
            Logg(
                behandlingId = behandlingId,
                type = LoggType.SØKNAD_REGISTRERT,
                tittel = if (!aktivSøknadGrunnlagFinnesFraFør) "Søknaden ble registrert" else "Søknaden ble endret",
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(rolleConfig, BehandlerRolle.SAKSBEHANDLER),
            ),
        )
    }

    fun opprettMottattDokumentLogg(
        behandling: Behandling,
        tekst: String = "",
        mottattDato: LocalDateTime,
    ) {
        lagreLogg(
            Logg(
                behandlingId = behandling.id,
                type = LoggType.DOKUMENT_MOTTATT,
                tittel = "Dokument mottatt ${mottattDato.toLocalDate().tilKortString()}",
                rolle =
                    SikkerhetContext.hentRolletilgangFraSikkerhetscontext(
                        rolleConfig,
                        BehandlerRolle.SAKSBEHANDLER,
                    ),
                tekst = tekst,
            ),
        )
    }

    fun opprettSettPåVentLogg(
        behandling: Behandling,
        årsak: String,
    ) {
        lagreLogg(
            Logg(
                behandlingId = behandling.id,
                type = LoggType.BEHANDLIG_SATT_PÅ_VENT,
                rolle =
                    SikkerhetContext.hentRolletilgangFraSikkerhetscontext(
                        rolleConfig,
                        BehandlerRolle.SAKSBEHANDLER,
                    ),
                tekst = "Årsak: $årsak",
            ),
        )
    }

    fun opprettSettPåMaskinellVent(
        behandling: Behandling,
        årsak: String,
    ) {
        lagreLogg(
            Logg(
                behandlingId = behandling.id,
                type = LoggType.BEHANDLING_SATT_PÅ_MASKINELL_VENT,
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(rolleConfig, BehandlerRolle.FORVALTER),
                tekst = "Årsak: $årsak",
            ),
        )
    }

    fun opprettTattAvMaskinellVent(behandling: Behandling) {
        lagreLogg(
            Logg(
                behandlingId = behandling.id,
                type = LoggType.BEHANDLING_TATT_AV_MASKINELL_VENT,
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(rolleConfig, BehandlerRolle.FORVALTER),
            ),
        )
    }

    fun opprettOppdaterVentingLogg(
        behandling: Behandling,
        endretFrist: LocalDate?,
        endretÅrsak: String?,
    ) {
        val tekst =
            when {
                endretFrist != null && endretÅrsak != null -> {
                    "Frist og årsak er endret til $endretÅrsak og ${endretFrist.tilKortString()}"
                }

                endretÅrsak != null -> {
                    "Årsak er endret til $endretÅrsak"
                }

                endretFrist != null -> {
                    "Frist er endret til ${endretFrist.tilKortString()}"
                }

                else -> {
                    logger.info("Ingen endringer tilknyttet frist eller årsak på ventende behandling. Oppretter ikke logginnslag.")
                    return
                }
            }

        lagreLogg(
            Logg(
                behandlingId = behandling.id,
                type = LoggType.VENTENDE_BEHANDLING_ENDRET,
                rolle =
                    SikkerhetContext.hentRolletilgangFraSikkerhetscontext(
                        rolleConfig,
                        BehandlerRolle.SAKSBEHANDLER,
                    ),
                tekst = tekst,
            ),
        )
    }

    fun opprettBehandlingGjenopptattLogg(behandling: Behandling) {
        lagreLogg(
            Logg(
                behandlingId = behandling.id,
                type = LoggType.BEHANDLIG_GJENOPPTATT,
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(rolleConfig, BehandlerRolle.SAKSBEHANDLER),
            ),
        )
    }

    fun opprettVilkårsvurderingLogg(
        behandling: Behandling,
        behandlingsForrigeResultat: Behandlingsresultat,
        behandlingsNyResultat: Behandlingsresultat,
    ) {
        val tekst =
            when {
                behandlingsForrigeResultat == Behandlingsresultat.IKKE_VURDERT -> {
                    "Resultat ble ${behandlingsNyResultat.displayName.lowercase()}"
                }

                behandlingsForrigeResultat != behandlingsNyResultat -> {
                    "Resultat gikk fra ${behandlingsForrigeResultat.displayName.lowercase()} til ${behandlingsNyResultat.displayName.lowercase()}"
                }

                else -> {
                    logger.info("Logg kan ikke lagres når $behandlingsForrigeResultat er samme som $behandlingsNyResultat")
                    return
                }
            }
        val tittel =
            when {
                behandlingsForrigeResultat != Behandlingsresultat.IKKE_VURDERT -> "Vilkårsvurdering endret"
                else -> "Vilkårsvurdering gjennomført"
            }

        lagreLogg(
            Logg(
                behandlingId = behandling.id,
                type = LoggType.VILKÅRSVURDERING,
                tittel = tittel,
                rolle =
                    SikkerhetContext.hentRolletilgangFraSikkerhetscontext(
                        rolleConfig,
                        BehandlerRolle.SAKSBEHANDLER,
                    ),
                tekst = tekst,
            ),
        )
    }

    fun opprettBrevIkkeDistribuertUkjentAdresseLogg(
        behandlingId: Long,
        brevnavn: String,
    ) {
        lagreLogg(
            Logg(
                behandlingId = behandlingId,
                type = LoggType.BREV_IKKE_DISTRIBUERT,
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(rolleConfig, BehandlerRolle.SYSTEM),
                tekst = brevnavn,
            ),
        )
    }

    fun opprettDistribuertBrevLogg(
        behandlingId: Long,
        tekst: String,
        rolle: BehandlerRolle,
    ) {
        lagreLogg(
            Logg(
                behandlingId = behandlingId,
                type = LoggType.DISTRIBUERE_BREV,
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(rolleConfig, rolle),
                tekst = tekst,
            ),
        )
    }

    fun opprettBrevIkkeDistribuertUkjentDødsboadresseLogg(
        behandlingId: Long,
        brevnavn: String,
    ) {
        lagreLogg(
            Logg(
                behandlingId = behandlingId,
                type = LoggType.BREV_IKKE_DISTRIBUERT_UKJENT_DØDSBO,
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(rolleConfig, BehandlerRolle.SYSTEM),
                tekst = brevnavn,
            ),
        )
    }

    fun opprettSendTilBeslutterLogg(behandlingId: Long) {
        lagreLogg(
            Logg(
                behandlingId = behandlingId,
                type = LoggType.SEND_TIL_BESLUTTER,
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(rolleConfig, BehandlerRolle.SAKSBEHANDLER),
            ),
        )
    }

    fun opprettHenleggBehandlingLogg(
        behandling: Behandling,
        årsak: String,
        begrunnelse: String,
    ) {
        lagreLogg(
            Logg(
                behandlingId = behandling.id,
                type = LoggType.HENLEGG_BEHANDLING,
                rolle =
                    SikkerhetContext.hentRolletilgangFraSikkerhetscontext(
                        rolleConfig,
                        BehandlerRolle.SAKSBEHANDLER,
                    ),
                tekst = "$årsak: $begrunnelse",
            ),
        )
    }

    fun opprettBeslutningOmVedtakLogg(
        behandling: Behandling,
        beslutning: Beslutning,
        begrunnelse: String?,
    ) {
        val beslutningErGodkjent = beslutning.erGodkjent()

        lagreLogg(
            Logg(
                behandlingId = behandling.id,
                type = LoggType.GODKJENNE_VEDTAK,
                tittel = if (beslutningErGodkjent) "Vedtak godkjent" else "Vedtak underkjent",
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(rolleConfig, BehandlerRolle.BESLUTTER),
                tekst = if (beslutningErGodkjent) "" else "Begrunnelse: $begrunnelse",
                opprettetAv = SikkerhetContext.hentSaksbehandlerNavn(),
            ),
        )
    }

    fun opprettAvsluttBehandlingLogg(behandling: Behandling) {
        lagreLogg(
            Logg(
                behandlingId = behandling.id,
                type = LoggType.FERDIGSTILLE_BEHANDLING,
                rolle = SikkerhetContext.hentRolletilgangFraSikkerhetscontext(rolleConfig, BehandlerRolle.SYSTEM),
            ),
        )
    }

    fun opprettBarnLagtTilLogg(
        behandling: Behandling,
        barn: Person,
    ) {
        val beskrivelse =
            "${barn.navn.uppercase()} (${barn.hentAlder()} år) | " +
                "${formaterIdent(barn.aktør.aktivFødselsnummer())} lagt til"
        lagreLogg(
            Logg(
                behandlingId = behandling.id,
                type = LoggType.BARN_LAGT_TIL,
                rolle =
                    SikkerhetContext.hentRolletilgangFraSikkerhetscontext(
                        rolleConfig,
                        BehandlerRolle.SAKSBEHANDLER,
                    ),
                tekst = beskrivelse,
            ),
        )
    }

    fun opprettKorrigertVedtakLogg(
        behandling: Behandling,
        korrigertVedtak: KorrigertVedtak,
    ) {
        val tekst =
            if (korrigertVedtak.aktiv) {
                """
                Vedtaksdato: ${korrigertVedtak.vedtaksdato.tilddMMyyyy()}
                Begrunnelse: ${korrigertVedtak.begrunnelse ?: "Ingen begrunnelse"}
                """.trimIndent()
            } else {
                ""
            }

        val tittel =
            if (korrigertVedtak.aktiv) {
                "Vedtaket er korrigert etter § 35"
            } else {
                "Korrigering av vedtaket etter § 35 er fjernet"
            }

        lagreLogg(
            Logg(
                behandlingId = behandling.id,
                type = LoggType.KORRIGERT_VEDTAK,
                rolle =
                    SikkerhetContext.hentRolletilgangFraSikkerhetscontext(
                        rolleConfig,
                        BehandlerRolle.SAKSBEHANDLER,
                    ),
                tittel = tittel,
                tekst = tekst,
            ),
        )
    }

    fun opprettEndretBehandlingstemaLogg(
        behandling: Behandling,
        forrigeKategori: BehandlingKategori,
        nyKategori: BehandlingKategori,
    ) = lagreLogg(
        Logg(
            behandlingId = behandling.id,
            type = LoggType.BEHANDLINGSTEMA_ENDRET,
            rolle =
                SikkerhetContext.hentRolletilgangFraSikkerhetscontext(
                    rolleConfig,
                    BehandlerRolle.SAKSBEHANDLER,
                ),
            tekst = "Behandlingstema er manuelt endret fra $forrigeKategori ordinær til $nyKategori ordinær",
        ),
    )

    fun opprettFeilutbetaltValutaLagtTilLogg(feilutbetaltValuta: FeilutbetaltValuta) =
        lagreLogg(
            Logg(
                behandlingId = feilutbetaltValuta.behandlingId,
                type = LoggType.FEILUTBETALT_VALUTA_LAGT_TIL,
                rolle =
                    SikkerhetContext.hentRolletilgangFraSikkerhetscontext(
                        rolleConfig,
                        BehandlerRolle.SAKSBEHANDLER,
                    ),
                tekst =
                    """
                    Periode: ${feilutbetaltValuta.fom.tilKortString()} - ${feilutbetaltValuta.tom.tilKortString()}
                    Beløp: ${feilutbetaltValuta.feilutbetaltBeløp} kr
                    """.trimIndent(),
            ),
        )

    fun opprettFeilutbetaltValutaFjernetLogg(feilutbetaltValuta: FeilutbetaltValuta) =
        lagreLogg(
            Logg(
                behandlingId = feilutbetaltValuta.behandlingId,
                type = LoggType.FEILUTBETALT_VALUTA_FJERNET,
                rolle =
                    SikkerhetContext.hentRolletilgangFraSikkerhetscontext(
                        rolleConfig,
                        BehandlerRolle.SAKSBEHANDLER,
                    ),
                tekst =
                    """
                    Periode: ${feilutbetaltValuta.fom.tilKortString()} - ${feilutbetaltValuta.tom.tilKortString()}
                    Beløp: ${feilutbetaltValuta.feilutbetaltBeløp} kr
                    """.trimIndent(),
            ),
        )

    fun loggRefusjonEøsPeriodeLagtTil(refusjonEøs: RefusjonEøs) =
        lagreLogg(
            Logg(
                behandlingId = refusjonEøs.behandlingId,
                type = LoggType.REFUSJON_EØS_LAGT_TIL,
                rolle =
                    SikkerhetContext.hentRolletilgangFraSikkerhetscontext(
                        rolleConfig,
                        BehandlerRolle.SAKSBEHANDLER,
                    ),
                tekst =
                    """
                    Periode: ${refusjonEøs.fom.tilKortString()} - ${refusjonEøs.tom.tilKortString()}
                    Beløp: ${refusjonEøs.refusjonsbeløp} kr/mnd
                    """.trimIndent(),
            ),
        )

    fun loggRefusjonEøsPeriodeFjernet(refusjonEøs: RefusjonEøs) =
        lagreLogg(
            Logg(
                behandlingId = refusjonEøs.behandlingId,
                type = LoggType.REFUSJON_EØS_FJERNET,
                rolle =
                    SikkerhetContext.hentRolletilgangFraSikkerhetscontext(
                        rolleConfig,
                        BehandlerRolle.SAKSBEHANDLER,
                    ),
                tekst =
                    """
                    Periode: ${refusjonEøs.fom.tilKortString()} - ${refusjonEøs.tom.tilKortString()}
                    Beløp: ${refusjonEøs.refusjonsbeløp} kr/mnd
                    """.trimIndent(),
            ),
        )

    fun opprettKorrigertEtterbetalingLogg(
        behandling: Behandling,
        korrigertEtterbetaling: KorrigertEtterbetaling,
    ) {
        val tekst =
            if (korrigertEtterbetaling.aktiv) {
                """
                Årsak: ${korrigertEtterbetaling.årsak.visningsnavn}
                Nytt beløp: ${korrigertEtterbetaling.beløp} kr
                Begrunnelse: ${korrigertEtterbetaling.begrunnelse ?: "Ingen begrunnelse"}
                """.trimIndent()
            } else {
                ""
            }

        val tittel =
            if (korrigertEtterbetaling.aktiv) {
                "Etterbetaling i brev er korrigert"
            } else {
                "Korrigert etterbetaling er angret"
            }

        lagreLogg(
            Logg(
                behandlingId = behandling.id,
                type = LoggType.KORRIGERT_ETTERBETALING,
                rolle =
                    SikkerhetContext.hentRolletilgangFraSikkerhetscontext(
                        rolleConfig,
                        BehandlerRolle.SAKSBEHANDLER,
                    ),
                tittel = tittel,
                tekst = tekst,
            ),
        )
    }

    fun opprettBrevmottakerLogg(
        brevmottaker: BrevmottakerDb,
        brevmottakerFjernet: Boolean,
    ) {
        val lagtTilEllerFjernet = if (brevmottakerFjernet) "fjernet" else "lagt til"
        val tittel = "${brevmottaker.type.visningsnavn} er $lagtTilEllerFjernet som brevmottaker"

        val tekst =
            listOfNotNull(
                brevmottaker.navn,
                brevmottaker.adresselinje1,
                brevmottaker.adresselinje2,
                brevmottaker.postnummer,
                brevmottaker.poststed,
                brevmottaker.landkode,
            ).joinToString(separator = System.lineSeparator())

        lagreLogg(
            Logg(
                behandlingId = brevmottaker.behandlingId,
                type = LoggType.BREVMOTTAKER_LAGT_TIL_ELLER_FJERNET,
                rolle =
                    SikkerhetContext.hentRolletilgangFraSikkerhetscontext(
                        rolleConfig,
                        BehandlerRolle.SAKSBEHANDLER,
                    ),
                tittel = tittel,
                tekst = tekst,
            ),
        )
    }

    fun opprettSammensattKontrollsakOpprettetLogg(
        behandlingId: Long,
    ) {
        lagreLogg(
            Logg(
                behandlingId = behandlingId,
                type = LoggType.SAMMENSATT_KONTROLLSAK_OPPRETTET,
                rolle =
                    SikkerhetContext.hentRolletilgangFraSikkerhetscontext(
                        rolleConfig,
                        BehandlerRolle.SAKSBEHANDLER,
                    ),
                tittel = LoggType.SAMMENSATT_KONTROLLSAK_OPPRETTET.tittel,
                tekst = "En sammensatt kontrollsak har blitt opprettet",
            ),
        )
    }

    fun opprettSammensattKontrollsakOppdatertLogg(
        behandlingId: Long,
    ) {
        lagreLogg(
            Logg(
                behandlingId = behandlingId,
                type = LoggType.SAMMENSATT_KONTROLLSAK_OPPDATERT,
                rolle =
                    SikkerhetContext.hentRolletilgangFraSikkerhetscontext(
                        rolleConfig,
                        BehandlerRolle.SAKSBEHANDLER,
                    ),
                tittel = LoggType.SAMMENSATT_KONTROLLSAK_OPPDATERT.tittel,
                tekst = "En sammensatt kontrollsak har blitt oppdatert",
            ),
        )
    }

    fun opprettSammensattKontrollsakSlettetLogg(
        behandlingId: Long,
    ) {
        lagreLogg(
            Logg(
                behandlingId = behandlingId,
                type = LoggType.SAMMENSATT_KONTROLLSAK_SLETTET,
                rolle =
                    SikkerhetContext.hentRolletilgangFraSikkerhetscontext(
                        rolleConfig,
                        BehandlerRolle.SAKSBEHANDLER,
                    ),
                tittel = LoggType.SAMMENSATT_KONTROLLSAK_SLETTET.tittel,
                tekst = "En sammensatt kontrollsak har blitt slettet",
            ),
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LoggService::class.java)
    }
}
