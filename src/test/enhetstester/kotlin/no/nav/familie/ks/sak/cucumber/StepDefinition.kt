package no.nav.familie.ks.sak.cucumber

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Og
import io.cucumber.java.no.Så
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.cucumber.domeneparser.Domenebegrep
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.ks.sak.api.dto.BarnMedOpplysningerDto
import no.nav.familie.ks.sak.api.dto.SøkerMedOpplysningerDto
import no.nav.familie.ks.sak.api.dto.SøknadDto
import no.nav.familie.ks.sak.common.domeneparser.VedtaksperiodeMedBegrunnelserParser
import no.nav.familie.ks.sak.common.domeneparser.VedtaksperiodeMedBegrunnelserParser.mapForventetVedtaksperioderMedBegrunnelser
import no.nav.familie.ks.sak.common.domeneparser.parseDato
import no.nav.familie.ks.sak.common.domeneparser.parseLong
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.registrersøknad.SøknadGrunnlagService
import no.nav.familie.ks.sak.kjerne.behandling.steg.registrersøknad.domene.SøknadGrunnlag
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.utbetalingsperiodeMedBegrunnelser.UtbetalingsperiodeMedBegrunnelserService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.ks.sak.kjerne.beregning.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ks.sak.kjerne.beregning.EndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.TilkjentYtelseUtils
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.tilAndelerTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.beregning.tilEndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ks.sak.kjerne.eøs.utenlandskperiodebeløp.domene.UtenlandskPeriodebeløp
import no.nav.familie.ks.sak.kjerne.eøs.valutakurs.domene.Valutakurs
import no.nav.familie.ks.sak.kjerne.fagsak.domene.Fagsak
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Målform
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import org.assertj.core.api.Assertions
import java.time.LocalDate

@Suppress("ktlint:standard:function-naming")
class StepDefinition {
    var fagsaker: Map<Long, Fagsak> = emptyMap()
    var behandlinger = mutableMapOf<Long, Behandling>()
    var behandlingTilForrigeBehandling = mutableMapOf<Long, Long?>()
    var vedtakListe = mutableListOf<Vedtak>()
    var persongrunnlag = mutableMapOf<Long, PersonopplysningGrunnlag>()
    var vilkårsvurdering = mutableMapOf<Long, Vilkårsvurdering>()
    var vedtaksperioderMedBegrunnelser = mutableMapOf<Long, List<VedtaksperiodeMedBegrunnelser>>()
    var kompetanser = mutableMapOf<Long, List<Kompetanse>>()
    var valutakurs = mutableMapOf<Long, List<Valutakurs>>()
    var utenlandskPeriodebeløp = mutableMapOf<Long, List<UtenlandskPeriodebeløp>>()
    var endredeUtbetalinger = mutableMapOf<Long, List<EndretUtbetalingAndel>>()
    var andelerTilkjentYtelse = mutableMapOf<Long, List<AndelTilkjentYtelse>>()
    var overstyrteEndringstidspunkt = mutableMapOf<Long, LocalDate>()
    var utvidetVedtaksperiodeMedBegrunnelser = listOf<UtvidetVedtaksperiodeMedBegrunnelser>()
    var uregistrerteBarn = mutableMapOf<Long, List<BarnMedOpplysningerDto>>()
    var målform: Målform = Målform.NB
    var søknadstidspunkt: LocalDate? = null

    var dagensDato: LocalDate = LocalDate.now()

    // every { behandlingRepository.finnIverksatteBehandlinger(Any()) } returns behandling
    // every { personopplysningGrunnlagService.finnAktivPersonopplysningGrunnlag(any()) } returns hentStatusRespons

    /**
     * Mulige verdier: | FagsakId |
     */
    @Gitt("følgende fagsaker")
    fun `følgende fagsaker  `(dataTable: DataTable) {
        fagsaker = lagFagsaker(dataTable) + fagsaker
    }

    /**
     * Mulige felter:
     * | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak |
     */
    @Og("følgende behandlinger")
    fun `følgende behandling`(dataTable: DataTable) {
        val nyeVedtak =
            lagVedtakListe(
                dataTable = dataTable,
                behandlinger = behandlinger,
                behandlingTilForrigeBehandling = behandlingTilForrigeBehandling,
                fagsaker = fagsaker,
            )

        vedtakListe.addAll(nyeVedtak)
    }

    /**
     * Mulige verdier: | BehandlingId |  AktørId | Persontype | Fødselsdato |
     */
    @Og("følgende persongrunnlag")
    fun `følgende persongrunnlag`(dataTable: DataTable) {
        val nyePersongrunnlag = lagPersonGrunnlag(dataTable)
        persongrunnlag.putAll(nyePersongrunnlag)
    }

    /**
     * Mulige verdier: | Ident | Fødselsdato | Er inkludert i søknaden | Er folkeregistrert |
     */
    @Og("med uregistrerte barn for behandling {}")
    fun `med uregistrerte barn`(
        behandlingId: Long,
        dataTable: DataTable,
    ) {
        uregistrerteBarn[behandlingId] = lagUregistrerteBarn(dataTable)
    }

    @Og("følgende dagens dato {}")
    fun `følgende dagens dato`(dagensDatoString: String) {
        dagensDato = parseDato(dagensDatoString)
    }

    /**
     * Mulige verdier: | AktørId | Vilkår | Utdypende vilkår | Fra dato | Til dato | Resultat | Er eksplisitt avslag | Vurderes etter |
     */
    @Og("følgende vilkårresultater for behandling {}")
    fun `legg til nye vilkårresultater for behandling`(
        behandlingId: Long,
        dataTable: DataTable,
    ) {
        vilkårsvurdering[behandlingId] = lagVilkårsvurdering(dataTable, this, behandlingId)
    }

    /**
     * Mulige verdier: | AktørId | Fra dato | Til dato | BehandlingId |  Årsak | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |
     */
    @Og("med endrede utbetalinger")
    fun `med endrede utbetalinger`(
        dataTable: DataTable,
    ) {
        endredeUtbetalinger = lagEndredeUtbetalinger(dataTable.asMaps(), persongrunnlag)
    }

    /**
     * Mulige felt:
     * | AktørId | Fra dato | Til dato | Resultat | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
     */
    @Og("med kompetanser")
    fun `med kompetanser  `(
        behandlingId: Long,
        dataTable: DataTable,
    ) {
        kompetanser = lagKompetanser(dataTable.asMaps(), persongrunnlag)
    }

    /**
     * Mulige felt:
     * | AktørId | Fra dato | Til dato | BehandlingId | Valutakursdato | Valuta kode | Kurs
     */
    @Og("med valutakurs")
    fun `med valutakurs`(
        dataTable: DataTable,
    ) {
        valutakurs = lagValutakurs(dataTable.asMaps(), persongrunnlag)
    }

    /**
     * Mulige felt:
     * | AktørId | Fra dato | Til dato | BehandlingId | Beløp | Valuta kode | Intervall | Utbetalingsland
     */
    @Og("med utenlandsk periodebeløp")
    fun `med utenlandsk periodebeløp`(
        dataTable: DataTable,
    ) {
        utenlandskPeriodebeløp = lagUtenlandskperiodeBeløp(dataTable.asMaps(), persongrunnlag)
    }

    @Og("andeler er beregnet for behandling {}")
    fun `andeler er beregnet`(
        behandlingId: Long,
    ) {
        andelerTilkjentYtelse[behandlingId] =
            TilkjentYtelseUtils.beregnTilkjentYtelse(
                vilkårsvurdering = vilkårsvurdering[behandlingId]!!,
                personopplysningGrunnlag = persongrunnlag[behandlingId]!!,
                endretUtbetalingAndeler = endredeUtbetalinger[behandlingId]?.map { EndretUtbetalingAndelMedAndelerTilkjentYtelse(it, emptyList()) } ?: emptyList(),
            ).andelerTilkjentYtelse.toList()
    }

    /**
     * Mulige verdier: | AktørId | Fra dato | Til dato | Beløp | Ytelse type | Prosent | Sats |
     */
    @Så("forvent følgende andeler tilkjent ytelse for behandling {}")
    fun `med andeler tilkjent ytelse`(
        behandlingId: Long,
        dataTable: DataTable,
    ) {
        val beregnetTilkjentYtelse = andelerTilkjentYtelse[behandlingId]!!
        val forventedeAndeler = lagAndelerTilkjentYtelse(dataTable, behandlingId, behandlinger, persongrunnlag)

        Assertions.assertThat(beregnetTilkjentYtelse)
            .usingRecursiveComparison().ignoringFieldsMatchingRegexes(".*endretTidspunkt", ".*opprettetTidspunkt", ".*kildeBehandlingId", ".*tilkjentYtelse")
            .isEqualTo(forventedeAndeler)
    }

    /**
     * Mulige verdier: | BehandlingId | Endringstidspunkt |
     */
    @Og("med overstyrt endringstidspunkt")
    fun settEndringstidspunkt(dataTable: DataTable) {
        overstyrteEndringstidspunkt =
            (
                overstyrteEndringstidspunkt +
                    dataTable.asMaps().associate { rad ->
                        parseLong(Domenebegrep.BEHANDLING_ID, rad) to
                            parseDato(VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser.ENDRINGSTIDSPUNKT, rad)
                    }
            ).toMutableMap()
    }

    @Og("vedtaksperioder er laget for behandling {}")
    fun `vedtaksperioder er laget for behandling`(behandlingId: Long) {
        val forrigeBehandling = behandlinger[behandlingTilForrigeBehandling[behandlingId]]

        val andelerTilkjentYtelseDenneBehandlingen = andelerTilkjentYtelse[behandlingId]!!
        val endredeUtbetalingerDenneBehandlingen = endredeUtbetalinger[behandlingId] ?: emptyList()

        val andelerTilkjentYtelseForrigeBehandling = andelerTilkjentYtelse[forrigeBehandling?.id] ?: emptyList()
        val endredeUtbetalingerForrigeBehandling = endredeUtbetalinger[forrigeBehandling?.id] ?: emptyList()

        vedtaksperioderMedBegrunnelser[behandlingId] =
            mockVedtaksperiodeService().genererVedtaksperioderMedBegrunnelser(
                vedtak = vedtakListe.single { it.behandling.id == behandlingId },
                manueltOverstyrtEndringstidspunkt = overstyrteEndringstidspunkt[behandlingId],
                gjelderFortsattInnvilget = false,
            )
    }

    /**
     * Mulige verdier: | Fra dato | Til dato | Vedtaksperiodetype |
     */
    @Så("forvent følgende vedtaksperioder på behandling {}")
    fun `forvent følgende vedtaksperioder med begrunnelser`(
        behandlingId: Long,
        dataTable: DataTable,
    ) {
        val forventedeVedtaksperioder =
            mapForventetVedtaksperioderMedBegrunnelser(
                dataTable = dataTable,
                vedtak =
                    vedtakListe.find { it.behandling.id == behandlingId }
                        ?: throw Feil("Fant ingen vedtak for behandling $behandlingId"),
            )

        val vedtaksperioderComparator = compareBy<VedtaksperiodeMedBegrunnelser>({ it.type }, { it.fom }, { it.tom })
        Assertions.assertThat(vedtaksperioderMedBegrunnelser[behandlingId]!!.sortedWith(vedtaksperioderComparator))
            .usingRecursiveComparison().ignoringFieldsMatchingRegexes(".*endretTidspunkt", ".*opprettetTidspunkt")
            .isEqualTo(forventedeVedtaksperioder.sortedWith(vedtaksperioderComparator))
    }

    fun mockVedtaksperiodeService(): VedtaksperiodeService {
        val behandlingRepository = mockk<BehandlingRepository>()
        every { behandlingRepository.finnIverksatteBehandlinger(any<Long>()) } answers {
            behandlinger.values.filter { behandling -> behandling.fagsak.id == firstArg<Long>() }
        }
        every { behandlingRepository.finnBehandlinger(any<Long>()) } answers {
            behandlinger.values.filter { behandling -> behandling.fagsak.id == firstArg<Long>() }
        }

        val personopplysningGrunnlagService = mockk<PersonopplysningGrunnlagService>()
        every { personopplysningGrunnlagService.finnAktivPersonopplysningGrunnlag(any<Long>()) } answers {
            persongrunnlag[firstArg()]
        }

        val andelerTilkjentYtelseOgEndreteUtbetalingerService = mockk<AndelerTilkjentYtelseOgEndreteUtbetalingerService>()
        every { andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(any<Long>()) } answers {
            val behandlingId = firstArg<Long>()
            andelerTilkjentYtelse[behandlingId]?.tilAndelerTilkjentYtelseMedEndreteUtbetalinger(endredeUtbetalinger[behandlingId] ?: emptyList()) ?: emptyList()
        }
        every { andelerTilkjentYtelseOgEndreteUtbetalingerService.finnEndreteUtbetalingerMedAndelerTilkjentYtelse(any<Long>()) } answers {
            val behandlingId = firstArg<Long>()
            endredeUtbetalinger[behandlingId]?.tilEndretUtbetalingAndelMedAndelerTilkjentYtelse(andelerTilkjentYtelse[behandlingId] ?: emptyList()) ?: emptyList()
        }

        val utbetalingsperiodeMedBegrunnelserService = mockk<UtbetalingsperiodeMedBegrunnelserService>()
        every { utbetalingsperiodeMedBegrunnelserService.hentUtbetalingsperioder(any<Vedtak>(), any()) } answers {
            val vedtak = firstArg<Vedtak>()
            vedtaksperioderMedBegrunnelser[vedtak.behandling.id] ?: emptyList()
        }

        val vilkårsvurderingRepository = mockk<VilkårsvurderingRepository>()
        every { vilkårsvurderingRepository.finnAktivForBehandling(any<Long>()) } answers {
            val behandlingId = firstArg<Long>()
            vilkårsvurdering[behandlingId]
        }
        val søknadGrunnlagService = mockk<SøknadGrunnlagService>()
        every { søknadGrunnlagService.hentAktiv(any<Long>()) } answers {
            val behandlingId = firstArg<Long>()
            val søknadDtoString =
                objectMapper.writeValueAsString(
                    SøknadDto(
                        barnaMedOpplysninger = uregistrerteBarn[behandlingId] ?: emptyList(),
                        endringAvOpplysningerBegrunnelse = "",
                        søkerMedOpplysninger = SøkerMedOpplysningerDto(ident = "", målform = målform),
                    ),
                )
            SøknadGrunnlag(behandlingId = behandlingId, søknad = søknadDtoString)
        }
        return VedtaksperiodeService(
            behandlingRepository = behandlingRepository,
            personopplysningGrunnlagService = personopplysningGrunnlagService,
            vedtaksperiodeHentOgPersisterService = mockk(),
            vedtakRepository = mockk(),
            vilkårsvurderingRepository = vilkårsvurderingRepository,
            sanityService = mockk(),
            søknadGrunnlagService = søknadGrunnlagService,
            utbetalingsperiodeMedBegrunnelserService = utbetalingsperiodeMedBegrunnelserService,
            andelerTilkjentYtelseOgEndreteUtbetalingerService = andelerTilkjentYtelseOgEndreteUtbetalingerService,
            integrasjonClient = mockk(),
            refusjonEøsRepository = mockk(),
            kompetanseService = mockk(),
        )
    }
}
