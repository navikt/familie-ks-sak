package no.nav.familie.ks.sak.cucumber

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Og
import io.cucumber.java.no.Så
import io.mockk.every
import io.mockk.mockk
import mockAdopsjonService
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.ks.sak.api.dto.BarnMedOpplysningerDto
import no.nav.familie.ks.sak.api.dto.SøkerMedOpplysningerDto
import no.nav.familie.ks.sak.api.dto.SøknadDto
import no.nav.familie.ks.sak.common.BehandlingId
import no.nav.familie.ks.sak.common.domeneparser.Domenebegrep
import no.nav.familie.ks.sak.common.domeneparser.VedtaksperiodeMedBegrunnelserParser
import no.nav.familie.ks.sak.common.domeneparser.VedtaksperiodeMedBegrunnelserParser.mapForventetVedtaksperioderMedBegrunnelser
import no.nav.familie.ks.sak.common.domeneparser.parseDato
import no.nav.familie.ks.sak.common.domeneparser.parseLong
import no.nav.familie.ks.sak.common.domeneparser.parseValgfriDato
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.util.LocalDateProvider
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.common.util.tilddMMyyyy
import no.nav.familie.ks.sak.cucumber.BrevBegrunnelseParser.mapBegrunnelser
import no.nav.familie.ks.sak.cucumber.mocking.CucumberMock
import no.nav.familie.ks.sak.cucumber.mocking.mockUnleashNextMedContextService
import no.nav.familie.ks.sak.data.lagVedtak
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelseDto
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.behandlingsresultat.BehandlingsresultatService
import no.nav.familie.ks.sak.kjerne.behandling.steg.registrersøknad.SøknadGrunnlagService
import no.nav.familie.ks.sak.kjerne.behandling.steg.registrersøknad.domene.SøknadGrunnlag
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.tilUtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.utbetalingsperiodeMedBegrunnelser.UtbetalingsperiodeMedBegrunnelserService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.ks.sak.kjerne.beregning.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ks.sak.kjerne.beregning.EndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.beregning.tilAndelerTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.beregning.tilEndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.brev.BrevPeriodeContext
import no.nav.familie.ks.sak.kjerne.brev.LANDKODER
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.BegrunnelseDtoMedData
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.BegrunnelserForPeriodeContext
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriodeDto
import no.nav.familie.ks.sak.kjerne.endretutbetaling.EndretUtbetalingAndelService
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.eøs.differanseberegning.beregnDifferanse
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.UtfyltKompetanse
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.tilIKompetanse
import no.nav.familie.ks.sak.kjerne.eøs.utenlandskperiodebeløp.domene.UtenlandskPeriodebeløp
import no.nav.familie.ks.sak.kjerne.eøs.valutakurs.domene.Valutakurs
import no.nav.familie.ks.sak.kjerne.fagsak.domene.Fagsak
import no.nav.familie.ks.sak.kjerne.overgangsordning.domene.OvergangsordningAndel
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Målform
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import org.assertj.core.api.Assertions.assertThat
import java.time.LocalDate

val sanityBegrunnelserMock = SanityBegrunnelseMock.hentSanityBegrunnelserMock()

@Suppress("ktlint:standard:function-naming")
class StepDefinition {
    var fagsaker: Map<Long, Fagsak> = emptyMap()
    var behandlinger = mutableMapOf<Long, Behandling>()
    var behandlingTilForrigeBehandling = mapOf<Long, Long?>()
    var personopplysningGrunnlagMap = mutableMapOf<Long, PersonopplysningGrunnlag>()
    var vilkårsvurdering = mutableMapOf<Long, Vilkårsvurdering>()
    var vedtaksperioderMedBegrunnelser = mutableMapOf<Long, List<VedtaksperiodeMedBegrunnelser>>()
    var kompetanser = mutableMapOf<Long, List<Kompetanse>>()
    var valutakurs = mutableMapOf<Long, List<Valutakurs>>()
    var utenlandskPeriodebeløp = mutableMapOf<Long, List<UtenlandskPeriodebeløp>>()
    var endredeUtbetalinger = mutableMapOf<Long, List<EndretUtbetalingAndel>>()
    var overgangsordningAndeler = mutableMapOf<Long, List<OvergangsordningAndel>>()
    var andelerTilkjentYtelse = mutableMapOf<Long, List<AndelTilkjentYtelse>>()
    var overstyrteEndringstidspunkt = mutableMapOf<Long, LocalDate>()
    var uregistrerteBarn = mutableMapOf<Long, List<BarnMedOpplysningerDto>>()
    var målform: Målform = Målform.NB
    var søknadstidspunkt: LocalDate? = null
    var vedtakslister = mutableListOf<Vedtak>()

    var dagensDato: LocalDate = LocalDate.now()

    /**
     * Mulige verdier: | FagsakId |
     */
    @Gitt("følgende fagsaker")
    fun `følgende fagsaker  `(dataTable: DataTable) {
        fagsaker = lagFagsaker(dataTable) + fagsaker
    }

    /**
     * Mulige felter:
     * | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsårsak | Behandlingsstatus |
     */
    @Og("følgende behandlinger")
    fun `følgende behandling`(dataTable: DataTable) {
        val nyeBehandlinger =
            lagbehandlinger(
                dataTable = dataTable,
                fagsaker = fagsaker,
            )

        behandlinger.putAll(nyeBehandlinger.associateBy { it.id }.toMutableMap())
        vedtakslister.addAll(behandlinger.values.map { it.tilVedtak() })

        behandlingTilForrigeBehandling = lagBehandlingTilForrigeBehandlingMap(dataTable)
    }

    private fun Behandling.tilVedtak() = lagVedtak(this)

    /**
     * Mulige verdier: | BehandlingId |  AktørId | Persontype | Fødselsdato |
     */
    @Og("følgende persongrunnlag")
    fun `følgende persongrunnlag`(dataTable: DataTable) {
        val nyePersongrunnlag = lagPersonGrunnlag(dataTable)
        personopplysningGrunnlagMap.putAll(nyePersongrunnlag)
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
        dagensDato = parseValgfriDato(dagensDatoString) ?: LocalDate.now()
    }

    /**
     * Mulige verdier: | AktørId | Vilkår | Utdypende vilkår | Fra dato | Til dato | Resultat | Er eksplisitt avslag | Vurderes etter | Søker har meldt fra om barnehageplass |
     */
    @Og("følgende vilkårresultater for behandling {}")
    fun `legg til nye vilkårresultater for behandling`(
        behandlingId: Long,
        dataTable: DataTable,
    ) {
        vilkårsvurdering[behandlingId] = lagVilkårsvurdering(dataTable, this, behandlingId)
    }

    /**
     * Mulige verdier: | AktørId | Fra dato | Til dato | BehandlingId |  Årsak | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted | Er eksplisitt avslag |
     */
    @Og("følgende endrede utbetalinger")
    fun `følgende endrede utbetalinger`(
        dataTable: DataTable,
    ) {
        endredeUtbetalinger = lagEndredeUtbetalinger(dataTable.asMaps(), personopplysningGrunnlagMap)
    }

    /**
     * Mulige felt:
     * | AktørId | Fra dato | Til dato | Resultat | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
     */
    @Og("følgende kompetanser for behandling {}")
    fun `følgende kompetanser for behandling {}`(
        behandlingId: Long,
        dataTable: DataTable,
    ) {
        kompetanser = lagKompetanser(dataTable.asMaps(), personopplysningGrunnlagMap, behandlingId)
    }

    /**
     * Mulige felt:
     * | AktørId | Fra dato | Til dato | BehandlingId | Valutakursdato | Valuta kode | Kurs |
     */
    @Og("følgende valutakurser for behandling {}")
    fun `følgende valutakurser for behandling`(
        behandlingId: Long,
        dataTable: DataTable,
    ) {
        valutakurs[behandlingId] = lagValutakurs(dataTable.asMaps(), personopplysningGrunnlagMap, behandlingId)
    }

    /**
     * Mulige felt:
     * | AktørId | Fra dato | Til dato | BehandlingId | Beløp | Valuta kode | Intervall | Utbetalingsland |
     */
    @Og("følgende utenlandske periodebeløp for behandling {}")
    fun `med utenlandske periodebeløp`(
        behandlingId: Long,
        dataTable: DataTable,
    ) {
        utenlandskPeriodebeløp[behandlingId] = lagUtenlandskperiodeBeløp(dataTable.asMaps(), personopplysningGrunnlagMap, behandlingId)
    }

    @Og("andeler er beregnet for behandling {}")
    fun `andeler er beregnet`(
        behandlingId: Long,
    ) {
        val andelerFørDifferanseberegning =
            CucumberMock(this)
                .tilkjentYtelseService
                .beregnTilkjentYtelse(
                    vilkårsvurdering = vilkårsvurdering[behandlingId]!!,
                    personopplysningGrunnlag = personopplysningGrunnlagMap[behandlingId]!!,
                    endretUtbetalingAndeler = endredeUtbetalinger[behandlingId]?.map { EndretUtbetalingAndelMedAndelerTilkjentYtelse(it, emptyList()) } ?: emptyList(),
                ).andelerTilkjentYtelse
                .toList()

        val andelerEtterDifferanseberegning =
            beregnDifferanse(
                andelerTilkjentYtelse = andelerFørDifferanseberegning,
                utenlandskePeriodebeløp = utenlandskPeriodebeløp[behandlingId] ?: emptyList(),
                valutakurser = valutakurs[behandlingId] ?: emptyList(),
            )

        andelerTilkjentYtelse[behandlingId] = andelerEtterDifferanseberegning
    }

    /**
     * Mulige verdier: | AktørId | Fra dato | Til dato | Beløp | Ytelse type | Prosent | Sats |
     */
    @Så("med følgende andeler tilkjent ytelse for behandling {}")
    fun `med andeler tilkjent ytelse`(
        behandlingId: Long,
        dataTable: DataTable,
    ) {
        andelerTilkjentYtelse[behandlingId] = lagAndelerTilkjentYtelse(dataTable, behandlingId, behandlinger, personopplysningGrunnlagMap)
    }

    /**
     * Mulige verdier:| AktørId | Fra dato | Til dato | Delt bosted | Antall Timer
     */
    @Så("følgende overgangsordning andeler for behandling {}")
    fun `med overgangsordning andeler`(
        behandlingId: Long,
        dataTable: DataTable,
    ) {
        overgangsordningAndeler[behandlingId] = lagOvergangsordningAndeler(dataTable, behandlingId, behandlinger, personopplysningGrunnlagMap)
    }

    /**
     * Mulige verdier: | AktørId | Fra dato | Til dato | Beløp | Ytelse type | Prosent | Sats |
     */
    @Så("forvent følgende andeler tilkjent ytelse for behandling {}")
    fun `forvent følgende andeler tilkjent ytelse`(
        behandlingId: Long,
        dataTable: DataTable,
    ) {
        val beregnetTilkjentYtelse = andelerTilkjentYtelse[behandlingId]!!
        val forventedeAndeler = lagAndelerTilkjentYtelse(dataTable, behandlingId, behandlinger, personopplysningGrunnlagMap)

        assertThat(beregnetTilkjentYtelse)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*endretTidspunkt", ".*opprettetTidspunkt", ".*kildeBehandlingId", ".*tilkjentYtelse")
            .isEqualTo(forventedeAndeler)
    }

    @Og("når behandlingsresultatet er utledet for behandling {}")
    fun `når behandlingsresultatet er utledet for behehandling`(
        behandlingId: Long,
    ) {
        val behandling = behandlinger[behandlingId]!!

        val behandlingsresultat = mockBehandlingsresultatService().utledBehandlingsresultat(behandlingId)

        behandlinger[behandlingId] = behandling.copy(resultat = behandlingsresultat)
    }

    @Så("forvent at behandlingsresultatet er {} på behandling {}")
    fun `forvent følgende behanlingsresultat på behandling`(
        forventetBehandlingsresultat: Behandlingsresultat,
        behandlingId: Long,
    ) {
        val faktiskResultat = behandlinger[behandlingId]!!.resultat
        assertThat(faktiskResultat).isEqualTo(forventetBehandlingsresultat)
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
        vedtaksperioderMedBegrunnelser[behandlingId] =
            mockVedtaksperiodeService().genererVedtaksperioderMedBegrunnelser(
                vedtak = behandlinger[behandlingId]!!.tilVedtak(),
                manueltOverstyrtEndringstidspunkt = overstyrteEndringstidspunkt[behandlingId],
            )
    }

    /**
     * Mulige verdier: | Fra dato | Til dato | Vedtaksperiodetype |
     */
    @Så("forvent følgende vedtaksperioder på behandling {}")
    fun `forvent følgende vedtaksperioder på behandling`(
        behandlingId: Long,
        dataTable: DataTable,
    ) {
        val forventedeVedtaksperioder =
            mapForventetVedtaksperioderMedBegrunnelser(
                dataTable = dataTable,
                vedtak =
                    behandlinger[behandlingId]?.tilVedtak()
                        ?: throw Feil("Fant ingen vedtak for behandling $behandlingId"),
            )
        val faktiskeVedtaksperioder = vedtaksperioderMedBegrunnelser[behandlingId]!!

        val vedtaksperioderComparator = compareBy<VedtaksperiodeMedBegrunnelser>({ it.type }, { it.fom }, { it.tom })
        assertThat(faktiskeVedtaksperioder.sortedWith(vedtaksperioderComparator))
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(
                ".*endretTidspunkt",
                ".*opprettetTidspunkt",
                ".*begrunnelser",
                ".*id",
                ".*vedtaksdato",
            ).isEqualTo(forventedeVedtaksperioder.sortedWith(vedtaksperioderComparator))
    }

    /**
     * Mulige verdier: | Fra dato | Til dato | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser | Regelverk Ugyldige begrunnelser | Ugyldige begrunnelser |
     */
    @Så("forvent at følgende begrunnelser er gyldige for behandling {}")
    fun `forvent at følgende begrunnelser er gyldige for behandling`(
        behandlingId: Long,
        dataTable: DataTable,
    ) {
        val forventedeStandardBegrunnelser = mapBegrunnelser(dataTable).toSet()

        forventedeStandardBegrunnelser.forEach { forventet ->
            val faktisk =
                hentUtvidedeVedtaksperioderMedBegrunnelser(behandlingId)
                    .mapIndexed { index, utvidetVedtaksperiodeMedBegrunnelser ->
                        utvidetVedtaksperiodeMedBegrunnelser.copy(
                            gyldigeBegrunnelser =
                                BegrunnelserForPeriodeContext(
                                    utvidetVedtaksperiodeMedBegrunnelser = utvidetVedtaksperiodeMedBegrunnelser,
                                    sanityBegrunnelser = sanityBegrunnelserMock,
                                    personopplysningGrunnlag = personopplysningGrunnlagMap[behandlingId]!!,
                                    personResultater = vilkårsvurdering[behandlingId]!!.personResultater.toList(),
                                    endretUtbetalingsandeler = endredeUtbetalinger[behandlingId] ?: emptyList(),
                                    erFørsteVedtaksperiode = index == 0,
                                    kompetanser = hentUtfylteKompetanserPåBehandling(behandlingId),
                                    overgangsordningAndeler = overgangsordningAndeler[behandlingId] ?: emptyList(),
                                    andelerTilkjentYtelse = hentAndelerTilkjentYtelseMedEndreteUtbetalinger(behandlingId),
                                    adopsjonerIBehandling = emptyList(), // TODO: Legg inn støtte for cucumber-tester
                                ).hentGyldigeBegrunnelserForVedtaksperiode(),
                        )
                    }.find { it.fom == forventet.fom && it.tom == forventet.tom }
                    ?: throw Feil(
                        "Forventet å finne en vedtaksperiode med  \n" +
                            "   Fom: ${forventet.fom?.tilddMMyyyy()} og Tom: ${forventet.tom?.tilddMMyyyy()}. \n" +
                            "Faktiske vedtaksperioder var \n${
                                vedtaksperioderMedBegrunnelser[behandlingId]!!.joinToString("\n") {
                                    "   Fom: ${it.fom?.tilddMMyyyy()}, Tom: ${it.tom?.tilddMMyyyy()}"
                                }
                            }",
                    )
            assertThat(faktisk.type)
                .`as`("For periode: ${forventet.fom} til ${forventet.tom}")
                .isEqualTo(forventet.type)
            assertThat(faktisk.gyldigeBegrunnelser)
                .`as`("For periode: ${forventet.fom} til ${forventet.tom}")
                .containsAll(forventet.inkluderteStandardBegrunnelser)

            if (faktisk.gyldigeBegrunnelser.isNotEmpty() && forventet.ekskluderteStandardBegrunnelser.isNotEmpty()) {
                assertThat(faktisk.gyldigeBegrunnelser).doesNotContainAnyElementsOf(forventet.ekskluderteStandardBegrunnelser)
            }
        }
    }

    private fun hentUtfylteKompetanserPåBehandling(behandlingId: Long) =
        (kompetanser[behandlingId] ?: emptyList())
            .map { it.tilIKompetanse() }
            .filterIsInstance<UtfyltKompetanse>()

    fun hentUtvidedeVedtaksperioderMedBegrunnelser(
        behandlingId: Long,
    ): List<UtvidetVedtaksperiodeMedBegrunnelser> {
        val vedtaksperioderMedBegrunnelser = vedtaksperioderMedBegrunnelser[behandlingId]!!
        return vedtaksperioderMedBegrunnelser.map {
            it.tilUtvidetVedtaksperiodeMedBegrunnelser(
                personopplysningGrunnlag = personopplysningGrunnlagMap[behandlingId]!!,
                andelerTilkjentYtelse = hentAndelerTilkjentYtelseMedEndreteUtbetalinger(behandlingId),
                dagensDato = dagensDato,
                sanityBegrunnelser = emptyList(),
            )
        }
    }

    /**
     * Mulige verdier: | Fra dato | Til dato | Standardbegrunnelser | Eøsbegrunnelser | Fritekster |
     */
    @Og("når disse begrunnelsene er valgt for behandling {}")
    fun `når disse begrunnelsene er valgt for behandling`(
        behandlingId: Long,
        dataTable: DataTable,
    ) {
        val vedtaksperioder = vedtaksperioderMedBegrunnelser[behandlingId]!!

        vedtaksperioderMedBegrunnelser[behandlingId] = leggBegrunnelserIVedtaksperiodene(dataTable, vedtaksperioder)
    }

    /**
     * Mulige verdier: | Brevperiodetype | Fra dato | Til dato | Beløp | Antall barn med utbetaling | Barnas fødselsdager | Du eller institusjonen |
     */
    @Så("forvent følgende brevperioder for behandling {}")
    fun `forvent følgende brevperioder for behandling i periode`(
        behandlingId: Long,
        dataTable: DataTable,
    ) {
        val faktiskeBrevperioder =
            hentBrevperioder(behandlingId).map { brevPeriode -> brevPeriode?.copy(tom = brevPeriode.tom?.map { it.trim() }) }

        val forvendtedeBrevperioder = parseBrevPerioder(dataTable)

        assertThat(faktiskeBrevperioder)
            .usingRecursiveComparison()
            .ignoringFields("begrunnelser")
            .ignoringFields("antallBarnMedNullutbetaling")
            .ignoringFields("antallBarnMedUtbetaling")
            .ignoringFields("fodselsdagerBarnMedUtbetaling")
            .isEqualTo(forvendtedeBrevperioder)
    }

    private fun hentBrevperioder(behandlingId: Long): List<BrevPeriodeDto?> =
        hentUtvidedeVedtaksperioderMedBegrunnelser(behandlingId).sortedBy { it.fom ?: TIDENES_MORGEN }.mapIndexedNotNull { index, it ->
            it.hentBrevPeriode(behandlingId, index == 0)
        }

    private fun UtvidetVedtaksperiodeMedBegrunnelser.hentBrevPeriode(
        behandlingId: Long,
        erFørsteVedtaksperiode: Boolean,
    ) = BrevPeriodeContext(
        utvidetVedtaksperiodeMedBegrunnelser = this,
        sanityBegrunnelser = sanityBegrunnelserMock,
        personopplysningGrunnlag = personopplysningGrunnlagMap[behandlingId]!!,
        personResultater = vilkårsvurdering[behandlingId]!!.personResultater.toList(),
        andelTilkjentYtelserMedEndreteUtbetalinger = hentAndelerTilkjentYtelseMedEndreteUtbetalinger(behandlingId),
        overgangsordningAndeler = overgangsordningAndeler[behandlingId] ?: emptyList(),
        uregistrerteBarn = uregistrerteBarn[behandlingId] ?: emptyList(),
        // TODO
        erFørsteVedtaksperiode = erFørsteVedtaksperiode,
        kompetanser = hentUtfylteKompetanserPåBehandling(behandlingId),
        landkoder = LANDKODER,
        adopsjonerIBehandling = emptyList(), // TODO: Fiks før merge
    ).genererBrevPeriodeDto()

    /**
     * Mulige verdier Nasjonal: | Begrunnelse | Type | Barnas fødselsdatoer | Antall barn | Gjelder søker  | Målform | Beløp | Søknadstidspunkt | Måned og år begrunnelsen gjelder for| Avtale tidspunkt delt bosted | Søkers rett til utvidet |
     * Mulige verdier EØS: | Begrunnelse | Type | Barnas fødselsdatoer | Antall barn | Gjelder søker | Målform | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
     */
    @Så("forvent følgende brevbegrunnelser for behandling {} i periode {} til {}")
    fun `forvent følgende brevbegrunnelser for behandling i periode`(
        behandlingId: Long,
        periodeFom: String,
        periodeTom: String,
        dataTable: DataTable,
    ) {
        val utvidedeVedtaksperioderMedBegrunnelser = hentUtvidedeVedtaksperioderMedBegrunnelser(behandlingId).sortedBy { it.fom ?: TIDENES_MORGEN }
        val relevantUtvidetVedtaksperiode =
            utvidedeVedtaksperioderMedBegrunnelser.find {
                it.fom == parseNullableDato(periodeFom) && it.tom == parseNullableDato(periodeTom)
            }!!

        val faktiskeBegrunnelser =
            relevantUtvidetVedtaksperiode
                .hentBrevPeriode(
                    behandlingId = behandlingId,
                    erFørsteVedtaksperiode = relevantUtvidetVedtaksperiode == utvidedeVedtaksperioderMedBegrunnelser.firstOrNull(),
                )!!
                .begrunnelser
                .filterIsInstance<BegrunnelseDtoMedData>()

        val forvendtedeBegrunnelser = parseBegrunnelser(dataTable)

        assertThat(faktiskeBegrunnelser.sortedBy { it.apiNavn })
            .usingRecursiveComparison()
            .ignoringFields("vedtakBegrunnelseType")
            .isEqualTo(forvendtedeBegrunnelser.sortedBy { it.apiNavn })
    }

    @Når("vi oppretter vilkårresultater for behandling {}")
    fun `vi oppretter vilkårresultater for behandling`(behandlingId: Long) {
        val vilkårsvurderingService = CucumberMock(this).vilkårsvurderingService
        val behandling = behandlinger[behandlingId]!!
        vilkårsvurdering[behandlingId] =
            vilkårsvurderingService.opprettVilkårsvurdering(
                behandling = behandling,
                forrigeBehandlingSomErVedtatt = behandlingTilForrigeBehandling[behandlingId]?.let { behandlinger[it] },
            )
    }

    // Mulige verdier: | AktørId | Vilkår | Utdypende vilkår | Fra dato | Til dato | Resultat | Er eksplisitt avslag | Vurderes etter | Søker har meldt fra om barnehageplass |
    @Så("forvent følgende vilkårresultater for behandling {}")
    fun `forvent følgende vilkårresultater for behandling`(
        behandlingId: Long,
        dataTable: DataTable,
    ) {
        val forventetVilkårsvurdering = lagVilkårsvurdering(dataTable, this, behandlingId)

        val faktiskeVilkårsvurderinger = vilkårsvurdering[behandlingId]!!

        val faktiskeVilkårResultaterGruppertPåAktør =
            faktiskeVilkårsvurderinger.personResultater.map { Pair(it.aktør, it.vilkårResultater) }.toMap()
        val forventetVilkårResultaterGruppertPåAktør =
            forventetVilkårsvurdering.personResultater.map { Pair(it.aktør, it.vilkårResultater) }.toMap()

        forventetVilkårResultaterGruppertPåAktør.forEach { (aktør, forventetVilkårResultat) ->
            val faktiskVilkårResultat = faktiskeVilkårResultaterGruppertPåAktør[aktør] ?: emptyList()

            val comparator = compareBy<VilkårResultat>({ it.vilkårType }, { it.periodeFom })
            assertThat(faktiskVilkårResultat.sortedWith(comparator))
                .`as`("Valider vilkår for aktør $aktør")
                .usingRecursiveComparison()
                .ignoringFieldsMatchingRegexes(
                    ".*endretTidspunkt",
                    ".*id",
                    ".*opprettetTidspunkt",
                    ".*begrunnelse",
                    ".*personResultat",
                    ".*behandlingId",
                ).isEqualTo(forventetVilkårResultat.sortedWith(comparator))
        }
    }

    fun mockBehandlingsresultatService(): BehandlingsresultatService {
        val behandlingService = mockk<BehandlingService>()

        every { behandlingService.hentBehandling(any()) } answers {
            val behandlingId = firstArg<Long>()
            behandlinger[behandlingId] ?: throw Feil("Ingen behandling med id: $behandlingId")
        }

        every { behandlingService.hentSisteBehandlingSomErVedtatt(any()) } answers {
            val fagsakId = firstArg<Long>()
            behandlinger.values
                .filter { behandling -> behandling.fagsak.id == fagsakId && behandling.status == BehandlingStatus.AVSLUTTET }
                .maxByOrNull { it.id }
        }

        val søknadGrunnlagService = mockk<SøknadGrunnlagService>()

        every { søknadGrunnlagService.hentAktiv(any()) } answers {
            val behandlingId = firstArg<Long>()
            lagSøknadGrunnlag(behandlingId) ?: throw Feil("Kunne ikke lage søknadGrunnlag")
        }

        every { søknadGrunnlagService.finnAktiv(any()) } answers {
            val behandlingId = firstArg<Long>()
            lagSøknadGrunnlag(behandlingId)
        }

        val personidentService = mockk<PersonidentService>()
        every { personidentService.hentAktør(any()) } answers {
            val personId = firstArg<String>()
            personopplysningGrunnlagMap.flatMap { it.value.personer }.first { it.id.toString() == personId }.aktør
        }

        val andelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()
        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(any()) } answers {
            val behandlingId = firstArg<Long>()
            andelerTilkjentYtelse[behandlingId] ?: emptyList()
        }

        val endretUtbetalingAndelService = mockk<EndretUtbetalingAndelService>()
        every { endretUtbetalingAndelService.hentEndredeUtbetalingAndeler(any()) } answers {
            val behandlingId = firstArg<Long>()
            endredeUtbetalinger[behandlingId] ?: emptyList()
        }

        val kompetanseService = mockk<KompetanseService>()
        every { kompetanseService.hentKompetanser(any()) } answers {
            val behandlingId = firstArg<BehandlingId>()
            kompetanser[behandlingId.id] ?: emptyList()
        }

        val localDateProvider = mockk<LocalDateProvider>()
        every { localDateProvider.now() } returns dagensDato

        return BehandlingsresultatService(
            behandlingService = behandlingService,
            vilkårsvurderingService = mockVilkårsvurderingService(),
            søknadGrunnlagService = søknadGrunnlagService,
            personidentService = personidentService,
            personopplysningGrunnlagService = mockPersonopplysningGrunnlagService(),
            andelerTilkjentYtelseRepository = andelTilkjentYtelseRepository,
            endretUtbetalingAndelService = endretUtbetalingAndelService,
            kompetanseService = kompetanseService,
            localDateProvider = localDateProvider,
        )
    }

    private fun lagSøknadGrunnlag(behandlingId: Long): SøknadGrunnlag? {
        if (!behandlinger[behandlingId]!!.erSøknad()) {
            return null
        }

        val søknadDtoString =
            objectMapper.writeValueAsString(
                SøknadDto(
                    barnaMedOpplysninger = lagRegistrertebarn(behandlingId) + (uregistrerteBarn[behandlingId] ?: emptyList()),
                    endringAvOpplysningerBegrunnelse = "",
                    søkerMedOpplysninger = SøkerMedOpplysningerDto(ident = "", målform = målform),
                ),
            )
        return SøknadGrunnlag(behandlingId = behandlingId, søknad = søknadDtoString)
    }

    fun mockVedtaksperiodeService(): VedtaksperiodeService {
        val behandlingRepository = mockk<BehandlingRepository>()
        every { behandlingRepository.finnIverksatteBehandlinger(any<Long>()) } answers {
            behandlinger.values.filter { behandling -> behandling.fagsak.id == firstArg<Long>() && behandling.status == BehandlingStatus.AVSLUTTET }
        }
        every { behandlingRepository.finnBehandlinger(any<Long>()) } answers {
            behandlinger.values.filter { behandling -> behandling.fagsak.id == firstArg<Long>() }
        }

        val vilkårsvurderingRepository = mockk<VilkårsvurderingRepository>()
        every { vilkårsvurderingRepository.finnAktivForBehandling(any<Long>()) } answers {
            val behandlingId = firstArg<Long>()
            vilkårsvurdering[behandlingId]
        }
        val søknadGrunnlagService = mockk<SøknadGrunnlagService>()
        every { søknadGrunnlagService.finnAktiv(any<Long>()) } answers {
            val behandlingId = firstArg<Long>()
            val søknadDtoString =
                objectMapper.writeValueAsString(
                    SøknadDto(
                        barnaMedOpplysninger =
                            lagRegistrertebarn(behandlingId) + (
                                uregistrerteBarn[behandlingId]
                                    ?: emptyList()
                            ),
                        endringAvOpplysningerBegrunnelse = "",
                        søkerMedOpplysninger = SøkerMedOpplysningerDto(ident = "", målform = målform),
                    ),
                )
            SøknadGrunnlag(behandlingId = behandlingId, søknad = søknadDtoString)
        }

        every { søknadGrunnlagService.hentAktiv(any<Long>()) } answers {
            val behandlingId = firstArg<Long>()
            val søknadDtoString =
                objectMapper.writeValueAsString(
                    SøknadDto(
                        barnaMedOpplysninger = lagRegistrertebarn(behandlingId) + (uregistrerteBarn[behandlingId] ?: emptyList()),
                        endringAvOpplysningerBegrunnelse = "",
                        søkerMedOpplysninger = SøkerMedOpplysningerDto(ident = "", målform = målform),
                    ),
                )
            SøknadGrunnlag(behandlingId = behandlingId, søknad = søknadDtoString)
        }

        val kompetanseService = mockk<KompetanseService>()
        every { kompetanseService.hentKompetanser(any<BehandlingId>()) } answers {
            val behandlingId = firstArg<BehandlingId>()
            kompetanser[behandlingId.id] ?: emptyList()
        }

        val utbetalingsperiodeMedBegrunnelserService =
            UtbetalingsperiodeMedBegrunnelserService(
                vilkårsvurderingService = mockVilkårsvurderingService(),
                andelerTilkjentYtelseOgEndreteUtbetalingerService = mockAndelerTilkjentYtelseOgEndreteUtbetalingerService(),
                personopplysningGrunnlagService = mockPersonopplysningGrunnlagService(),
                kompetanseService = kompetanseService,
                unleashNextMedContextService = mockUnleashNextMedContextService(),
                adopsjonService = mockAdopsjonService(),
            )

        return VedtaksperiodeService(
            behandlingRepository = behandlingRepository,
            personopplysningGrunnlagService = mockPersonopplysningGrunnlagService(),
            vedtaksperiodeHentOgPersisterService = mockk(),
            vedtakRepository = mockk(),
            vilkårsvurderingRepository = vilkårsvurderingRepository,
            sanityService = mockk(),
            søknadGrunnlagService = søknadGrunnlagService,
            utbetalingsperiodeMedBegrunnelserService = utbetalingsperiodeMedBegrunnelserService,
            overgangsordningAndelService = mockk(),
            andelerTilkjentYtelseOgEndreteUtbetalingerService = mockAndelerTilkjentYtelseOgEndreteUtbetalingerService(),
            integrasjonClient = mockk(),
            refusjonEøsRepository = mockk(),
            kompetanseService = kompetanseService,
            adopsjonService = mockAdopsjonService(),
        )
    }

    private fun lagRegistrertebarn(behandlingId: Long): List<BarnMedOpplysningerDto> =
        personopplysningGrunnlagMap[behandlingId]
            ?.personer
            ?.filter { it.type == PersonType.BARN }
            ?.map { person ->
                BarnMedOpplysningerDto(
                    ident = person.id.toString(),
                    fødselsdato = person.fødselsdato,
                )
            } ?: emptyList()

    private fun mockPersonopplysningGrunnlagService(): PersonopplysningGrunnlagService {
        val personopplysningGrunnlagService = mockk<PersonopplysningGrunnlagService>()
        every { personopplysningGrunnlagService.finnAktivPersonopplysningGrunnlag(any<Long>()) } answers {
            personopplysningGrunnlagMap[firstArg()]
        }
        every { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(any<Long>()) } answers {
            personopplysningGrunnlagMap[firstArg()]!!
        }
        every { personopplysningGrunnlagService.hentBarna(any<Long>()) } answers {
            val behandlingId = firstArg<Long>()
            personopplysningGrunnlagMap[behandlingId]!!.barna
        }
        return personopplysningGrunnlagService
    }

    private fun mockVilkårsvurderingService(): VilkårsvurderingService {
        val vilkårsvurderingService = mockk<VilkårsvurderingService>()
        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(any<Long>()) } answers {
            val behandlingId = firstArg<Long>()
            vilkårsvurdering[behandlingId]!!
        }
        every { vilkårsvurderingService.oppdater(any()) } answers { firstArg<Vilkårsvurdering>() }

        return vilkårsvurderingService
    }

    private fun mockAndelerTilkjentYtelseOgEndreteUtbetalingerService(): AndelerTilkjentYtelseOgEndreteUtbetalingerService {
        val andelerTilkjentYtelseOgEndreteUtbetalingerService =
            mockk<AndelerTilkjentYtelseOgEndreteUtbetalingerService>()
        every { andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(any<Long>()) } answers {
            val behandlingId = firstArg<Long>()
            hentAndelerTilkjentYtelseMedEndreteUtbetalinger(behandlingId)
        }
        every { andelerTilkjentYtelseOgEndreteUtbetalingerService.finnEndreteUtbetalingerMedAndelerTilkjentYtelse(any<Long>()) } answers {
            val behandlingId = firstArg<Long>()
            endredeUtbetalinger[behandlingId]?.tilEndretUtbetalingAndelMedAndelerTilkjentYtelse(
                andelerTilkjentYtelse[behandlingId] ?: emptyList(),
            ) ?: emptyList()
        }
        return andelerTilkjentYtelseOgEndreteUtbetalingerService
    }

    private fun hentAndelerTilkjentYtelseMedEndreteUtbetalinger(behandlingId: Long) =
        andelerTilkjentYtelse[behandlingId]?.tilAndelerTilkjentYtelseMedEndreteUtbetalinger(
            endredeUtbetalinger[behandlingId] ?: emptyList(),
        ) ?: emptyList()
}

private object SanityBegrunnelseMock {
    // For å laste ned begrunnelsene kjør scriptet "src/test/resources/oppdater-sanity-mock.sh" eller
    // se https://familie-brev.sanity.studio/ks-brev/vision med query fra SanityQueries.kt.
    fun hentSanityBegrunnelserMock(): List<SanityBegrunnelse> {
        val restSanityBegrunnelserJson =
            this::class.java.getResource("/cucumber/restSanityBegrunnelser")!!

        val restSanityBegrunnelser =
            objectMapper
                .readValue(restSanityBegrunnelserJson, Array<SanityBegrunnelseDto>::class.java)
                .toList()

        return restSanityBegrunnelser.map { it.tilSanityBegrunnelse() }
    }
}
