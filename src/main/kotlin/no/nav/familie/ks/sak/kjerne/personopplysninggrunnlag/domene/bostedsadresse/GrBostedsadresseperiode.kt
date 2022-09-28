package no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.bostedsadresse

import no.nav.familie.ks.sak.common.entitet.BaseEntitet
import no.nav.familie.ks.sak.common.entitet.DatoIntervallEntitet
import javax.persistence.Embedded
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.SequenceGenerator
import javax.persistence.Table

/**
 * Ble brukt i tidlig fase av automatisk vurdering av fødselshendelser, men brukes ikke lenger.
 * Tar vare på i tilfelle vi må hente opp dataene igjen.
 */
@Entity(name = "GrBostedsadresseperiode")
@Table(name = "PO_BOSTEDSADRESSEPERIODE")
data class GrBostedsadresseperiode(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "po_bostedsadresseperiode_seq_generator")
    @SequenceGenerator(
        name = "po_bostedsadresseperiode_seq_generator",
        sequenceName = "po_bostedsadresseperiode_seq",
        allocationSize = 50
    )
    val id: Long = 0,

    @Embedded
    val periode: DatoIntervallEntitet? = null
) : BaseEntitet()
