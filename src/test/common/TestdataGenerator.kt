package no.nav.familie.ks.sak.data

import no.nav.commons.foedselsnummer.testutils.FoedselsnummerGenerator
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.fagsak.domene.Fagsak
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personident.Personident
import kotlin.random.Random

val fødselsnummerGenerator = FoedselsnummerGenerator()

fun randomFnr(): String = fødselsnummerGenerator.foedselsnummer().asString

fun randomAktør(fnr: String = randomFnr()): Aktør =
    Aktør(Random.nextLong(1000_000_000_000, 31_121_299_99999).toString()).also {
        it.personidenter.add(
            randomPersonident(it, fnr)
        )
    }

fun randomPersonident(aktør: Aktør, fnr: String = randomFnr()): Personident =
    Personident(fødselsnummer = fnr, aktør = aktør)

fun lagFagsak(aktør: Aktør = randomAktør(randomFnr())) = Fagsak(aktør = aktør)

fun lagBehandling(
    fagsak: Fagsak = lagFagsak(),
    type: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
    opprettetÅrsak: BehandlingÅrsak,
    kategori: BehandlingKategori = BehandlingKategori.NASJONAL
): Behandling = Behandling(
    fagsak = fagsak,
    type = type,
    opprettetÅrsak = opprettetÅrsak,
    kategori = kategori
).initBehandlingStegTilstand()
