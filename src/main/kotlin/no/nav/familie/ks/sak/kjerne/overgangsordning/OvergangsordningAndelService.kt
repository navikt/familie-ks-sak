package no.nav.familie.ks.sak.kjerne.overgangsordning

import no.nav.familie.ks.sak.api.dto.OvergangsordningAndelDto
import no.nav.familie.ks.sak.common.BehandlingId
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.beregning.BeregningService
import no.nav.familie.ks.sak.kjerne.overgangsordning.OvergangsordningAndelValidator.validerAtBarnehagevilkårErOppfyltIOvergangsordningAndelPeriode
import no.nav.familie.ks.sak.kjerne.overgangsordning.OvergangsordningAndelValidator.validerFomDato
import no.nav.familie.ks.sak.kjerne.overgangsordning.OvergangsordningAndelValidator.validerIngenOverlappMedEksisterendeOvergangsordningAndeler
import no.nav.familie.ks.sak.kjerne.overgangsordning.OvergangsordningAndelValidator.validerTomDato
import no.nav.familie.ks.sak.kjerne.overgangsordning.domene.OvergangsordningAndel
import no.nav.familie.ks.sak.kjerne.overgangsordning.domene.OvergangsordningAndelRepository
import no.nav.familie.ks.sak.kjerne.overgangsordning.domene.tilPerioder
import no.nav.familie.ks.sak.kjerne.overgangsordning.domene.utfyltePerioder
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.slåSammenLikePerioder
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OvergangsordningAndelService(
    private val overgangsordningAndelRepository: OvergangsordningAndelRepository,
    private val beregningService: BeregningService,
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val overgangsordningAndelOppdatertAbonnementer: List<OvergangsordningAndelerOppdatertAbonnent> = emptyList(),
) {
    fun hentOvergangsordningAndeler(behandlingId: Long) = overgangsordningAndelRepository.hentOvergangsordningAndelerForBehandling(behandlingId)

    fun hentUtfylteOvergangsordningAndeler(behandlingId: Long) = hentOvergangsordningAndeler(behandlingId).utfyltePerioder()

    fun hentOvergangsordningAndelerForPerson(
        behandlingId: Long,
        person: Person,
    ): List<OvergangsordningAndel> = hentOvergangsordningAndeler(behandlingId).filter { it.person == person }

    @Transactional
    fun opprettTomOvergangsordningAndel(behandling: Behandling): OvergangsordningAndel {
        if (!behandling.erOvergangsordning()) {
            throw FunksjonellFeil("Kan ikke opprette overgangsordningandel på behandling som ikke har årsak overgangsordning")
        }

        return overgangsordningAndelRepository.save(OvergangsordningAndel(behandlingId = behandling.id))
    }

    @Transactional
    fun oppdaterOvergangsordningAndelOgOppdaterTilkjentYtelse(
        behandling: Behandling,
        overgangsordningAndelId: Long,
        overgangsordningAndelRequestDto: OvergangsordningAndelDto,
    ) {
        val overgangsordningAndel = finnOvergangsordningAndel(overgangsordningAndelId)
        val personopplysningGrunnlag = personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandling.id)
        val person = personopplysningGrunnlag.personer.single { it.aktør.aktivFødselsnummer() == overgangsordningAndelRequestDto.personIdent }
        val vilkårsvurdering by lazy { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id) }
        val andreUtfylteOvergangsordningAndelerPåBehandling by lazy {
            hentUtfylteOvergangsordningAndeler(behandling.id).filter { it.id != overgangsordningAndelId }
        }

        val utfyltOvergangsordningAndel =
            overgangsordningAndel
                .fraOvergangsordningAndelDto(overgangsordningAndelRequestDto, person)
                .tilUtfyltOvergangsordningAndel()

        val barnehageplassVilkår =
            vilkårsvurdering
                .hentPersonResultaterTilAktør(person.aktør.aktørId)
                .filter { it.vilkårType == Vilkår.BARNEHAGEPLASS }

        validerFomDato(
            overgangsordningAndel = utfyltOvergangsordningAndel,
            gyldigFom = beregnGyldigFom(person),
        )

        validerTomDato(
            overgangsordningAndel = utfyltOvergangsordningAndel,
            gyldigTom = beregnGyldigTom(person),
        )

        validerIngenOverlappMedEksisterendeOvergangsordningAndeler(
            nyOvergangsordningAndel = utfyltOvergangsordningAndel,
            eksisterendeUtfylteOvergangsordningAndeler = andreUtfylteOvergangsordningAndelerPåBehandling,
        )

        validerAtBarnehagevilkårErOppfyltIOvergangsordningAndelPeriode(
            overgangsordningAndel = utfyltOvergangsordningAndel,
            barnehageplassVilkår = barnehageplassVilkår,
        )

        slåSammenOgOppdaterOvergangsordningAndeler(behandling, person)

        beregningService.oppdaterTilkjentYtelsePåBehandling(
            behandling = behandling,
            personopplysningGrunnlag = personopplysningGrunnlag,
            vilkårsvurdering = vilkårsvurdering,
        )

        overgangsordningAndelOppdatertAbonnementer.forEach { abonnent ->
            abonnent.tilpassKompetanserTilOvergangsordningAndeler(
                behandlingId = BehandlingId(behandling.id),
            )
        }
    }

    @Transactional
    fun fjernOvergangsordningAndelOgOppdaterTilkjentYtelse(
        behandling: Behandling,
        overgangsordningAndelId: Long,
    ) {
        overgangsordningAndelRepository.deleteById(overgangsordningAndelId)

        val personopplysningGrunnlag = personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandlingId = behandling.id)
        val vilkårsvurdering = vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id)

        beregningService.oppdaterTilkjentYtelsePåBehandling(
            behandling = behandling,
            personopplysningGrunnlag = personopplysningGrunnlag,
            vilkårsvurdering = vilkårsvurdering,
        )

        overgangsordningAndelOppdatertAbonnementer.forEach { abonnent ->
            abonnent.tilpassKompetanserTilOvergangsordningAndeler(
                behandlingId = BehandlingId(behandling.id),
            )
        }
    }

    @Transactional
    fun kopierOvergangsordningAndelFraForrigeBehandling(
        behandling: Behandling,
        forrigeBehandling: Behandling,
    ) = hentOvergangsordningAndeler(forrigeBehandling.id).map {
        overgangsordningAndelRepository.save(it.copy(id = 0, behandlingId = behandling.id))
    }

    private fun finnOvergangsordningAndel(overgangsordningAndelId: Long) =
        overgangsordningAndelRepository.finnOvergangsordningAndel(overgangsordningAndelId)
            ?: throw FunksjonellFeil(melding = "Fant ikke overgangsordningandel med id $overgangsordningAndelId")

    private fun slåSammenOgOppdaterOvergangsordningAndeler(
        behandling: Behandling,
        person: Person,
    ) {
        val overgangsordningAndeler = hentOvergangsordningAndelerForPerson(behandling.id, person)
        val sammenslåtteOvergangsordningAndeler = overgangsordningAndeler.slåSammenLikePerioder()
        val utfyltePerioder = overgangsordningAndeler.filter { it.erObligatoriskeFelterUtfylt() }

        overgangsordningAndelRepository.deleteAll(utfyltePerioder)
        overgangsordningAndelRepository.saveAllAndFlush(sammenslåtteOvergangsordningAndeler)
    }

    private fun List<OvergangsordningAndel>.slåSammenLikePerioder(): List<OvergangsordningAndel> =
        utfyltePerioder()
            .tilPerioder()
            .tilTidslinje()
            .slåSammenLikePerioder()
            .tilPerioderIkkeNull()
            .map { it.verdi.tilOvergangsordningAndel(fom = it.fom!!.toYearMonth(), tom = it.tom!!.toYearMonth()) }
}

interface OvergangsordningAndelerOppdatertAbonnent {
    fun tilpassKompetanserTilOvergangsordningAndeler(
        behandlingId: BehandlingId,
    )
}
