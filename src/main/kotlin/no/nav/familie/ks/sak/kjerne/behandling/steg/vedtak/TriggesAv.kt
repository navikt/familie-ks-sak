package no.nav.familie.ks.sak.kjerne.vedtak

import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType

data class TriggesAv(
    val vilkår: Set<Vilkår>,
    val personTyper: Set<PersonType>
)
