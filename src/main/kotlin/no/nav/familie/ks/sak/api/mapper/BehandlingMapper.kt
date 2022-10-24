package no.nav.familie.ks.sak.api.mapper

import no.nav.familie.kontrakter.felles.personopplysning.KJOENN
import no.nav.familie.ks.sak.api.dto.ArbeidsfordelingResponsDto
import no.nav.familie.ks.sak.api.dto.BehandlingResponsDto
import no.nav.familie.ks.sak.api.dto.BehandlingStegTilstandResponsDto
import no.nav.familie.ks.sak.api.dto.PersonResponsDto
import no.nav.familie.ks.sak.api.dto.SøknadDto
import no.nav.familie.ks.sak.api.mapper.RegisterHistorikkMapper.lagRegisterHistorikkResponsDto
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person

object BehandlingMapper {

    fun lagBehandlingRespons(
        behandling: Behandling,
        arbeidsfordelingPåBehandling: ArbeidsfordelingPåBehandling,
        søknadsgrunnlag: SøknadDto?,
        personer: List<PersonResponsDto>,
        personResultater: List<PersonResultat>?
    ) =
        BehandlingResponsDto(
            behandlingId = behandling.id,
            steg = behandling.steg,
            stegTilstand = behandling.behandlingStegTilstand.map {
                BehandlingStegTilstandResponsDto(
                    it.behandlingSteg,
                    it.behandlingStegStatus
                )
            },
            status = behandling.status,
            resultat = behandling.resultat,
            type = behandling.type,
            kategori = behandling.kategori,
            årsak = behandling.opprettetÅrsak,
            opprettetTidspunkt = behandling.opprettetTidspunkt,
            endretAv = behandling.endretAv,
            arbeidsfordelingPåBehandling = lagArbeidsfordelingRespons(arbeidsfordelingPåBehandling),
            søknadsgrunnlag = søknadsgrunnlag,
            personer = personer,
            personResultater = personResultater?.map { VilkårsvurderingMapper.lagPersonResultatRespons(it) }
                ?: emptyList()
        )

    private fun lagArbeidsfordelingRespons(arbeidsfordelingPåBehandling: ArbeidsfordelingPåBehandling) =
        ArbeidsfordelingResponsDto(
            behandlendeEnhetId = arbeidsfordelingPåBehandling.behandlendeEnhetId,
            behandlendeEnhetNavn = arbeidsfordelingPåBehandling.behandlendeEnhetNavn,
            manueltOverstyrt = arbeidsfordelingPåBehandling.manueltOverstyrt
        )

    fun lagPersonRespons(person: Person, landKodeOgLandNavn: Map<String, String>) = PersonResponsDto(
        type = person.type,
        fødselsdato = person.fødselsdato,
        personIdent = person.aktør.aktivFødselsnummer(),
        navn = person.navn,
        kjønn = KJOENN.valueOf(person.kjønn.name),
        målform = person.målform,
        dødsfallDato = person.dødsfall?.dødsfallDato,
        registerhistorikk = lagRegisterHistorikkResponsDto(person, landKodeOgLandNavn)
    )
}
