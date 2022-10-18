package no.nav.familie.ks.sak.kjerne.vedtak

import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår

data class TriggesAv(
    val vilkår: Set<Vilkår>,
    val personTyper: Set<PersonType>
)
