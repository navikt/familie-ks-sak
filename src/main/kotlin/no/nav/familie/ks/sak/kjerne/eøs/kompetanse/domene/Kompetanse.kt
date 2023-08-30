package no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.util.YearMonthConverter
import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.EøsSkjemaEntitet
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import java.time.YearMonth

@Entity(name = "Kompetanse")
@Table(name = "KOMPETANSE")
data class Kompetanse(
    @Column(name = "fom", columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    override val fom: YearMonth?,

    @Column(name = "tom", columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    override val tom: YearMonth?,

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "AKTOER_TIL_KOMPETANSE",
        joinColumns = [JoinColumn(name = "fk_kompetanse_id")],
        inverseJoinColumns = [JoinColumn(name = "fk_aktoer_id")],
    )
    override val barnAktører: Set<Aktør> = emptySet(),

    @Enumerated(EnumType.STRING)
    @Column(name = "soekers_aktivitet")
    val søkersAktivitet: SøkersAktivitet? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "annen_forelderes_aktivitet")
    val annenForeldersAktivitet: AnnenForeldersAktivitet? = null,

    @Column(name = "annen_forelderes_aktivitetsland")
    val annenForeldersAktivitetsland: String? = null,

    @Column(name = "sokers_aktivitetsland")
    val søkersAktivitetsland: String? = null,

    @Column(name = "barnets_bostedsland")
    val barnetsBostedsland: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "resultat")
    val resultat: KompetanseResultat? = null,
) : EøsSkjemaEntitet<Kompetanse>() {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "kompetanse_seq_generator")
    @SequenceGenerator(
        name = "kompetanse_seq_generator",
        sequenceName = "kompetanse_seq",
        allocationSize = 50,
    )
    override var id: Long = 0

    @Column(name = "fk_behandling_id", updatable = false, nullable = false)
    override var behandlingId: Long = 0

    override fun utenInnhold() = this.copy(
        søkersAktivitet = null,
        søkersAktivitetsland = null,
        annenForeldersAktivitet = null,
        annenForeldersAktivitetsland = null,
        barnetsBostedsland = null,
        resultat = null,
    )

    override fun kopier(fom: YearMonth?, tom: YearMonth?, barnAktører: Set<Aktør>) =
        copy(fom = fom, tom = tom, barnAktører = barnAktører)

    fun validerFelterErSatt() {
        if (!erFelterSatt()) {
            throw Feil("Kompetanse mangler verdier")
        }
    }

    fun erFelterSatt() = søkersAktivitet != null &&
        annenForeldersAktivitet != null &&
        barnetsBostedsland != null &&
        resultat != null &&
        barnAktører.isNotEmpty()

    companion object {
        val blankKompetanse = Kompetanse(fom = null, tom = null, barnAktører = emptySet())
    }
}

enum class SøkersAktivitet {
    ARBEIDER,
    SELVSTENDIG_NÆRINGSDRIVENDE,
    MOTTAR_UTBETALING_SOM_ERSTATTER_LØNN,
    UTSENDT_ARBEIDSTAKER_FRA_NORGE,
    MOTTAR_UFØRETRYGD,
    MOTTAR_PENSJON,
    ARBEIDER_PÅ_NORSKREGISTRERT_SKIP,
    ARBEIDER_PÅ_NORSK_SOKKEL,
    ARBEIDER_FOR_ET_NORSK_FLYSELSKAP,
    ARBEIDER_VED_UTENLANDSK_UTENRIKSSTASJON,
    MOTTAR_UTBETALING_FRA_NAV_UNDER_OPPHOLD_I_UTLANDET,
    MOTTAR_UFØRETRYGD_FRA_NAV_UNDER_OPPHOLD_I_UTLANDET,
    MOTTAR_PENSJON_FRA_NAV_UNDER_OPPHOLD_I_UTLANDET,
    INAKTIV,
}

enum class AnnenForeldersAktivitet {
    I_ARBEID,
    MOTTAR_UTBETALING_SOM_ERSTATTER_LØNN,
    FORSIKRET_I_BOSTEDSLAND,
    MOTTAR_PENSJON,
    INAKTIV,
    IKKE_AKTUELT,
}

enum class KompetanseResultat {
    NORGE_ER_PRIMÆRLAND,
    NORGE_ER_SEKUNDÆRLAND,
    TO_PRIMÆRLAND,
}
