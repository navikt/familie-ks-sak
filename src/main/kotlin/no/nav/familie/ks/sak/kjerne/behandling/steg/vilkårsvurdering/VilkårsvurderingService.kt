package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering

import no.nav.familie.ks.sak.api.dto.EndreVilkårResultatDto
import no.nav.familie.ks.sak.api.dto.NyttVilkårDto
import no.nav.familie.ks.sak.api.dto.VedtakBegrunnelseTilknyttetVilkårResponseDto
import no.nav.familie.ks.sak.common.BehandlingId
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.integrasjon.sanity.SanityService
import no.nav.familie.ks.sak.integrasjon.secureLogger
import no.nav.familie.ks.sak.kjerne.adopsjon.AdopsjonService
import no.nav.familie.ks.sak.kjerne.adopsjon.AdopsjonValidator
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.AnnenVurderingType
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
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
    private val personidentService: PersonidentService,
    private val adopsjonService: AdopsjonService,
    private val adopsjonValidator: AdopsjonValidator,
) {
    @Transactional
    fun opprettVilkårsvurdering(
        behandling: Behandling,
        forrigeBehandlingSomErVedtatt: Behandling?,
    ): Vilkårsvurdering {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppretter vilkårsvurdering for behandling ${behandling.id}")

        val aktivVilkårsvurdering = finnAktivVilkårsvurdering(behandling.id)
        val vilkårsvurderingFraForrigeBehandling = forrigeBehandlingSomErVedtatt?.let { hentAktivVilkårsvurderingForBehandling(forrigeBehandlingSomErVedtatt.id) }

        val personopplysningGrunnlag =
            personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandling.id)

        val adopsjonerIBehandling = adopsjonService.hentAlleAdopsjonerForBehandling(BehandlingId(behandling.id))

        val initiellVilkårsvurdering =
            genererInitiellVilkårsvurdering(
                behandling,
                vilkårsvurderingFraForrigeBehandling,
                personopplysningGrunnlag,
                adopsjonerIBehandling,
            )

        vilkårsvurderingFraForrigeBehandling?.let {
            initiellVilkårsvurdering.kopierResultaterFraForrigeBehandling(
                vilkårsvurderingForrigeBehandling = it,
            )
        }

        aktivVilkårsvurdering?.finnOpplysningspliktVilkår()?.let {
            initiellVilkårsvurdering.personResultater
                .single { it.erSøkersResultater() }
                .leggTilBlankAnnenVurdering(AnnenVurderingType.OPPLYSNINGSPLIKT)
        }

        initiellVilkårsvurdering.oppdaterMedDødsdatoer(
            personopplysningGrunnlag = personopplysningGrunnlag,
        )

        return lagreVilkårsvurdering(initiellVilkårsvurdering, aktivVilkårsvurdering)
    }

    private fun lagreVilkårsvurdering(
        vilkårsvurdering: Vilkårsvurdering,
        aktivVilkårsvurdering: Vilkårsvurdering?,
    ): Vilkårsvurdering {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} lagrer vilkårsvurdering $vilkårsvurdering")
        aktivVilkårsvurdering?.let { vilkårsvurderingRepository.saveAndFlush(it.also { it.aktiv = false }) }

        return vilkårsvurderingRepository.save(vilkårsvurdering)
    }

    fun erFremtidigOpphørIBehandling(behandling: Behandling): Boolean {
        val vilkårsvurdering =
            finnAktivVilkårsvurdering(behandling.id)
                ?: throw Feil("Fant ingen vilkårsvurdering for behandling ${behandling.id}")

        val vilkårResultaterPåBehandling = vilkårsvurdering.personResultater.flatMap { it.vilkårResultater }

        return vilkårResultaterPåBehandling.filter { it.vilkårType == Vilkår.BARNEHAGEPLASS }.any {
            it.søkerHarMeldtFraOmBarnehageplass == true
        }
    }

    fun finnAktivVilkårsvurdering(behandlingId: Long): Vilkårsvurdering? = vilkårsvurderingRepository.finnAktivForBehandling(behandlingId)

    fun hentVilkårsbegrunnelser(): Map<BegrunnelseType, List<VedtakBegrunnelseTilknyttetVilkårResponseDto>> = standardbegrunnelserTilNedtrekksmenytekster(sanityService.hentSanityBegrunnelser())

    @Transactional
    fun endreVilkårPåBehandling(
        behandlingId: Long,
        endreVilkårResultatDto: EndreVilkårResultatDto,
    ) {
        val vilkårsvurdering = hentAktivVilkårsvurderingForBehandling(behandlingId)
        val personResultat =
            hentPersonResultatForPerson(vilkårsvurdering.personResultater, endreVilkårResultatDto.personIdent)

        if (endreVilkårResultatDto.endretVilkårResultat.vilkårType == Vilkår.BARNETS_ALDER) {
            adopsjonValidator.validerGyldigAdopsjonstilstandForBarnetsAlderVilkår(
                vilkår = endreVilkårResultatDto.endretVilkårResultat.vilkårType,
                utdypendeVilkårsvurdering = endreVilkårResultatDto.endretVilkårResultat.utdypendeVilkårsvurderinger,
                nyAdopsjonsdato = endreVilkårResultatDto.adopsjonsdato,
                barnetsFødselsdato =
                    personopplysningGrunnlagService
                        .hentAktivPersonopplysningGrunnlagThrows(behandlingId)
                        .barna
                        .firstOrNull { it.aktør == personResultat.aktør }
                        ?.fødselsdato ?: throw Feil("Finner ikke barn med aktørId ${personResultat.aktør} i personopplysningsgrunnlaget"),
            )
            adopsjonService.oppdaterAdopsjonsdato(behandlingId = BehandlingId(behandlingId), aktør = personResultat.aktør, nyAdopsjonsdato = endreVilkårResultatDto.adopsjonsdato)
        }

        val eksisterendeVilkårResultater = personResultat.vilkårResultater

        val nyeOgEndredeVilkårResultater =
            endreVilkårResultat(eksisterendeVilkårResultater.toList(), endreVilkårResultatDto.endretVilkårResultat)

        val vilkårResultaterEtterFiltreringAvLovligOpphold = fjernEllerLeggTilLovligOppholdVilkår(personResultat, nyeOgEndredeVilkårResultater)

        // Det er ikke nødvendig å save Vilkårresultatene eksplitt pga @Transactional
        eksisterendeVilkårResultater.clear()
        eksisterendeVilkårResultater.addAll(vilkårResultaterEtterFiltreringAvLovligOpphold)
    }

    @Transactional
    fun opprettNyttVilkårPåBehandling(
        behandlingId: Long,
        nyttVilkårDto: NyttVilkårDto,
    ) {
        val vilkårsvurdering = hentAktivVilkårsvurderingForBehandling(behandlingId)
        val personResultat =
            hentPersonResultatForPerson(vilkårsvurdering.personResultater, nyttVilkårDto.personIdent)

        val eksisterendeVilkårResultater = personResultat.vilkårResultater

        val nyttVilkårResultat = opprettNyttVilkårResultat(personResultat, nyttVilkårDto.vilkårType)

        val vilkårResultaterEtterFiltreringAvLovligOpphold = fjernEllerLeggTilLovligOppholdVilkår(personResultat, (eksisterendeVilkårResultater + nyttVilkårResultat).toList())

        // Det er ikke nødvendig å save Vilkårresultatene eksplitt pga @Transactional
        eksisterendeVilkårResultater.clear()
        eksisterendeVilkårResultater.addAll(vilkårResultaterEtterFiltreringAvLovligOpphold)
    }

    @Transactional
    fun slettVilkårPåBehandling(
        behandlingId: Long,
        vilkårId: Long,
        aktør: Aktør,
    ) {
        val vilkårsvurdering = hentAktivVilkårsvurderingForBehandling(behandlingId)

        val personResultat =
            hentPersonResultatForPerson(vilkårsvurdering.personResultater, aktør.aktivFødselsnummer())

        val eksisterendeVilkårResultater = personResultat.vilkårResultater

        val vilkårResultatSomSkalSlettes =
            eksisterendeVilkårResultater.find { it.id == vilkårId }
                ?: throw Feil(
                    message = "Prøver å slette et vilkår som ikke finnes",
                    frontendFeilmelding = "Vilkåret du prøver å slette finnes ikke i systemet.",
                )

        eksisterendeVilkårResultater.remove(vilkårResultatSomSkalSlettes)

        val perioderMedSammeVilkårType =
            eksisterendeVilkårResultater
                .filter { it.vilkårType == vilkårResultatSomSkalSlettes.vilkårType && it.id != vilkårResultatSomSkalSlettes.id }

        // Vi oppretter initiell vilkår dersom det ikke finnes flere av samme type.
        if (perioderMedSammeVilkårType.isEmpty()) {
            val nyttVilkårMedNullstilteFelter =
                opprettNyttVilkårResultat(personResultat, vilkårResultatSomSkalSlettes.vilkårType)

            eksisterendeVilkårResultater.add(nyttVilkårMedNullstilteFelter)
        }

        val vilkårResultaterEtterFiltreringAvLovligOpphold = fjernEllerLeggTilLovligOppholdVilkår(personResultat, (eksisterendeVilkårResultater).toList())

        // Det er ikke nødvendig å save Vilkårresultatene eksplitt pga @Transactional
        eksisterendeVilkårResultater.clear()
        eksisterendeVilkårResultater.addAll(vilkårResultaterEtterFiltreringAvLovligOpphold)
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

            val oppdaterteVilkår =
                personResultat.vilkårResultater.map { vilkårResultat ->
                    if (vilkårResultat.periodeFom == null || vilkårResultat.resultat != Resultat.OPPFYLT) {
                        vilkårResultat.kopier(
                            periodeFom = persongrunnlag.personer.find { it.aktør == personResultat.aktør }?.fødselsdato,
                            resultat = Resultat.OPPFYLT,
                        )
                    } else {
                        vilkårResultat
                    }
                }

            personResultat.vilkårResultater.clear()
            personResultat.vilkårResultater.addAll(oppdaterteVilkår)
        }
    }

    @Transactional
    fun fjernEllerLeggTilLovligOppholdVilkår(
        personResultat: PersonResultat,
        vilkårResultater: List<VilkårResultat>,
    ): List<VilkårResultat> {
        if (!personResultat.erSøkersResultater()) return vilkårResultater

        val bosattIRiketVilkår = vilkårResultater.filter { it.vilkårType == Vilkår.BOSATT_I_RIKET }
        val finnesBosattIRiketVilkårVurdertEtterEøs = bosattIRiketVilkår.any { it.vurderesEtter == Regelverk.EØS_FORORDNINGEN }
        val lovligOppholdVilkårFinnesAllerede = vilkårResultater.any { it.vilkårType == Vilkår.LOVLIG_OPPHOLD }

        return when {
            finnesBosattIRiketVilkårVurdertEtterEøs && !lovligOppholdVilkårFinnesAllerede -> {
                vilkårResultater + opprettNyttVilkårResultat(personResultat, Vilkår.LOVLIG_OPPHOLD)
            }

            lovligOppholdVilkårFinnesAllerede && !finnesBosattIRiketVilkårVurdertEtterEøs -> vilkårResultater.filter { it.vilkårType != Vilkår.LOVLIG_OPPHOLD }
            else -> return vilkårResultater
        }
    }

    private fun hentPersonResultatForPerson(
        personResultater: Set<PersonResultat>,
        personIdent: String,
    ): PersonResultat {
        val aktør = personidentService.hentAktør(personIdent)

        return personResultater.find { it.aktør == aktør } ?: throw Feil(
            message = "Fant ikke vilkårsvurdering for person",
            frontendFeilmelding = "Fant ikke vilkårsvurdering for person med ident $personIdent",
        )
    }

    companion object {
        val logger = LoggerFactory.getLogger(VilkårsvurderingService::class.java)
    }
}
