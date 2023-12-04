package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering

import io.mockk.mockk
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.data.lagVilkårsvurderingOppfylt
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.AnnenVurderingType
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import org.hamcrest.CoreMatchers.`is` as Is

class OppdaterVilkårsvurderingTest {
    @Test
    fun `kopierOverOppfylteOgIkkeAktuelleResultaterFraForrigeBehandling skal legge til nytt vilkår`() {
        val søkerPersonIdent = randomFnr()
        val barnPersonIdent = randomFnr()
        val persongrunnlag =
            lagPersonopplysningGrunnlag(
                søkerPersonIdent = søkerPersonIdent,
                barnasIdenter = listOf(barnPersonIdent),
            )
        val vilkårsvurderingForrigeBehandling =
            lagVilkårsvurderingOppfylt(personer = listOf(persongrunnlag.søker, persongrunnlag.barna.single()))
        val initiellVilkårsvurdering =
            genererInitiellVilkårsvurdering(behandling = mockk(relaxed = true), personopplysningGrunnlag = persongrunnlag)

        initiellVilkårsvurdering.kopierOverOppfylteOgIkkeAktuelleResultaterFraForrigeBehandling(
            vilkårsvurderingForrigeBehandling = vilkårsvurderingForrigeBehandling,
        )

        val søkerVilkårResultater =
            initiellVilkårsvurdering.personResultater.single { it.aktør.aktivFødselsnummer() == søkerPersonIdent }.vilkårResultater

        val barnVilkårResultater =
            initiellVilkårsvurdering.personResultater.single { it.aktør.aktivFødselsnummer() == barnPersonIdent }.vilkårResultater

        Assertions.assertEquals(2, søkerVilkårResultater.size)
        Assertions.assertEquals(5, barnVilkårResultater.size)

        Vilkår.hentVilkårFor(persongrunnlag.søker.type).forEach { vilkår ->
            Assertions.assertEquals(
                Resultat.OPPFYLT,
                søkerVilkårResultater.find { it.vilkårType == vilkår }?.resultat,
            )
        }

        Vilkår.hentVilkårFor(persongrunnlag.barna.single().type).forEach { vilkår ->
            Assertions.assertEquals(
                Resultat.OPPFYLT,
                barnVilkårResultater.find { it.vilkårType == vilkår }?.resultat,
            )
        }
    }

    @Test
    fun `kopierOverOppfylteOgIkkeAktuelleResultaterFraForrigeBehandling skal legge til person på vilkårsvurdering`() {
        val søkerPersonIdent = randomFnr()
        val persongrunnlag1 =
            lagPersonopplysningGrunnlag(
                søkerPersonIdent = søkerPersonIdent,
            )
        val vilkårsvurderingForrigeBehandling = lagVilkårsvurderingOppfylt(personer = listOf(persongrunnlag1.søker))

        val barnPersonIdent = randomFnr()
        val persongrunnlag2 =
            lagPersonopplysningGrunnlag(
                søkerPersonIdent = søkerPersonIdent,
                barnasIdenter = listOf(barnPersonIdent),
            )
        val initiellVilkårsvurdering =
            genererInitiellVilkårsvurdering(behandling = mockk(relaxed = true), personopplysningGrunnlag = persongrunnlag2)

        initiellVilkårsvurdering.kopierOverOppfylteOgIkkeAktuelleResultaterFraForrigeBehandling(
            vilkårsvurderingForrigeBehandling = vilkårsvurderingForrigeBehandling,
        )

        val søkerVilkårResultater =
            initiellVilkårsvurdering.personResultater.single { it.aktør.aktivFødselsnummer() == søkerPersonIdent }.vilkårResultater

        val barnVilkårResultater =
            initiellVilkårsvurdering.personResultater.single { it.aktør.aktivFødselsnummer() == barnPersonIdent }.vilkårResultater

        Assertions.assertEquals(2, initiellVilkårsvurdering.personResultater.size)

        Vilkår.hentVilkårFor(persongrunnlag2.søker.type).forEach { vilkår ->
            Assertions.assertEquals(
                Resultat.OPPFYLT,
                søkerVilkårResultater.find { it.vilkårType == vilkår }?.resultat,
            )
        }

        val initiellVilkårsvurderingUendret =
            genererInitiellVilkårsvurdering(behandling = mockk(relaxed = true), personopplysningGrunnlag = persongrunnlag2)
        val barnVilkårResultaterUendret =
            initiellVilkårsvurderingUendret.personResultater.single { it.aktør.aktivFødselsnummer() == barnPersonIdent }.vilkårResultater

        Vilkår.hentVilkårFor(persongrunnlag2.barna.single().type).forEach { vilkår ->
            Assertions.assertEquals(
                barnVilkårResultaterUendret.find { it.vilkårType == vilkår }?.resultat,
                barnVilkårResultater.find { it.vilkårType == vilkår }?.resultat,
            )
        }
    }

    @Test
    fun `kopierOverOppfylteOgIkkeAktuelleResultaterFraForrigeBehandling skal fjerne person på vilkårsvurdering`() {
        val persongrunnlagRevurdering =
            lagPersonopplysningGrunnlag(
                søkerPersonIdent = randomFnr(),
            )
        val initiellVilkårsvurdering =
            genererInitiellVilkårsvurdering(
                behandling = mockk(relaxed = true),
                personopplysningGrunnlag = persongrunnlagRevurdering,
            )

        val persongrunnlagForrigeBehandling =
            lagPersonopplysningGrunnlag(
                søkerPersonIdent = randomFnr(),
                barnasIdenter = listOf(randomFnr()),
            )
        val vilkårsvurderingForrigeBehandling =
            lagVilkårsvurderingOppfylt(
                personer =
                listOf(
                    persongrunnlagForrigeBehandling.søker,
                    persongrunnlagForrigeBehandling.barna.single(),
                ),
            )

        initiellVilkårsvurdering.kopierOverOppfylteOgIkkeAktuelleResultaterFraForrigeBehandling(
            vilkårsvurderingForrigeBehandling = vilkårsvurderingForrigeBehandling,
        )
        Assertions.assertEquals(1, initiellVilkårsvurdering.personResultater.size)
    }

    @Test
    fun `kopierOverOppfylteOgIkkeAktuelleResultaterFraForrigeBehandling skal ha med tomt vilkår på person hvis vilkåret ble avslått forrige behandling`() {
        val søkerFnr = randomFnr()
        val nyBehandling = lagBehandling()
        val forrigeBehandling = lagBehandling()

        val persongrunnlag =
            lagPersonopplysningGrunnlag(
                behandlingId = nyBehandling.id,
                søkerPersonIdent = søkerFnr,
            )
        val initiellVilkårsvurdering =
            genererInitiellVilkårsvurdering(
                behandling = nyBehandling,
                personopplysningGrunnlag = persongrunnlag,
            )
        val vilkårsvurderingForrigeBehandling = Vilkårsvurdering(behandling = forrigeBehandling)
        val personResultat =
            PersonResultat(
                vilkårsvurdering = vilkårsvurderingForrigeBehandling,
                aktør = persongrunnlag.søker.aktør,
            )
        val bosattIRiketVilkårResultater =
            setOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    personResultat = personResultat,
                    resultat = Resultat.IKKE_OPPFYLT,
                    periodeFom = LocalDate.now().minusYears(2),
                    periodeTom = LocalDate.now().minusYears(1),
                ),
            )
        personResultat.setSortedVilkårResultater(bosattIRiketVilkårResultater)
        vilkårsvurderingForrigeBehandling.personResultater = setOf(personResultat)

        initiellVilkårsvurdering.kopierOverOppfylteOgIkkeAktuelleResultaterFraForrigeBehandling(
            vilkårsvurderingForrigeBehandling = vilkårsvurderingForrigeBehandling,
        )

        val nyInitBosattIRiketVilkår =
            initiellVilkårsvurdering.personResultater.find { it.aktør.aktivFødselsnummer() == søkerFnr }
                ?.vilkårResultater?.filter { it.vilkårType == Vilkår.BOSATT_I_RIKET }
                ?: emptyList()

        Assertions.assertTrue(nyInitBosattIRiketVilkår.isNotEmpty())
        Assertions.assertTrue(nyInitBosattIRiketVilkår.single().resultat == Resultat.IKKE_VURDERT)
    }

    // kopierOverOppfylteOgIkkeAktuelleResultaterFraForrigeBehandling
    @Test
    fun `kopierOverOppfylteOgIkkeAktuelleResultaterFraForrigeBehandling skal kun ta med oppfylte og ikke aktuelle perioder`() {
        val søkerAktørId = randomAktør()
        val nyBehandling = lagBehandling()
        val forrigeBehandling = lagBehandling()

        val initiellVilkårsvurdering =
            lagVilkårsvurderingOppfylt(
                behandling = nyBehandling,
                personer =
                listOf(
                    lagPerson(personType = PersonType.SØKER, aktør = søkerAktørId),
                    lagPerson(personType = PersonType.BARN, aktør = randomAktør()),
                ),
            )
        val aktivMedBosattIRiketDelvisIkkeOppfylt = Vilkårsvurdering(behandling = forrigeBehandling)
        val personResultat =
            PersonResultat(
                vilkårsvurdering = aktivMedBosattIRiketDelvisIkkeOppfylt,
                aktør = søkerAktørId,
            )
        val bosattIRiketVilkårResultater =
            setOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    personResultat = personResultat,
                    resultat = Resultat.IKKE_OPPFYLT,
                    periodeFom = LocalDate.now().minusYears(2),
                    periodeTom = LocalDate.now().minusYears(1),
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    personResultat = personResultat,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.now(),
                    periodeTom = LocalDate.now().plusYears(1),
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    personResultat = personResultat,
                    resultat = Resultat.IKKE_AKTUELT,
                    periodeFom = LocalDate.now().plusYears(2),
                    periodeTom = LocalDate.now().plusYears(3),
                ),
            )
        personResultat.setSortedVilkårResultater(bosattIRiketVilkårResultater)
        aktivMedBosattIRiketDelvisIkkeOppfylt.personResultater = setOf(personResultat)

        initiellVilkårsvurdering.kopierOverOppfylteOgIkkeAktuelleResultaterFraForrigeBehandling(
            vilkårsvurderingForrigeBehandling = aktivMedBosattIRiketDelvisIkkeOppfylt,
        )

        val nyInitBosattIRiketVilkår =
            initiellVilkårsvurdering.personResultater.find {
                it.aktør == søkerAktørId
            }?.vilkårResultater?.filter { it.vilkårType == Vilkår.BOSATT_I_RIKET }
                ?: emptyList()

        Assertions.assertTrue(nyInitBosattIRiketVilkår.isNotEmpty())
        Assertions.assertTrue(nyInitBosattIRiketVilkår.all { it.resultat == Resultat.OPPFYLT || it.resultat == Resultat.IKKE_AKTUELT })
    }

    @Test
    fun `kopierOverOppfylteOgIkkeAktuelleResultaterFraForrigeBehandling skal beholde andreVurderinger lagt til på inneværende behandling`() {
        val søkerAktørId = randomAktør()
        val nyBehandling = lagBehandling()

        val initiellVilkårsvurderingUtenAndreVurderinger =
            lagVilkårsvurderingOppfylt(
                behandling = nyBehandling,
                personer =
                listOf(
                    lagPerson(personType = PersonType.SØKER, aktør = søkerAktørId),
                    lagPerson(personType = PersonType.BARN, aktør = randomAktør()),
                ),
            )
        val vilkårsvurderingForrigeBehandling = initiellVilkårsvurderingUtenAndreVurderinger.copy()
        vilkårsvurderingForrigeBehandling.personResultater.find { it.erSøkersResultater() }!!
            .leggTilBlankAnnenVurdering(AnnenVurderingType.OPPLYSNINGSPLIKT)

        initiellVilkårsvurderingUtenAndreVurderinger.kopierOverOppfylteOgIkkeAktuelleResultaterFraForrigeBehandling(
            vilkårsvurderingForrigeBehandling = vilkårsvurderingForrigeBehandling,
        )

        val nyInitInnholderOpplysningspliktVilkår =
            initiellVilkårsvurderingUtenAndreVurderinger.personResultater.find { it.erSøkersResultater() }!!.andreVurderinger
                .any { it.type == AnnenVurderingType.OPPLYSNINGSPLIKT }

        Assertions.assertTrue(nyInitInnholderOpplysningspliktVilkår)
    }

    @Test
    fun `genererInitiellVilkårsvurdering skal generere eøs spesifikke vilkår dersom det er en behandling med kategori EØS`() {
        val søkerFnr = randomFnr()
        val nyBehandling = lagBehandling(kategori = BehandlingKategori.EØS)

        val persongrunnlag =
            lagPersonopplysningGrunnlag(
                behandlingId = nyBehandling.id,
                søkerPersonIdent = søkerFnr,
            )
        val initiellVilkårsvurdering =
            genererInitiellVilkårsvurdering(
                behandling = nyBehandling,
                personopplysningGrunnlag = persongrunnlag,
            )

        val finnesEøsSpesifikkeVilkårIVilkårsvurdering = initiellVilkårsvurdering.personResultater.flatMap { it.vilkårResultater }.any { it.vilkårType.eøsSpesifikt }

        assertThat(finnesEøsSpesifikkeVilkårIVilkårsvurdering, Is(true))
    }

    @Test
    fun `genererInitiellVilkårsvurdering skal generere ikke eøs spesifikke vilkår dersom det er en behandling med kategori NASJONAL`() {
        val søkerFnr = randomFnr()
        val nyBehandling = lagBehandling(kategori = BehandlingKategori.NASJONAL)

        val persongrunnlag =
            lagPersonopplysningGrunnlag(
                behandlingId = nyBehandling.id,
                søkerPersonIdent = søkerFnr,
            )
        val initiellVilkårsvurdering =
            genererInitiellVilkårsvurdering(
                behandling = nyBehandling,
                personopplysningGrunnlag = persongrunnlag,
            )

        val finnesEøsSpesifikkeVilkårIVilkårsvurdering = initiellVilkårsvurdering.personResultater.flatMap { it.vilkårResultater }.any { it.vilkårType.eøsSpesifikt }

        assertThat(finnesEøsSpesifikkeVilkårIVilkårsvurdering, Is(false))
    }
}
