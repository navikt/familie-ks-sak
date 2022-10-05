package no.nav.familie.ks.sak.kjerne.vilkårsvurdering

import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class VilkårsvurderingService(
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService
) {

    fun opprettVilkårsvurdering(behandling: Behandling, forrigeBehandlingSomErVedtatt: Behandling?): Vilkårsvurdering {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppretter vilkårsvurdering for behandling ${behandling.id}")

        val aktivVilkårsvurdering = hentAktivVilkårsvurdering(behandling.id)

        val personopplysningGrunnlag =
            personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandling.id)
        val initiellVilkårsvurdering = genererInitiellVilkårsvurdering(behandling, personopplysningGrunnlag)

        val finnesVilkårsvurderingPåInneværendeBehandling = aktivVilkårsvurdering != null
        val førsteVilkårsvurderingPåBehandlingOgFinnesTidligereVedtattBehandling =
            forrigeBehandlingSomErVedtatt != null && !finnesVilkårsvurderingPåInneværendeBehandling

        var vilkårsvurdering = initiellVilkårsvurdering

        if (førsteVilkårsvurderingPåBehandlingOgFinnesTidligereVedtattBehandling) {
            // vilkårsvurdering = genererVilkårsvurderingBasertPåTidligereVilkårsvurdering(initiellVilkårsvurdering, forrigeBehandlingSomErVedtatt)
            // TODO: implementer generering av vilkårsvurdering basert på tidligere vilkårsvurdering
        }

        return lagreVilkårsvurdering(vilkårsvurdering, aktivVilkårsvurdering)
    }

    fun lagreVilkårsvurdering(
        vilkårsvurdering: Vilkårsvurdering,
        aktivVilkårsvurdering: Vilkårsvurdering?
    ): Vilkårsvurdering {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} lagrer vilkårsvurdering $vilkårsvurdering")
        aktivVilkårsvurdering?.let { vilkårsvurderingRepository.saveAndFlush(it.also { it.aktiv = false }) }

        return vilkårsvurderingRepository.save(vilkårsvurdering)
    }

    fun hentAktivVilkårsvurdering(behandlingId: Long): Vilkårsvurdering? =
        vilkårsvurderingRepository.finnAktiv(behandlingId)

    private fun genererInitiellVilkårsvurdering(
        behandling: Behandling,
        personopplysningGrunnlag: PersonopplysningGrunnlag
    ): Vilkårsvurdering {
        return Vilkårsvurdering(behandling = behandling).apply {
            personResultater = personopplysningGrunnlag.personer.map { person ->
                val personResultat = PersonResultat(vilkårsvurdering = this, aktør = person.aktør)

                val vilkårForPerson = Vilkår.hentVilkårFor(person.type)

                val vilkårResultater = vilkårForPerson.map { vilkår ->
                    VilkårResultat(
                        personResultat = personResultat,
                        erAutomatiskVurdert = true,
                        resultat = Resultat.IKKE_VURDERT,
                        vilkårType = vilkår,
                        begrunnelse = "",
                        behandlingId = behandling.id
                    )
                }.toSortedSet(VilkårResultat.VilkårResultatComparator)

                personResultat.setSortedVilkårResultater(vilkårResultater)

                personResultat
            }.toSet()
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(VilkårsvurderingService::class.java)
    }
}
