package no.nav.familie.ks.sak.data

import no.nav.commons.foedselsnummer.testutils.FoedselsnummerGenerator
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personident.Personident
import kotlin.random.Random

object Testdata {

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
}
