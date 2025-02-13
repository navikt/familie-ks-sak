package no.nav.familie.ks.sak.kjerne.adopsjon

import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.lagPersonResultat
import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.data.lagVilkårsvurdering
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.lagAutomatiskGenererteVilkårForBarnetsAlder
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class AdopsjonValidatorTest {
    val adopsjonValidator = AdopsjonValidator()

    @Test
    fun `Skal kaste feil om det finnes adopsjon i utdypende vilkårsvurdering, men ikke adopsjonsdato for person`() {
        val søker = lagPerson(personType = PersonType.SØKER, aktør = randomAktør())
        val barn = lagPerson(personType = PersonType.BARN, aktør = randomAktør())
        val barn2 = lagPerson(personType = PersonType.BARN, aktør = randomAktør())
        val vilkårsvurdering =
            lagVilkårsvurdering {
                setOf(
                    lagPersonResultat(vilkårsvurdering = it, aktør = søker.aktør),
                    lagPersonResultat(vilkårsvurdering = it, aktør = barn.aktør, lagVilkårResultater = { personResultat -> lagVilkårResultaterForBarn(personResultat = personResultat, erAdopsjon = true, fødselsdato = barn.fødselsdato) }),
                    lagPersonResultat(vilkårsvurdering = it, aktør = barn2.aktør, lagVilkårResultater = { personResultat -> lagVilkårResultaterForBarn(personResultat = personResultat, erAdopsjon = true, fødselsdato = barn2.fødselsdato) }),
                )
            }

        val adopsjoner = listOf(Adopsjon(behandlingId = vilkårsvurdering.behandling.id, aktør = barn2.aktør, adopsjonsdato = barn2.fødselsdato.plusMonths(2)))

        assertThrows<FunksjonellFeil> { adopsjonValidator.validerAdopsjonIUtdypendeVilkårsvurderingOgAdopsjonsdato(vilkårsvurdering = vilkårsvurdering, adopsjonerIBehandling = adopsjoner, støtterAdopsjonILøsningen = true) }
    }

    @Test
    fun `Skal kaste feil om det finnes adopsjonsdato for person, men adopsjon er ikke valgt i utdypende vilkårsvurdering`() {
        val søker = lagPerson(personType = PersonType.SØKER, aktør = randomAktør())
        val barn = lagPerson(personType = PersonType.BARN, aktør = randomAktør())
        val barn2 = lagPerson(personType = PersonType.BARN, aktør = randomAktør())
        val vilkårsvurdering =
            lagVilkårsvurdering {
                setOf(
                    lagPersonResultat(vilkårsvurdering = it, aktør = søker.aktør),
                    lagPersonResultat(vilkårsvurdering = it, aktør = barn.aktør, lagVilkårResultater = { personResultat -> lagVilkårResultaterForBarn(personResultat = personResultat, erAdopsjon = false, fødselsdato = barn.fødselsdato) }),
                    lagPersonResultat(vilkårsvurdering = it, aktør = barn2.aktør, lagVilkårResultater = { personResultat -> lagVilkårResultaterForBarn(personResultat = personResultat, erAdopsjon = false, fødselsdato = barn2.fødselsdato) }),
                )
            }

        val adopsjoner = listOf(Adopsjon(behandlingId = vilkårsvurdering.behandling.id, aktør = barn.aktør, adopsjonsdato = barn.fødselsdato.plusMonths(2)))

        assertThrows<FunksjonellFeil> { adopsjonValidator.validerAdopsjonIUtdypendeVilkårsvurderingOgAdopsjonsdato(vilkårsvurdering = vilkårsvurdering, adopsjonerIBehandling = adopsjoner, støtterAdopsjonILøsningen = true) }
    }

    @Test
    fun `Skal ikke kaste feil om det finnes både adopsjonsdato og adopsjon er valgt i utdypende vilkårsvurdering eller ingen av delene`() {
        val søker = lagPerson(personType = PersonType.SØKER, aktør = randomAktør())
        val barn = lagPerson(personType = PersonType.BARN, aktør = randomAktør())
        val barn2 = lagPerson(personType = PersonType.BARN, aktør = randomAktør())
        val vilkårsvurdering =
            lagVilkårsvurdering {
                setOf(
                    lagPersonResultat(vilkårsvurdering = it, aktør = søker.aktør),
                    lagPersonResultat(vilkårsvurdering = it, aktør = barn.aktør, lagVilkårResultater = { personResultat -> lagVilkårResultaterForBarn(personResultat = personResultat, erAdopsjon = true, fødselsdato = barn.fødselsdato) }),
                    lagPersonResultat(vilkårsvurdering = it, aktør = barn2.aktør, lagVilkårResultater = { personResultat -> lagVilkårResultaterForBarn(personResultat = personResultat, erAdopsjon = false, fødselsdato = barn2.fødselsdato) }),
                )
            }

        val adopsjoner = listOf(Adopsjon(behandlingId = vilkårsvurdering.behandling.id, aktør = barn.aktør, adopsjonsdato = barn.fødselsdato.plusMonths(2)))

        assertDoesNotThrow { adopsjonValidator.validerAdopsjonIUtdypendeVilkårsvurderingOgAdopsjonsdato(vilkårsvurdering = vilkårsvurdering, adopsjonerIBehandling = adopsjoner, støtterAdopsjonILøsningen = true) }
    }

    @Test
    fun `Skal ikke kaste feil hvis toggle er av, selv om det finnes adopsjon i utdypende vilkårsvurdering, men ikke adopsjonsdato for person`() {
        val søker = lagPerson(personType = PersonType.SØKER, aktør = randomAktør())
        val barn = lagPerson(personType = PersonType.BARN, aktør = randomAktør())
        val barn2 = lagPerson(personType = PersonType.BARN, aktør = randomAktør())
        val vilkårsvurdering =
            lagVilkårsvurdering {
                setOf(
                    lagPersonResultat(vilkårsvurdering = it, aktør = søker.aktør),
                    lagPersonResultat(vilkårsvurdering = it, aktør = barn.aktør, lagVilkårResultater = { personResultat -> lagVilkårResultaterForBarn(personResultat = personResultat, erAdopsjon = true, fødselsdato = barn.fødselsdato) }),
                    lagPersonResultat(vilkårsvurdering = it, aktør = barn2.aktør, lagVilkårResultater = { personResultat -> lagVilkårResultaterForBarn(personResultat = personResultat, erAdopsjon = false, fødselsdato = barn2.fødselsdato) }),
                )
            }

        val adopsjoner = listOf(Adopsjon(behandlingId = vilkårsvurdering.behandling.id, aktør = barn2.aktør, adopsjonsdato = barn2.fødselsdato.plusMonths(2)))

        assertDoesNotThrow { adopsjonValidator.validerAdopsjonIUtdypendeVilkårsvurderingOgAdopsjonsdato(vilkårsvurdering = vilkårsvurdering, adopsjonerIBehandling = adopsjoner, støtterAdopsjonILøsningen = false) }
    }

    private fun lagVilkårResultaterForBarn(
        personResultat: PersonResultat,
        erAdopsjon: Boolean,
        fødselsdato: LocalDate,
    ): Set<VilkårResultat> =
        Vilkår
            .hentVilkårFor(PersonType.BARN)
            .flatMap { vilkår ->
                if (vilkår == Vilkår.BARNETS_ALDER) {
                    lagAutomatiskGenererteVilkårForBarnetsAlder(personResultat = personResultat, behandlingId = personResultat.vilkårsvurdering.behandling.id, fødselsdato = fødselsdato, erAdopsjon = erAdopsjon)
                } else {
                    listOf(
                        lagVilkårResultat(
                            behandlingId = personResultat.vilkårsvurdering.behandling.id,
                            personResultat = personResultat,
                            vilkårType = vilkår,
                            resultat = Resultat.OPPFYLT,
                            periodeFom = LocalDate.now().minusMonths(1),
                            periodeTom = LocalDate.now().plusYears(2),
                            begrunnelse = "",
                        ),
                    )
                }
            }.toSet()
}
