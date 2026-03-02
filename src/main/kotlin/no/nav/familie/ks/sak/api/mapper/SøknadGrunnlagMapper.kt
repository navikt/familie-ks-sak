package no.nav.familie.ks.sak.api.mapper

import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.ks.sak.api.dto.SøknadDto
import no.nav.familie.ks.sak.kjerne.behandling.steg.registrersøknad.domene.SøknadGrunnlag

object SøknadGrunnlagMapper {
    fun SøknadGrunnlag.tilSøknadDto(): SøknadDto = jsonMapper.readValue(this.søknad, SøknadDto::class.java)
}
