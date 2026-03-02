package no.nav.familie.ks.sak.cucumber

import io.cucumber.datatable.DataTable
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.ks.sak.api.dto.BarnMedOpplysningerDto
import no.nav.familie.ks.sak.api.dto.SøkerMedOpplysningerDto
import no.nav.familie.ks.sak.api.dto.SøknadDto
import no.nav.familie.ks.sak.api.dto.tilKalkulertMånedligBeløp
import no.nav.familie.ks.sak.common.domeneparser.BrevPeriodeParser.DomenebegrepBrevBegrunnelse.MÅLFORM
import no.nav.familie.ks.sak.common.domeneparser.Domenebegrep
import no.nav.familie.ks.sak.common.domeneparser.DomenebegrepAndelTilkjentYtelse
import no.nav.familie.ks.sak.common.domeneparser.VedtaksperiodeMedBegrunnelserParser
import no.nav.familie.ks.sak.common.domeneparser.VedtaksperiodeMedBegrunnelserParser.DomenebegrepPersongrunnlag.ER_FOLKEREGISTRERT
import no.nav.familie.ks.sak.common.domeneparser.VedtaksperiodeMedBegrunnelserParser.DomenebegrepPersongrunnlag.ER_INKLUDERT_I_SØKNADEN
import no.nav.familie.ks.sak.common.domeneparser.VedtaksperiodeMedBegrunnelserParser.parseAktørId
import no.nav.familie.ks.sak.common.domeneparser.parseBigDecimal
import no.nav.familie.ks.sak.common.domeneparser.parseBoolean
import no.nav.familie.ks.sak.common.domeneparser.parseDato
import no.nav.familie.ks.sak.common.domeneparser.parseEnum
import no.nav.familie.ks.sak.common.domeneparser.parseEnumListe
import no.nav.familie.ks.sak.common.domeneparser.parseInt
import no.nav.familie.ks.sak.common.domeneparser.parseList
import no.nav.familie.ks.sak.common.domeneparser.parseLong
import no.nav.familie.ks.sak.common.domeneparser.parseValgfriBigDecimal
import no.nav.familie.ks.sak.common.domeneparser.parseValgfriBoolean
import no.nav.familie.ks.sak.common.domeneparser.parseValgfriDato
import no.nav.familie.ks.sak.common.domeneparser.parseValgfriEnum
import no.nav.familie.ks.sak.common.domeneparser.parseValgfriInt
import no.nav.familie.ks.sak.common.domeneparser.parseValgfriLong
import no.nav.familie.ks.sak.common.domeneparser.parseValgfriString
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.util.tilddMMyyyy
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagDødsfall
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.data.tilfeldigPerson
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStegTilstand
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.registrersøknad.domene.SøknadGrunnlag
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.EØSBegrunnelseDB
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.NasjonalEllerFellesBegrunnelseDB
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.EØSBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.IBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.NasjonalEllerFellesBegrunnelse
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ks.sak.kjerne.eøs.differanseberegning.domene.Intervall
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.KompetanseAktivitet
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ks.sak.kjerne.eøs.utenlandskperiodebeløp.domene.UtenlandskPeriodebeløp
import no.nav.familie.ks.sak.kjerne.eøs.valutakurs.domene.Valutakurs
import no.nav.familie.ks.sak.kjerne.fagsak.domene.Fagsak
import no.nav.familie.ks.sak.kjerne.overgangsordning.domene.OvergangsordningAndel
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Målform
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import org.apache.commons.lang3.RandomStringUtils
import java.math.BigDecimal
import java.time.LocalDate

fun Map<Long, Behandling>.finnBehandling(behandlingId: Long) = this[behandlingId] ?: throw Feil("Finner ikke behandling med id $behandlingId")

fun Map<Long, PersonopplysningGrunnlag>.finnPersonGrunnlagForBehandling(behandlingId: Long): PersonopplysningGrunnlag = this[behandlingId] ?: throw Feil("Finner ikke persongrunnlag for behandling med id $behandlingId")

fun lagFagsaker(dataTable: DataTable) =
    dataTable
        .asMaps()
        .map { rad ->
            Fagsak(
                id = parseLong(Domenebegrep.FAGSAK_ID, rad),
                aktør = randomAktør(),
            )
        }.associateBy { it.id }

fun lagbehandlinger(
    dataTable: DataTable,
    fagsaker: Map<Long, Fagsak>,
): List<Behandling> =
    dataTable.asMaps().map { rad ->
        val behandlingId = parseLong(Domenebegrep.BEHANDLING_ID, rad)
        val fagsakId = parseValgfriLong(Domenebegrep.FAGSAK_ID, rad)
        val fagsak = fagsaker[fagsakId] ?: lagFagsak()
        val behandlingÅrsak = parseValgfriEnum<BehandlingÅrsak>(Domenebegrep.BEHANDLINGSÅRSAK, rad)
        val behandlingKategori =
            parseValgfriEnum<BehandlingKategori>(Domenebegrep.BEHANDLINGSKATEGORI, rad)
                ?: BehandlingKategori.NASJONAL
        val behandlingsresultat =
            parseValgfriEnum<Behandlingsresultat>(Domenebegrep.BEHANDLINGSRESULTAT, rad)
                ?: Behandlingsresultat.IKKE_VURDERT
        val status = parseValgfriEnum<BehandlingStatus>(Domenebegrep.BEHANDLINGSSTATUS, rad)

        val behandling =
            lagBehandling(
                fagsak = fagsak,
                opprettetÅrsak = behandlingÅrsak ?: BehandlingÅrsak.SØKNAD,
                resultat = behandlingsresultat,
                kategori = behandlingKategori,
            ).copy(id = behandlingId)
        behandling.apply {
            this.status = status ?: BehandlingStatus.UTREDES

            if (status == BehandlingStatus.AVSLUTTET) {
                this.behandlingStegTilstand.add(
                    BehandlingStegTilstand(
                        behandling = this,
                        behandlingSteg = BehandlingSteg.AVSLUTT_BEHANDLING,
                    ),
                )
            }
        }
    }

fun lagBehandlingTilForrigeBehandlingMap(
    dataTable: DataTable,
) = dataTable.asMaps().associate { rad ->
    parseLong(Domenebegrep.BEHANDLING_ID, rad) to parseValgfriLong(Domenebegrep.FORRIGE_BEHANDLING_ID, rad)
}

fun lagVilkårsvurdering(
    dataTable: DataTable,
    stepDefinition: StepDefinition,
    behandlingId: Long,
): Vilkårsvurdering {
    val vilkårvurdering =
        Vilkårsvurdering(
            behandling = stepDefinition.behandlinger[behandlingId] ?: throw Feil("Fant ikke behandling med id $behandlingId"),
        )

    val vilkårResultaterPerPerson =
        dataTable.asMaps().groupBy { parseAktørId(it) }

    val personresultater =
        vilkårResultaterPerPerson
            .map { (aktørId, personResultatDataRader) ->
                val personResultat =
                    PersonResultat(
                        vilkårsvurdering = vilkårvurdering,
                        aktør =
                            stepDefinition.personopplysningGrunnlagMap[behandlingId]!!
                                .personer
                                .single { it.aktør.aktørId == aktørId }
                                .aktør,
                    )

                val vilkårResultater =
                    personResultatDataRader.flatMap { rad ->
                        lagVilkårResultater(rad, behandlingId, personResultat)
                    }
                personResultat.setSortedVilkårResultater(vilkårResultater.toSet())
                personResultat
            }.toSet()

    vilkårvurdering.personResultater = personresultater

    return vilkårvurdering
}

private fun lagVilkårResultater(
    rad: MutableMap<String, String>,
    behandlingId: Long,
    personResultat: PersonResultat,
): List<VilkårResultat> {
    val vilkårFor =
        parseEnumListe<Vilkår>(
            VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser.VILKÅR,
            rad,
        )

    val utdypendeVilkårsvurderingFor =
        parseEnumListe<UtdypendeVilkårsvurdering>(
            VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser.UTDYPENDE_VILKÅR,
            rad,
        )

    val vurderesEtter =
        parseValgfriEnum<Regelverk>(
            VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser.VURDERES_ETTER,
            rad,
        )

    val søkerHarMeldtFraOmBarnehageplass =
        parseValgfriBoolean(
            VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser.FREMTIDIG_BARNEHAGEPLASS,
            rad,
        )

    val antallTimer =
        parseValgfriBigDecimal(
            VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser.ANTALL_TIMER,
            rad,
        )

    return vilkårFor.map { vilkår ->
        VilkårResultat(
            behandlingId = behandlingId,
            personResultat = personResultat,
            vilkårType = vilkår,
            resultat =
                parseEnum<Resultat>(
                    VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser.RESULTAT,
                    rad,
                ),
            periodeFom = parseValgfriDato(Domenebegrep.FRA_DATO, rad),
            periodeTom = parseValgfriDato(Domenebegrep.TIL_DATO, rad),
            erEksplisittAvslagPåSøknad =
                parseValgfriBoolean(
                    VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser.ER_EKSPLISITT_AVSLAG,
                    rad,
                ),
            begrunnelse = "",
            utdypendeVilkårsvurderinger = utdypendeVilkårsvurderingFor,
            vurderesEtter = vurderesEtter,
            // TODO må fjerne filterIsInstance når vi får inn eøsbegrunnelser her også
            begrunnelser = hentStandardBegrunnelser(rad).filterIsInstance<NasjonalEllerFellesBegrunnelse>(),
            søkerHarMeldtFraOmBarnehageplass = søkerHarMeldtFraOmBarnehageplass,
            antallTimer = antallTimer,
            erAutomatiskVurdert =
                parseValgfriBoolean(DomenebegrepAndelTilkjentYtelse.ER_AUTOMATISK_VURDERT, rad) ?: false,
        )
    }
}

private fun hentStandardBegrunnelser(rad: MutableMap<String, String>): List<IBegrunnelse> {
    val standardbegrunnelser: List<IBegrunnelse> =
        try {
            parseEnumListe<NasjonalEllerFellesBegrunnelse>(
                VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser.STANDARDBEGRUNNELSER,
                rad,
            )
        } catch (_: Exception) {
            parseEnumListe<EØSBegrunnelse>(
                VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser.STANDARDBEGRUNNELSER,
                rad,
            )
        }

    return standardbegrunnelser
}

fun lagKompetanser(
    nyeKompetanserPerBarn: MutableList<MutableMap<String, String>>,
    personopplysningGrunnlag: Map<Long, PersonopplysningGrunnlag>,
    behandlingId: Long,
) = nyeKompetanserPerBarn
    .map { rad ->
        val aktørerForKompetanse = VedtaksperiodeMedBegrunnelserParser.parseAktørIdListe(rad)
        Kompetanse(
            fom = parseValgfriDato(Domenebegrep.FRA_DATO, rad)?.toYearMonth(),
            tom = parseValgfriDato(Domenebegrep.TIL_DATO, rad)?.toYearMonth(),
            barnAktører =
                personopplysningGrunnlag
                    .finnPersonGrunnlagForBehandling(behandlingId)
                    .personer
                    .filter { aktørerForKompetanse.contains(it.aktør.aktørId) }
                    .map { it.aktør }
                    .toSet(),
            søkersAktivitet =
                parseValgfriEnum<KompetanseAktivitet>(
                    VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.SØKERS_AKTIVITET,
                    rad,
                )
                    ?: KompetanseAktivitet.ARBEIDER,
            annenForeldersAktivitet =
                parseValgfriEnum<KompetanseAktivitet>(
                    VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.ANNEN_FORELDERS_AKTIVITET,
                    rad,
                )
                    ?: KompetanseAktivitet.I_ARBEID,
            søkersAktivitetsland =
                parseValgfriString(
                    VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.SØKERS_AKTIVITETSLAND,
                    rad,
                )?.also { validerErLandkode(it) } ?: "PL",
            annenForeldersAktivitetsland =
                parseValgfriString(
                    VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.ANNEN_FORELDERS_AKTIVITETSLAND,
                    rad,
                )?.also { validerErLandkode(it) } ?: "NO",
            barnetsBostedsland =
                parseValgfriString(
                    VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.BARNETS_BOSTEDSLAND,
                    rad,
                )?.also { validerErLandkode(it) } ?: "NO",
            resultat =
                parseEnum<KompetanseResultat>(
                    VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.RESULTAT,
                    rad,
                ),
        ).also { it.behandlingId = behandlingId }
    }.groupBy { it.behandlingId }
    .toMutableMap()

fun lagValutakurs(
    nyeValutakursPerBarn: MutableList<MutableMap<String, String>>,
    personopplysningGrunnlag: Map<Long, PersonopplysningGrunnlag>,
    behandlingId: Long,
): List<Valutakurs> =
    nyeValutakursPerBarn.map { rad ->
        val aktørerForValutakurs = VedtaksperiodeMedBegrunnelserParser.parseAktørIdListe(rad)

        Valutakurs(
            fom = parseValgfriDato(Domenebegrep.FRA_DATO, rad)?.toYearMonth(),
            tom = parseValgfriDato(Domenebegrep.TIL_DATO, rad)?.toYearMonth(),
            barnAktører =
                personopplysningGrunnlag
                    .finnPersonGrunnlagForBehandling(behandlingId)
                    .personer
                    .filter { aktørerForValutakurs.contains(it.aktør.aktørId) }
                    .map { it.aktør }
                    .toSet(),
            valutakursdato = parseValgfriDato(VedtaksperiodeMedBegrunnelserParser.DomenebegrepValutakurs.VALUTAKURSDATO, rad),
            valutakode =
                parseValgfriString(
                    VedtaksperiodeMedBegrunnelserParser.DomenebegrepValutakurs.VALUTA_KODE,
                    rad,
                ),
            kurs = parseBigDecimal(VedtaksperiodeMedBegrunnelserParser.DomenebegrepValutakurs.KURS, rad),
        ).also { it.behandlingId = behandlingId }
    }

fun lagUtenlandskperiodeBeløp(
    nyeUtenlandskPeriodebeløpPerBarn: MutableList<MutableMap<String, String>>,
    personopplysningGrunnlag: Map<Long, PersonopplysningGrunnlag>,
    behandlingId: Long,
): List<UtenlandskPeriodebeløp> =
    nyeUtenlandskPeriodebeløpPerBarn.map { rad ->
        val aktørerForValutakurs = VedtaksperiodeMedBegrunnelserParser.parseAktørIdListe(rad)

        UtenlandskPeriodebeløp(
            fom = parseValgfriDato(Domenebegrep.FRA_DATO, rad)?.toYearMonth(),
            tom = parseValgfriDato(Domenebegrep.TIL_DATO, rad)?.toYearMonth(),
            barnAktører =
                personopplysningGrunnlag
                    .finnPersonGrunnlagForBehandling(behandlingId)
                    .personer
                    .filter { aktørerForValutakurs.contains(it.aktør.aktørId) }
                    .map { it.aktør }
                    .toSet(),
            beløp = parseBigDecimal(VedtaksperiodeMedBegrunnelserParser.DomenebegrepUtenlandskPeriodebeløp.BELØP, rad),
            valutakode =
                parseValgfriString(
                    VedtaksperiodeMedBegrunnelserParser.DomenebegrepUtenlandskPeriodebeløp.VALUTA_KODE,
                    rad,
                ),
            intervall = parseValgfriEnum<Intervall>(VedtaksperiodeMedBegrunnelserParser.DomenebegrepUtenlandskPeriodebeløp.INTERVALL, rad),
            utbetalingsland = parseValgfriString(VedtaksperiodeMedBegrunnelserParser.DomenebegrepUtenlandskPeriodebeløp.UTBETALINGSLAND, rad),
        ).let {
            it.behandlingId = behandlingId
            it.copy(kalkulertMånedligBeløp = it.tilKalkulertMånedligBeløp())
        }
    }

private fun validerErLandkode(it: String) {
    if (it.length != 2) {
        throw Feil("$it er ikke en landkode")
    }
}

fun lagEndredeUtbetalinger(
    nyeEndredeUtbetalingAndeler: MutableList<MutableMap<String, String>>,
    persongrunnlag: Map<Long, PersonopplysningGrunnlag>,
) = nyeEndredeUtbetalingAndeler
    .map { rad ->
        val aktørIder = VedtaksperiodeMedBegrunnelserParser.parseAktørIdListe(rad)
        val behandlingId = parseLong(Domenebegrep.BEHANDLING_ID, rad)
        EndretUtbetalingAndel(
            behandlingId = behandlingId,
            fom = parseValgfriDato(Domenebegrep.FRA_DATO, rad)?.toYearMonth(),
            tom = parseValgfriDato(Domenebegrep.TIL_DATO, rad)?.toYearMonth(),
            personer =
                persongrunnlag
                    .finnPersonGrunnlagForBehandling(behandlingId)
                    .personer
                    .filter { it.aktør.aktørId in aktørIder }
                    .toMutableSet(),
            prosent =
                parseValgfriLong(
                    VedtaksperiodeMedBegrunnelserParser.DomenebegrepEndretUtbetaling.PROSENT,
                    rad,
                )?.toBigDecimal() ?: BigDecimal.valueOf(100),
            årsak =
                parseValgfriEnum<Årsak>(VedtaksperiodeMedBegrunnelserParser.DomenebegrepEndretUtbetaling.ÅRSAK, rad)
                    ?: Årsak.ALLEREDE_UTBETALT,
            søknadstidspunkt = parseValgfriDato(Domenebegrep.SØKNADSTIDSPUNKT, rad) ?: LocalDate.now(),
            begrunnelse = "Fordi at...",
            erEksplisittAvslagPåSøknad =
                parseValgfriBoolean(
                    VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser.ER_EKSPLISITT_AVSLAG,
                    rad,
                ),
        )
    }.groupBy { it.behandlingId }
    .toMutableMap()

fun lagPersonGrunnlag(dataTable: DataTable): Map<Long, PersonopplysningGrunnlag> =
    dataTable
        .asMaps()
        .map { rad ->
            val behandlingsIder = parseList(Domenebegrep.BEHANDLING_ID, rad)
            behandlingsIder.map { id ->
                id to
                    tilfeldigPerson(
                        personType =
                            parseEnum(
                                VedtaksperiodeMedBegrunnelserParser.DomenebegrepPersongrunnlag.PERSON_TYPE,
                                rad,
                            ),
                        fødselsdato =
                            parseDato(
                                VedtaksperiodeMedBegrunnelserParser.DomenebegrepPersongrunnlag.FØDSELSDATO,
                                rad,
                            ),
                        aktør = randomAktør().copy(aktørId = parseAktørId(rad)),
                    ).also { person ->
                        parseValgfriDato(
                            VedtaksperiodeMedBegrunnelserParser.DomenebegrepPersongrunnlag.DØDSFALLDATO,
                            rad,
                        )?.let { person.dødsfall = lagDødsfall(person = person, dødsfallDato = it) }
                    }
            }
        }.flatten()
        .groupBy({ it.first }, { it.second })
        .map { (behandlingId, personer) ->
            PersonopplysningGrunnlag(
                behandlingId = behandlingId,
                personer = personer.toMutableSet(),
            )
        }.associateBy { it.behandlingId }

fun lagSøknadGrunnlag(
    dataTable: DataTable,
    stepDefinition: StepDefinition,
): Map<Long, SøknadGrunnlag> =
    dataTable
        .asMaps()
        .groupBy { parseLong(Domenebegrep.BEHANDLING_ID, it) }
        .map { (behandlingId, rader) ->
            val behandling = stepDefinition.behandlinger[behandlingId]
            if (behandling == null || !behandling.erSøknad()) {
                throw Feil("Behandling $behandlingId er ikke en søknadsbehandling")
            }

            val personopplysningGrunnlag =
                stepDefinition.personopplysningGrunnlagMap[behandlingId]
                    ?: throw Feil("Finner ikke personopplysninggrunnlag for behandling $behandlingId")

            val søkersMålform =
                rader
                    .find { parseAktørId(it) == personopplysningGrunnlag.søker.aktør.aktørId }
                    ?.let { parseValgfriEnum<Målform>(MÅLFORM, it) }
                    ?: Målform.NB

            val søkerMedOpplysningerDto =
                SøkerMedOpplysningerDto(
                    ident = personopplysningGrunnlag.søker.aktør.aktivFødselsnummer(),
                    målform = søkersMålform,
                )

            val barnMedOpplysningerDtoer =
                rader
                    .filter { parseAktørId(it) != personopplysningGrunnlag.søker.aktør.aktørId }
                    .map { rad ->
                        val aktørId = parseAktørId(rad)
                        val person = personopplysningGrunnlag.barna.find { aktørId == it.aktør.aktørId } ?: throw Feil("Finner ikke barn med aktørId $aktørId i personopplysninggrunnlag for behandling $behandlingId")
                        BarnMedOpplysningerDto(
                            ident = person.aktør.aktivFødselsnummer(),
                            navn = person.navn,
                            fødselsdato = person.fødselsdato,
                            inkludertISøknaden = parseValgfriBoolean(ER_INKLUDERT_I_SØKNADEN, rad) ?: true,
                            erFolkeregistrert = parseValgfriBoolean(ER_FOLKEREGISTRERT, rad) ?: true,
                        )
                    }

            val søknad =
                jsonMapper.writeValueAsString(
                    SøknadDto(
                        søkerMedOpplysninger = søkerMedOpplysningerDto,
                        barnaMedOpplysninger = barnMedOpplysningerDtoer,
                        endringAvOpplysningerBegrunnelse = "",
                    ),
                )

            behandlingId to
                SøknadGrunnlag(
                    behandlingId = behandlingId,
                    søknad = søknad,
                )
        }.toMap()

fun lagOvergangsordningAndeler(
    dataTable: DataTable,
    behandlingId: Long,
    behandlinger: MutableMap<Long, Behandling>,
    personGrunnlag: Map<Long, PersonopplysningGrunnlag>,
) = dataTable.asMaps().map { rad ->
    val aktørId = parseAktørId(rad)
    OvergangsordningAndel(
        id = 0,
        fom = parseDato(Domenebegrep.FRA_DATO, rad).toYearMonth(),
        tom = parseDato(Domenebegrep.TIL_DATO, rad).toYearMonth(),
        behandlingId = behandlinger.finnBehandling(behandlingId).id,
        person =
            personGrunnlag
                .finnPersonGrunnlagForBehandling(behandlingId)
                .personer
                .find { aktørId == it.aktør.aktørId }!!,
        deltBosted = parseBoolean(VedtaksperiodeMedBegrunnelserParser.DomenebegrepOvergangsordning.DELT_BOSTED, rad),
        antallTimer =
            parseBigDecimal(
                VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser.ANTALL_TIMER,
                rad,
            ),
    )
}

fun lagAndelerTilkjentYtelse(
    dataTable: DataTable,
    behandlingId: Long,
    behandlinger: MutableMap<Long, Behandling>,
    personGrunnlag: Map<Long, PersonopplysningGrunnlag>,
) = dataTable.asMaps().map { rad ->
    val aktørId = parseAktørId(rad)
    val beløp = parseInt(VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser.BELØP, rad)
    lagAndelTilkjentYtelse(
        stønadFom = parseDato(Domenebegrep.FRA_DATO, rad).toYearMonth(),
        stønadTom = parseDato(Domenebegrep.TIL_DATO, rad).toYearMonth(),
        behandling = behandlinger.finnBehandling(behandlingId),
        aktør =
            personGrunnlag
                .finnPersonGrunnlagForBehandling(behandlingId)
                .personer
                .find { aktørId == it.aktør.aktørId }!!
                .aktør,
        kalkulertUtbetalingsbeløp = beløp,
        ytelseType =
            parseValgfriEnum<YtelseType>(
                VedtaksperiodeMedBegrunnelserParser.DomenebegrepAndelTilkjentYtelse.YTELSE_TYPE,
                rad,
            ) ?: YtelseType.ORDINÆR_KONTANTSTØTTE,
        prosent =
            parseValgfriLong(
                VedtaksperiodeMedBegrunnelserParser.DomenebegrepEndretUtbetaling.PROSENT,
                rad,
            )?.toBigDecimal() ?: BigDecimal(100),
        sats =
            parseValgfriInt(
                VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser.SATS,
                rad,
            ) ?: beløp,
        nasjonaltPeriodebeløp =
            parseValgfriInt(
                VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser.NASJONALT_PERIODEBELØP,
                rad,
            ) ?: beløp,
        differanseberegnetPeriodebeløp =
            parseValgfriInt(
                VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser.DIFFERANSEBEREGNET_BELØP,
                rad,
            ),
    )
}

fun lagUregistrerteBarn(dataTable: DataTable) =
    dataTable.asMaps().map { rad ->
        val ident = parseValgfriString(VedtaksperiodeMedBegrunnelserParser.DomenebegrepPersongrunnlag.IDENT, rad) ?: RandomStringUtils.randomAlphanumeric(10)
        val fødselsDato = parseValgfriDato(VedtaksperiodeMedBegrunnelserParser.DomenebegrepPersongrunnlag.FØDSELSDATO, rad)
        val erInkludertISøknaden = parseValgfriBoolean(ER_INKLUDERT_I_SØKNADEN, rad) ?: false
        val erFolkeregistrert = parseValgfriBoolean(ER_FOLKEREGISTRERT, rad) ?: false
        BarnMedOpplysningerDto(
            ident = ident,
            fødselsdato = fødselsDato,
            inkludertISøknaden = erInkludertISøknaden,
            erFolkeregistrert = erFolkeregistrert,
        )
    }

fun leggBegrunnelserIVedtaksperiodene(
    dataTable: DataTable,
    vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>,
): List<VedtaksperiodeMedBegrunnelser> {
    val vedtaksperioderSomHarFåttBegrunnelser =
        dataTable.asMaps().map { rad ->
            val fom = parseValgfriDato(Domenebegrep.FRA_DATO, rad)
            val tom = parseValgfriDato(Domenebegrep.TIL_DATO, rad)

            val vedtaksperiode =
                vedtaksperioder.find { it.fom == fom && it.tom == tom }
                    ?: throw Feil(
                        "Ingen vedtaksperioder med Fom=$fom og Tom=$tom. " +
                            "Vedtaksperiodene var ${vedtaksperioder.map { "\n${it.fom?.tilddMMyyyy()} til ${it.tom?.tilddMMyyyy()}" }}",
                    )

            val nasjonaleOgFellesBegrunnelser =
                parseEnumListe<NasjonalEllerFellesBegrunnelse>(
                    VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser.STANDARDBEGRUNNELSER,
                    rad,
                ).map {
                    NasjonalEllerFellesBegrunnelseDB(
                        vedtaksperiodeMedBegrunnelser = vedtaksperiode,
                        nasjonalEllerFellesBegrunnelse = it,
                    )
                }
            val eøsBegrunnelser =
                parseEnumListe<EØSBegrunnelse>(
                    VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser.EØSBEGRUNNELSER,
                    rad,
                ).map {
                    EØSBegrunnelseDB(
                        vedtaksperiodeMedBegrunnelser = vedtaksperiode,
                        begrunnelse = it,
                    )
                }

            vedtaksperiode.copy(
                begrunnelser = nasjonaleOgFellesBegrunnelser.toMutableSet(),
                eøsBegrunnelser = eøsBegrunnelser.toMutableSet(),
            )
        }

    val vedtaksperioderUtenBegrunnelser =
        vedtaksperioder.filter { vedtaksperiodeUtenBegrunnelse ->
            vedtaksperioderSomHarFåttBegrunnelser.none {
                it.fom == vedtaksperiodeUtenBegrunnelse.fom &&
                    it.tom == vedtaksperiodeUtenBegrunnelse.tom &&
                    it.type == vedtaksperiodeUtenBegrunnelse.type
            }
        }

    return vedtaksperioderSomHarFåttBegrunnelser + vedtaksperioderUtenBegrunnelser
}
