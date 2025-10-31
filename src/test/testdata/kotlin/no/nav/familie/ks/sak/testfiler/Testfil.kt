package no.nav.familie.ks.sak.testfiler

object Testfil {
    val TEST_PDF = this::class.java.getResource("/dokument/mockvedtak.pdf")!!.readBytes()
}
