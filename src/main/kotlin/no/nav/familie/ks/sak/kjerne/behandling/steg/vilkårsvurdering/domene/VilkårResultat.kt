package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ks.sak.common.entitet.BaseEntitet
import no.nav.familie.ks.sak.common.util.StringListConverter
import no.nav.familie.ks.sak.common.util.førsteDagINesteMåned
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.finnTilOgMedDato
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.IBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.IBegrunnelseListConverter
import no.nav.familie.tidslinje.Periode
import org.hibernate.annotations.Immutable
import java.math.BigDecimal
import java.time.LocalDate

@Entity(name = "VilkårResultat")
@Table(name = "vilkar_resultat")
@Immutable
class VilkårResultat(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vilkar_resultat_seq_generator")
    @SequenceGenerator(
        name = "vilkar_resultat_seq_generator",
        sequenceName = "vilkar_resultat_seq",
        allocationSize = 50,
    )
    val id: Long = 0,
    // Denne må være nullable slik at man kan slette vilkår fra person resultat
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "fk_person_resultat_id")
    var personResultat: PersonResultat?,
    @Enumerated(EnumType.STRING)
    @Column(name = "vilkar")
    val vilkårType: Vilkår,
    @Enumerated(EnumType.STRING)
    @Column(name = "resultat")
    var resultat: Resultat,
    @Column(name = "periode_fom")
    var periodeFom: LocalDate? = null,
    @Column(name = "periode_tom")
    var periodeTom: LocalDate? = null,
    @Column(name = "begrunnelse", columnDefinition = "TEXT", nullable = false)
    var begrunnelse: String,
    @Column(name = "fk_behandling_id", nullable = false)
    var behandlingId: Long,
    @Column(name = "er_automatisk_vurdert", nullable = false)
    var erAutomatiskVurdert: Boolean = false,
    @Column(name = "er_eksplisitt_avslag_paa_soknad")
    var erEksplisittAvslagPåSøknad: Boolean? = null,
    @Column(name = "evaluering_aarsak")
    @Convert(converter = StringListConverter::class)
    val evalueringÅrsaker: List<String> = emptyList(),
    @Column(name = "regel_input", columnDefinition = "TEXT")
    var regelInput: String? = null,
    @Column(name = "regel_output", columnDefinition = "TEXT")
    var regelOutput: String? = null,
    @Column(name = "vedtak_begrunnelse_spesifikasjoner")
    @Convert(converter = IBegrunnelseListConverter::class)
    var begrunnelser: List<IBegrunnelse> = emptyList(),
    @Enumerated(EnumType.STRING)
    @Column(name = "vurderes_etter")
    var vurderesEtter: Regelverk? = personResultat?.let { vilkårType.defaultRegelverk(it.vilkårsvurdering.behandling.kategori) },
    @Column(name = "utdypende_vilkarsvurderinger")
    @Convert(converter = UtdypendeVilkårsvurderingerConverter::class)
    var utdypendeVilkårsvurderinger: List<UtdypendeVilkårsvurdering> = emptyList(),
    @Column(name = "antall_timer")
    val antallTimer: BigDecimal? = null,
    @Column(name = "soker_har_meldt_fra_om_barnehageplass")
    var søkerHarMeldtFraOmBarnehageplass: Boolean? = null,
) : BaseEntitet() {
    fun erAvslagUtenPeriode() = erEksplisittAvslagPåSøknad == true && periodeFom == null && periodeTom == null

    fun harFremtidigTom() = periodeTom?.isAfter(LocalDate.now().sisteDagIMåned()) ?: true

    fun erAdopsjonOppfylt() =
        vilkårType == Vilkår.BARNETS_ALDER &&
            utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.ADOPSJON) &&
            resultat == Resultat.OPPFYLT

    fun erOppfylt() = this.resultat == Resultat.OPPFYLT

    fun erIkkeAktuelt() = this.resultat == Resultat.IKKE_AKTUELT

    fun harMeldtBarnehageplassOgErFulltidIBarnehage() = this.søkerHarMeldtFraOmBarnehageplass == true && (this.antallTimer == null || this.antallTimer >= BigDecimal(33))

    fun kopierMedNyPeriodeOgBehandling(
        fom: LocalDate?,
        tom: LocalDate?,
        behandlingId: Long,
    ): VilkårResultat =
        VilkårResultat(
            personResultat = this.personResultat,
            erAutomatiskVurdert = this.erAutomatiskVurdert,
            vilkårType = this.vilkårType,
            resultat = this.resultat,
            periodeFom = fom,
            periodeTom = tom,
            begrunnelse = this.begrunnelse,
            regelInput = this.regelInput,
            regelOutput = this.regelOutput,
            behandlingId = behandlingId,
            erEksplisittAvslagPåSøknad = this.erEksplisittAvslagPåSøknad,
            vurderesEtter = this.vurderesEtter,
            utdypendeVilkårsvurderinger = this.utdypendeVilkårsvurderinger,
            antallTimer = antallTimer,
            søkerHarMeldtFraOmBarnehageplass = søkerHarMeldtFraOmBarnehageplass,
        )

    fun tilPeriode(vilkår: List<VilkårResultat>): Periode<Long>? {
        val fraOgMedDato = this.periodeFom?.førsteDagINesteMåned() ?: return null
        val tilOgMedDato = finnTilOgMedDato(tilOgMed = this.periodeTom, vilkårResultater = vilkår)
        if (fraOgMedDato.toYearMonth().isAfter(tilOgMedDato.toYearMonth())) return null
        return Periode(verdi = this.behandlingId, fom = fraOgMedDato, tom = tilOgMedDato)
    }

    fun kopier(
        personResultat: PersonResultat? = this.personResultat,
        periodeFom: LocalDate? = this.periodeFom,
        resultat: Resultat = this.resultat,
        periodeTom: LocalDate? = this.periodeTom,
        begrunnelse: String = this.begrunnelse,
        utdypendeVilkårsvurderinger: List<UtdypendeVilkårsvurdering> = this.utdypendeVilkårsvurderinger,
    ) = VilkårResultat(
        personResultat = personResultat ?: this.personResultat,
        erAutomatiskVurdert = this.erAutomatiskVurdert,
        vilkårType = this.vilkårType,
        resultat = resultat,
        periodeFom = periodeFom,
        periodeTom = periodeTom,
        begrunnelse = begrunnelse,
        behandlingId = this.behandlingId,
        regelInput = this.regelInput,
        regelOutput = this.regelOutput,
        erEksplisittAvslagPåSøknad = this.erEksplisittAvslagPåSøknad,
        vurderesEtter = this.vurderesEtter,
        utdypendeVilkårsvurderinger = utdypendeVilkårsvurderinger,
        antallTimer = this.antallTimer,
        søkerHarMeldtFraOmBarnehageplass = this.søkerHarMeldtFraOmBarnehageplass,
    )

    override fun toString(): String = """ VilkårResultat(id=$id,vilkårType=$vilkårType,periodeFom=$periodeFom,periodeTom=$periodeTom,resultat=$resultat,evalueringÅrsaker=$evalueringÅrsaker",erAutomatiskVurdert=$erAutomatiskVurdert) """

    companion object {
        val VilkårResultatComparator = compareBy<VilkårResultat>({ it.periodeFom }, { it.resultat }, { it.vilkårType })
    }
}
