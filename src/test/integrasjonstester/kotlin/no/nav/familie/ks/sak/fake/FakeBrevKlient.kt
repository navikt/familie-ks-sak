package no.nav.familie.ks.sak.fake
import no.nav.familie.ks.sak.kjerne.brev.BrevKlient
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.BegrunnelseDtoMedData
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.BrevDto
import no.nav.familie.ks.sak.testfiler.Testfil.TEST_PDF
import org.springframework.web.client.RestTemplate

class FakeBrevKlient :
    BrevKlient(
        familieBrevUri = "brev_uri_mock",
        sanityDataset = "",
        restOperations = RestTemplate(),
    ) {
    val genererteBrev = mutableListOf<BrevDto>()

    override fun genererBrev(
        målform: String,
        brev: BrevDto,
    ): ByteArray {
        genererteBrev.add(brev)
        return TEST_PDF
    }

    override fun hentBegrunnelsestekst(
        begrunnelseData: BegrunnelseDtoMedData,
    ): String = "Dummytekst for ${begrunnelseData.apiNavn}"
}
