package no.nav.familie.ks.sak.api.mapper

import no.nav.familie.kontrakter.felles.personopplysning.KJOENN
import no.nav.familie.ks.sak.api.dto.AnnenVurderingDto
import no.nav.familie.ks.sak.api.dto.ArbeidsfordelingResponsDto
import no.nav.familie.ks.sak.api.dto.BehandlingResponsDto
import no.nav.familie.ks.sak.api.dto.BehandlingStegTilstandResponsDto
import no.nav.familie.ks.sak.api.dto.PersonResponsDto
import no.nav.familie.ks.sak.api.dto.PersonResultatResponsDto
import no.nav.familie.ks.sak.api.dto.VilkårResultatDto
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.AnnenVurdering
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.VilkårResultat

object BehandlingMapper {

    fun lagBehandlingRespons(
        behandling: Behandling,
        arbeidsfordelingPåBehandling: ArbeidsfordelingPåBehandling,
        personer: List<Person>?,
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
            personer = personer?.map { lagPersonRespons(it) } ?: emptyList(),
            personResultater = personResultater?.map { lagPersonResultatRespons(it) } ?: emptyList()
        )

    private fun lagArbeidsfordelingRespons(arbeidsfordelingPåBehandling: ArbeidsfordelingPåBehandling) =
        ArbeidsfordelingResponsDto(
            behandlendeEnhetId = arbeidsfordelingPåBehandling.behandlendeEnhetId,
            behandlendeEnhetNavn = arbeidsfordelingPåBehandling.behandlendeEnhetNavn,
            manueltOverstyrt = arbeidsfordelingPåBehandling.manueltOverstyrt
        )

    private fun lagPersonRespons(person: Person) = PersonResponsDto(
        type = person.type,
        fødselsdato = person.fødselsdato,
        personIdent = person.aktør.aktivFødselsnummer(),
        navn = person.navn,
        kjønn = KJOENN.valueOf(person.kjønn.name),
        målform = person.målform,
        dødsfallDato = person.dødsfall?.dødsfallDato
    )

    private fun lagPersonResultatRespons(personResultat: PersonResultat) = PersonResultatResponsDto(
        personIdent = personResultat.aktør.aktivFødselsnummer(),
        vilkårResultater = personResultat.vilkårResultater.map { lagVilkårResultatRespons(it) },
        andreVurderinger = personResultat.andreVurderinger.map { lagAnnenVurderingRespons(it) }
    )

    private fun lagVilkårResultatRespons(vilkårResultat: VilkårResultat) = VilkårResultatDto(
        resultat = vilkårResultat.resultat,
        erAutomatiskVurdert = vilkårResultat.erAutomatiskVurdert,
        erEksplisittAvslagPåSøknad = vilkårResultat.erEksplisittAvslagPåSøknad,
        id = vilkårResultat.id,
        vilkårType = vilkårResultat.vilkårType,
        periodeFom = vilkårResultat.periodeFom,
        periodeTom = vilkårResultat.periodeTom,
        begrunnelse = vilkårResultat.begrunnelse,
        endretAv = vilkårResultat.endretAv,
        endretTidspunkt = vilkårResultat.endretTidspunkt,
        behandlingId = vilkårResultat.behandlingId,
        erVurdert = vilkårResultat.resultat != Resultat.IKKE_VURDERT || vilkårResultat.versjon > 0,
        avslagBegrunnelser = vilkårResultat.standardbegrunnelser,
        vurderesEtter = vilkårResultat.vurderesEtter,
        utdypendeVilkårsvurderinger = vilkårResultat.utdypendeVilkårsvurderinger
    )

    private fun lagAnnenVurderingRespons(annenVurdering: AnnenVurdering) = AnnenVurderingDto(
        id = annenVurdering.id,
        resultat = annenVurdering.resultat,
        type = annenVurdering.type,
        begrunnelse = annenVurdering.begrunnelse
    )
}
