package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering

import no.nav.familie.ks.sak.api.dto.EndreVilkårResultatDto
import no.nav.familie.ks.sak.api.dto.NyttVilkårDto
import no.nav.familie.ks.sak.api.dto.VedtakBegrunnelseTilknyttetVilkårResponseDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.integrasjon.sanity.SanityService
import no.nav.familie.ks.sak.integrasjon.secureLogger
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.AnnenVurderingType
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.BegrunnelseType
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class VilkårsvurderingService(
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val sanityService: SanityService,
    private val personidentService: PersonidentService
) {

    @Transactional
    fun opprettVilkårsvurdering(
        behandling: Behandling,
        forrigeBehandlingSomErVedtatt: Behandling?
    ): Vilkårsvurdering {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppretter vilkårsvurdering for behandling ${behandling.id}")

        val aktivVilkårsvurdering = finnAktivVilkårsvurdering(behandling.id)

        val personopplysningGrunnlag =
            personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandling.id)

        val initiellVilkårsvurdering = genererInitiellVilkårsvurdering(behandling, personopplysningGrunnlag)

        if (forrigeBehandlingSomErVedtatt != null) {
            initiellVilkårsvurdering.kopierOverOppfylteOgIkkeAktuelleResultaterFraForrigeBehandling(
                vilkårsvurderingForrigeBehandling = hentAktivVilkårsvurderingForBehandling(forrigeBehandlingSomErVedtatt.id)
            )
        }

        aktivVilkårsvurdering?.finnOpplysningspliktVilkår()?.let {
            initiellVilkårsvurdering.personResultater.single { it.erSøkersResultater() }
                .leggTilBlankAnnenVurdering(AnnenVurderingType.OPPLYSNINGSPLIKT)
        }

        initiellVilkårsvurdering.oppdaterMedDødsdatoer(
            personopplysningGrunnlag = personopplysningGrunnlag
        )

        return lagreVilkårsvurdering(initiellVilkårsvurdering, aktivVilkårsvurdering)
    }

    private fun lagreVilkårsvurdering(
        vilkårsvurdering: Vilkårsvurdering,
        aktivVilkårsvurdering: Vilkårsvurdering?
    ): Vilkårsvurdering {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} lagrer vilkårsvurdering $vilkårsvurdering")
        aktivVilkårsvurdering?.let { vilkårsvurderingRepository.saveAndFlush(it.also { it.aktiv = false }) }

        return vilkårsvurderingRepository.save(vilkårsvurdering)
    }

    fun finnAktivVilkårsvurdering(behandlingId: Long): Vilkårsvurdering? =
        vilkårsvurderingRepository.finnAktivForBehandling(behandlingId)

    fun hentVilkårsbegrunnelser(): Map<BegrunnelseType, List<VedtakBegrunnelseTilknyttetVilkårResponseDto>> =
        standardbegrunnelserTilNedtrekksmenytekster(sanityService.hentSanityBegrunnelser()) + eøsStandardbegrunnelserTilNedtrekksmenytekster(
            sanityService.hentSanityEØSBegrunnelser()
        )

    @Transactional
    fun endreVilkårPåBehandling(
        behandlingId: Long,
        endreVilkårResultatDto: EndreVilkårResultatDto
    ) {
        val vilkårsvurdering = hentAktivVilkårsvurderingForBehandling(behandlingId)
        val personResultat =
            hentPersonResultatForPerson(vilkårsvurdering.personResultater, endreVilkårResultatDto.personIdent)

        val eksisterendeVilkårResultater = personResultat.vilkårResultater

        val nyeOgEndredeVilkårResultater =
            endreVilkårResultat(eksisterendeVilkårResultater.toList(), endreVilkårResultatDto.endretVilkårResultat)

        // Det er ikke nødvendig å save Vilkårresultatene eksplitt pga @Transactional
        eksisterendeVilkårResultater.clear()
        eksisterendeVilkårResultater.addAll(nyeOgEndredeVilkårResultater)
    }

    @Transactional
    fun opprettNyttVilkårPåBehandling(behandlingId: Long, nyttVilkårDto: NyttVilkårDto) {
        val vilkårsvurdering = hentAktivVilkårsvurderingForBehandling(behandlingId)
        val personResultat =
            hentPersonResultatForPerson(vilkårsvurdering.personResultater, nyttVilkårDto.personIdent)

        val eksisterendeVilkårResultater = personResultat.vilkårResultater

        val nyttVilkårResultat = opprettNyttVilkårResultat(personResultat, nyttVilkårDto.vilkårType)

        // Det er ikke nødvendig å save Vilkårresultatene eksplitt pga @Transactional
        eksisterendeVilkårResultater.add(nyttVilkårResultat)
    }

    @Transactional
    fun slettVilkårPåBehandling(behandlingId: Long, vilkårId: Long, aktør: Aktør) {
        val vilkårsvurdering = hentAktivVilkårsvurderingForBehandling(behandlingId)

        val personResultat =
            hentPersonResultatForPerson(vilkårsvurdering.personResultater, aktør.aktivFødselsnummer())

        val eksisterendeVilkårResultater = personResultat.vilkårResultater

        val vilkårResultatSomSkalSlettes = eksisterendeVilkårResultater.find { it.id == vilkårId }
            ?: throw Feil(
                message = "Prøver å slette et vilkår som ikke finnes",
                frontendFeilmelding = "Vilkåret du prøver å slette finnes ikke i systemet."
            )

        eksisterendeVilkårResultater.remove(vilkårResultatSomSkalSlettes)

        val perioderMedSammeVilkårType = eksisterendeVilkårResultater
            .filter { it.vilkårType == vilkårResultatSomSkalSlettes.vilkårType && it.id != vilkårResultatSomSkalSlettes.id }

        // Vi oppretter initiell vilkår dersom det ikke finnes flere av samme type.
        if (perioderMedSammeVilkårType.isEmpty()) {
            val nyttVilkårMedNullstilteFelter =
                opprettNyttVilkårResultat(personResultat, vilkårResultatSomSkalSlettes.vilkårType)

            eksisterendeVilkårResultater.add(nyttVilkårMedNullstilteFelter)
        }
    }

    fun hentAktivVilkårsvurderingForBehandling(behandlingId: Long): Vilkårsvurdering =
        finnAktivVilkårsvurdering(behandlingId)
            ?: throw Feil("Fant ikke vilkårsvurdering knyttet til behandling=$behandlingId")

    @Transactional
    fun oppdater(vilkårsvurdering: Vilkårsvurdering): Vilkårsvurdering {
        secureLogger.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppdaterer vilkårsvurdering $vilkårsvurdering")
        return vilkårsvurderingRepository.save(vilkårsvurdering)
    }

    @Transactional
    fun fyllUtVilkårsvurdering(behandlingId: Long) {
        val vilkårsvurdering = vilkårsvurderingRepository.finnAktivForBehandling(behandlingId)

        val persongrunnlag = personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandlingId)

        vilkårsvurdering?.personResultater?.forEach { personResultat ->

            val oppdaterteVilkår = personResultat.vilkårResultater.map { vilkårResultat ->
                if (vilkårResultat.periodeFom == null || vilkårResultat.resultat != Resultat.OPPFYLT) {
                    vilkårResultat.kopier(
                        periodeFom = persongrunnlag.personer.find { it.aktør == personResultat.aktør }?.fødselsdato,
                        resultat = Resultat.OPPFYLT
                    )
                } else vilkårResultat
            }

            personResultat.vilkårResultater.clear()
            personResultat.vilkårResultater.addAll(oppdaterteVilkår)
        }
    }

    private fun hentPersonResultatForPerson(
        personResultater: Set<PersonResultat>,
        personIdent: String
    ): PersonResultat {
        val aktør = personidentService.hentAktør(personIdent)

        return personResultater.find { it.aktør == aktør } ?: throw Feil(
            message = "Fant ikke vilkårsvurdering for person",
            frontendFeilmelding = "Fant ikke vilkårsvurdering for person med ident $personIdent"
        )
    }

    companion object {

        val logger = LoggerFactory.getLogger(VilkårsvurderingService::class.java)
    }
}
