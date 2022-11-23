package no.nav.familie.ks.sak.kjerne.brev.begrunnelser

import io.mockk.mockk
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelseType
import no.nav.familie.ks.sak.integrasjon.sanity.domene.Trigger
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.Standardbegrunnelse
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tilFørskjøvetVilkårResultatTidslinjeMap
import java.math.BigDecimal
import java.time.LocalDate

class FinnGyldigeBegrunnelserForPeriodeContextTest {

    private val sanityBegrunnelser = lagSanityBegrunnelser()
    private val barnAktør = randomAktør()
    private val søkerAktør = randomAktør()
    private val persongrunnlag = lagPersonopplysningGrunnlag(
        barnasIdenter = listOf(barnAktør.aktivFødselsnummer()),
        barnAktør = listOf(barnAktør),
        søkerPersonIdent = søkerAktør.aktivFødselsnummer(),
        søkerAktør = søkerAktør
    )
    private val vilkårOppfyltFom = LocalDate.now().minusMonths(8)
    private val vilkårOppfyltTom = LocalDate.now()

    @Test
    fun `hentGyldigeBegrunnelserForVedtaksperiode - skal returnere standardbegrunnelsen INNVILGET_IKKE_BARNEHAGE når antall timer barnehage er 0 eller mer enn 33 og barnet ikke er adoptert`() {
        val personResultatBarn = PersonResultat(
            aktør = barnAktør,
            vilkårsvurdering = mockk(),
            vilkårResultater = lagVilkårResultaterForVilkårTyper(
                vilkårTyper = Vilkår.hentVilkårFor(PersonType.BARN),
                fom = vilkårOppfyltFom,
                tom = vilkårOppfyltTom
            )
        )
        val personResultatSøker = PersonResultat(
            aktør = søkerAktør,
            vilkårsvurdering = mockk(),
            vilkårResultater = lagVilkårResultaterForVilkårTyper(
                vilkårTyper = Vilkår.hentVilkårFor(PersonType.SØKER),
                fom = vilkårOppfyltFom,
                tom = vilkårOppfyltTom
            )
        )
        val personResultater = listOf(
            personResultatBarn,
            personResultatSøker
        )

        val standardbegrunnelser =
            lagFinnGyldigeBegrunnelserForPeriodeContext(personResultater).hentGyldigeBegrunnelserForVedtaksperiode()

        assertEquals(1, standardbegrunnelser.size)
        assertEquals(Standardbegrunnelse.INNVILGET_IKKE_BARNEHAGE, standardbegrunnelser.first())
    }

    @Test
    fun `hentGyldigeBegrunnelserForVedtaksperiode - skal returnere standardbegrunnelsen INNVILGET_IKKE_BARNEHAGE_ADOPSJON når antall timer barnehage er 0 eller mer enn 33 og barnet er adoptert`() {
        val personResultatBarn = PersonResultat(
            aktør = barnAktør,
            vilkårsvurdering = mockk(),
            vilkårResultater = mutableSetOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNETS_ALDER,
                    periodeFom = vilkårOppfyltFom,
                    periodeTom = vilkårOppfyltTom,
                    utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.ADOPSJON)
                )
            )
        )
        personResultatBarn.vilkårResultater.addAll(
            lagVilkårResultaterForVilkårTyper(
                setOf(
                    Vilkår.BARNEHAGEPLASS,
                    Vilkår.BOR_MED_SØKER,
                    Vilkår.MEDLEMSKAP_ANNEN_FORELDER,
                    Vilkår.BOSATT_I_RIKET
                ),
                fom = vilkårOppfyltFom,
                tom = vilkårOppfyltTom
            )
        )
        val personResultatSøker = PersonResultat(
            aktør = søkerAktør,
            vilkårsvurdering = mockk(),
            vilkårResultater = lagVilkårResultaterForVilkårTyper(
                vilkårTyper = Vilkår.hentVilkårFor(PersonType.SØKER),
                fom = vilkårOppfyltFom,
                tom = vilkårOppfyltTom
            )
        )
        val personResultater = listOf(
            personResultatBarn,
            personResultatSøker
        )

        val standardbegrunnelser =
            lagFinnGyldigeBegrunnelserForPeriodeContext(personResultater).hentGyldigeBegrunnelserForVedtaksperiode()

        assertEquals(1, standardbegrunnelser.size)
        assertEquals(Standardbegrunnelse.INNVILGET_IKKE_BARNEHAGE_ADOPSJON, standardbegrunnelser.first())
    }

    @Test
    fun `hentGyldigeBegrunnelserForVedtaksperiode - skal returnere standardbegrunnelsen INNVILGET_DELTID_BARNEHAGE når antall timer barnehage er større enn 0 og mindre enn 33 og barnet ikke er adoptert`() {
        val personResultatBarn = PersonResultat(
            aktør = barnAktør,
            vilkårsvurdering = mockk(),
            vilkårResultater = mutableSetOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = vilkårOppfyltFom,
                    periodeTom = vilkårOppfyltTom,
                    antallTimer = BigDecimal.valueOf(8)
                )
            )
        )
        personResultatBarn.vilkårResultater.addAll(
            lagVilkårResultaterForVilkårTyper(
                setOf(
                    Vilkår.BARNETS_ALDER,
                    Vilkår.BOR_MED_SØKER,
                    Vilkår.MEDLEMSKAP_ANNEN_FORELDER,
                    Vilkår.BOSATT_I_RIKET
                ),
                fom = vilkårOppfyltFom,
                tom = vilkårOppfyltTom
            )
        )
        val personResultatSøker = PersonResultat(
            aktør = søkerAktør,
            vilkårsvurdering = mockk(),
            vilkårResultater = lagVilkårResultaterForVilkårTyper(
                vilkårTyper = Vilkår.hentVilkårFor(PersonType.SØKER),
                fom = vilkårOppfyltFom,
                tom = vilkårOppfyltTom
            )
        )
        val personResultater = listOf(
            personResultatBarn,
            personResultatSøker
        )

        val standardbegrunnelser =
            lagFinnGyldigeBegrunnelserForPeriodeContext(personResultater).hentGyldigeBegrunnelserForVedtaksperiode()

        assertEquals(1, standardbegrunnelser.size)
        assertEquals(Standardbegrunnelse.INNVILGET_DELTID_BARNEHAGE, standardbegrunnelser.first())
    }

    @Test
    fun `hentGyldigeBegrunnelserForVedtaksperiode - skal returnere standardbegrunnelsen INNVILGET_DELTID_BARNEHAGE når antall timer barnehage er større enn 0 og mindre enn 33 og barnet er adoptert`() {
        val personResultatBarn = PersonResultat(
            aktør = barnAktør,
            vilkårsvurdering = mockk(),
            vilkårResultater = mutableSetOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    periodeFom = vilkårOppfyltFom,
                    periodeTom = vilkårOppfyltTom,
                    antallTimer = BigDecimal.valueOf(8)
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.BARNETS_ALDER,
                    periodeFom = vilkårOppfyltFom,
                    periodeTom = vilkårOppfyltTom,
                    utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.ADOPSJON)
                )
            )
        )
        personResultatBarn.vilkårResultater.addAll(
            lagVilkårResultaterForVilkårTyper(
                setOf(
                    Vilkår.BOR_MED_SØKER,
                    Vilkår.MEDLEMSKAP_ANNEN_FORELDER,
                    Vilkår.BOSATT_I_RIKET
                ),
                fom = vilkårOppfyltFom,
                tom = vilkårOppfyltTom
            )
        )
        val personResultatSøker = PersonResultat(
            aktør = søkerAktør,
            vilkårsvurdering = mockk(),
            vilkårResultater = lagVilkårResultaterForVilkårTyper(
                vilkårTyper = Vilkår.hentVilkårFor(PersonType.SØKER),
                fom = vilkårOppfyltFom,
                tom = vilkårOppfyltTom
            )
        )
        val personResultater = listOf(
            personResultatBarn,
            personResultatSøker
        )

        val standardbegrunnelser =
            lagFinnGyldigeBegrunnelserForPeriodeContext(personResultater).hentGyldigeBegrunnelserForVedtaksperiode()

        assertEquals(1, standardbegrunnelser.size)
        assertEquals(Standardbegrunnelse.INNVILGET_DELTID_BARNEHAGE_ADOPSJON, standardbegrunnelser.first())
    }

    private fun lagSanityBegrunnelser(): List<SanityBegrunnelse> = listOf(
        SanityBegrunnelse(
            apiNavn = Standardbegrunnelse.INNVILGET_IKKE_BARNEHAGE.sanityApiNavn,
            navnISystem = "Ikke barnehage",
            type = SanityBegrunnelseType.STANDARD,
            vilkår = listOf(Vilkår.BARNEHAGEPLASS, Vilkår.BARNETS_ALDER),
            rolle = emptyList(),
            triggere = emptyList(),
            utdypendeVilkårsvurderinger = emptyList(),
            hjemler = emptyList()
        ),
        SanityBegrunnelse(
            apiNavn = Standardbegrunnelse.INNVILGET_IKKE_BARNEHAGE_ADOPSJON.sanityApiNavn,
            navnISystem = "Ikke barnehage - adopsjon",
            type = SanityBegrunnelseType.STANDARD,
            vilkår = listOf(Vilkår.BARNEHAGEPLASS, Vilkår.BARNETS_ALDER),
            rolle = emptyList(),
            triggere = emptyList(),
            utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.ADOPSJON),
            hjemler = emptyList()
        ),
        SanityBegrunnelse(
            apiNavn = Standardbegrunnelse.INNVILGET_DELTID_BARNEHAGE.sanityApiNavn,
            navnISystem = "Deltid barnehage",
            type = SanityBegrunnelseType.STANDARD,
            vilkår = listOf(Vilkår.BARNEHAGEPLASS, Vilkår.BARNETS_ALDER),
            rolle = emptyList(),
            triggere = listOf(Trigger.DELTID),
            utdypendeVilkårsvurderinger = emptyList(),
            hjemler = emptyList()
        ),
        SanityBegrunnelse(
            apiNavn = Standardbegrunnelse.INNVILGET_DELTID_BARNEHAGE_ADOPSJON.sanityApiNavn,
            navnISystem = "Deltid barnehage - adopsjon",
            type = SanityBegrunnelseType.STANDARD,
            vilkår = listOf(Vilkår.BARNEHAGEPLASS, Vilkår.BARNETS_ALDER),
            rolle = emptyList(),
            triggere = listOf(Trigger.DELTID),
            utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.ADOPSJON),
            hjemler = emptyList()
        )
    )

    private fun lagVilkårResultaterForVilkårTyper(
        vilkårTyper: Set<Vilkår>,
        fom: LocalDate,
        tom: LocalDate
    ): MutableSet<VilkårResultat> =
        vilkårTyper.map { lagVilkårResultat(vilkårType = it, periodeFom = fom, periodeTom = tom) }.toMutableSet()

    private fun lagFinnGyldigeBegrunnelserForPeriodeContext(personResultater: List<PersonResultat>): FinnGyldigeBegrunnelserForPeriodeContext {
        // Må forskyve personresultatene for å finne riktig dato for vedtaksperiode.
        val vedtaksperiodeStartsTidpunkt =
            personResultater.tilFørskjøvetVilkårResultatTidslinjeMap(persongrunnlag).values.first().startsTidspunkt

        val utvidetVedtaksperiodeMedBegrunnelser = UtvidetVedtaksperiodeMedBegrunnelser(
            id = 0,
            fom = vedtaksperiodeStartsTidpunkt,
            tom = vedtaksperiodeStartsTidpunkt.plusDays(1),
            type = Vedtaksperiodetype.UTBETALING,
            begrunnelser = emptyList()
        )

        return FinnGyldigeBegrunnelserForPeriodeContext(
            utvidetVedtaksperiodeMedBegrunnelser,
            sanityBegrunnelser,
            persongrunnlag,
            personResultater,
            listOf(barnAktør.aktørId, søkerAktør.aktørId)
        )
    }
}
