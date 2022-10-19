package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ks.sak.common.entitet.BaseEntitet
import no.nav.familie.ks.sak.common.util.Periode
import no.nav.familie.ks.sak.common.util.StringListConverter
import no.nav.familie.ks.sak.common.util.førsteDagINesteMåned
import no.nav.familie.ks.sak.common.util.sisteDagIMåned

import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.finnTilOgMedDato

import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.Standardbegrunnelse
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.StandardbegrunnelseListConverter

import org.hibernate.annotations.Immutable
import java.math.BigDecimal
import java.time.LocalDate
import javax.persistence.Column
import javax.persistence.Convert
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@Entity(name = "VilkårResultat")
@Table(name = "vilkar_resultat")
@Immutable
class VilkårResultat(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vilkar_resultat_seq_generator")
    @SequenceGenerator(
        name = "vilkar_resultat_seq_generator",
        sequenceName = "vilkar_resultat_seq",
        allocationSize = 50
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
    @Convert(converter = StandardbegrunnelseListConverter::class)
    var standardbegrunnelser: List<Standardbegrunnelse> = emptyList(),

    @Enumerated(EnumType.STRING)
    @Column(name = "vurderes_etter")
    var vurderesEtter: Regelverk? = personResultat?.let { vilkårType.defaultRegelverk(it.vilkårsvurdering.behandling.kategori) },

    @Column(name = "utdypende_vilkarsvurderinger")
    @Convert(converter = UtdypendeVilkårsvurderingerConverter::class)
    var utdypendeVilkårsvurderinger: List<UtdypendeVilkårsvurdering> = emptyList(),

    @Column(name = "antall_timer")
    val antallTimer: BigDecimal? = null
) : BaseEntitet() {

    fun erAvslagUtenPeriode() = erEksplisittAvslagPåSøknad == true && periodeFom == null && periodeTom == null
    fun harFremtidigTom() = periodeTom?.isAfter(LocalDate.now().sisteDagIMåned()) ?: true

    fun kopierMedNyPeriodeOgBehandling(fom: LocalDate?, tom: LocalDate?, behandlingId: Long): VilkårResultat {
        return VilkårResultat(
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
            antallTimer = antallTimer
        )
    }

    fun tilPeriode(vilkår: List<VilkårResultat>): Periode? {
        val fraOgMedDato = this.periodeFom?.førsteDagINesteMåned() ?: return null
        val tilOgMedDato = finnTilOgMedDato(tilOgMed = this.periodeTom, vilkårResultater = vilkår)
        if (fraOgMedDato.toYearMonth().isAfter(tilOgMedDato.toYearMonth())) return null
        return Periode(fom = fraOgMedDato, tom = tilOgMedDato)
    }

    companion object {
        val VilkårResultatComparator = compareBy<VilkårResultat>({ it.periodeFom }, { it.resultat }, { it.vilkårType })
    }
}
