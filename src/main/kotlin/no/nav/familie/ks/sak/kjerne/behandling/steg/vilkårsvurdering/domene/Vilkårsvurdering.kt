package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ks.sak.common.entitet.BaseEntitet
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling

@Entity(name = "Vilkårsvurdering")
@Table(name = "vilkaarsvurdering")
data class Vilkårsvurdering(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vilkaarsvurdering_seq_generator")
    @SequenceGenerator(
        name = "vilkaarsvurdering_seq_generator",
        sequenceName = "vilkaarsvurdering_seq",
        allocationSize = 50,
    )
    val id: Long = 0,
    @ManyToOne(optional = false)
    @JoinColumn(name = "fk_behandling_id", nullable = false, updatable = false)
    val behandling: Behandling,
    @Column(name = "aktiv", nullable = false)
    var aktiv: Boolean = true,
    @OneToMany(
        fetch = FetchType.EAGER,
        mappedBy = "vilkårsvurdering",
        cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH],
    )
    var personResultater: Set<PersonResultat> = setOf(),
) : BaseEntitet() {
    fun hentPersonResultaterTilAktør(aktørId: String): List<VilkårResultat> =
        personResultater.find { it.aktør.aktørId == aktørId }?.vilkårResultater?.toList()
            ?: throw Feil("Fant ikke personresultat for $aktørId")

    fun finnOpplysningspliktVilkår(): AnnenVurdering? =
        personResultater
            .single { it.erSøkersResultater() }
            .andreVurderinger
            .singleOrNull { it.type == AnnenVurderingType.OPPLYSNINGSPLIKT }
}
