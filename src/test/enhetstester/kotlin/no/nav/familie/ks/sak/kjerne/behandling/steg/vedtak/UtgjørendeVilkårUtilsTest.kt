package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak

import no.nav.familie.ks.sak.common.util.Periode
import no.nav.familie.ks.sak.data.fnrTilAktør
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.lagTriggesAv
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.brev.domene.tilBrevPerson
import no.nav.familie.ks.sak.kjerne.brev.domene.tilBrevPersonResultat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class UtgjørendeVilkårUtilsTest {

    @Test
    fun `hentPersonerForAlleUtgjørendeVilkår skal hente riktige personer fra vilkårsvurderingen basert på innvilgelsesbegrunnelse`() {
        val søkerFnr = randomFnr()
        val barn1Fnr = randomFnr()
        val barn2Fnr = randomFnr()

        val søkerAktørId = fnrTilAktør(søkerFnr)
        val barn1AktørId = fnrTilAktør(barn1Fnr)

        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val personopplysningGrunnlag =
            lagPersonopplysningGrunnlag(behandling.id, søkerFnr, listOf(barn1Fnr, barn2Fnr))

        val vilkårsvurdering = Vilkårsvurdering(
            behandling = behandling
        )

        val søkerPersonResultat =
            PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = søkerAktørId)
        søkerPersonResultat.setSortedVilkårResultater(
            setOf(
                VilkårResultat(
                    personResultat = søkerPersonResultat,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2009, 12, 24),
                    periodeTom = LocalDate.of(2010, 6, 1),
                    begrunnelse = "",
                    behandlingId = vilkårsvurdering.behandling.id
                ),
                VilkårResultat(
                    personResultat = søkerPersonResultat,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2008, 12, 24),
                    periodeTom = LocalDate.of(2010, 6, 1),
                    begrunnelse = "",
                    behandlingId = vilkårsvurdering.behandling.id
                )
            )
        )

        val barn1PersonResultat =
            PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn1AktørId)

        barn1PersonResultat.setSortedVilkårResultater(
            setOf(
                VilkårResultat(
                    personResultat = barn1PersonResultat,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2009, 12, 24),
                    periodeTom = LocalDate.of(2010, 6, 1),
                    begrunnelse = "",
                    behandlingId = vilkårsvurdering.behandling.id
                ),
                VilkårResultat(
                    personResultat = barn1PersonResultat,
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2009, 11, 24),
                    periodeTom = LocalDate.of(2010, 6, 1),
                    begrunnelse = "",
                    behandlingId = vilkårsvurdering.behandling.id
                ),
                VilkårResultat(
                    personResultat = søkerPersonResultat,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2009, 12, 24),
                    periodeTom = LocalDate.of(2010, 6, 1),
                    begrunnelse = "",
                    behandlingId = vilkårsvurdering.behandling.id
                )
            )
        )

        val barn2PersonResultat =
            PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn1AktørId)

        barn2PersonResultat.setSortedVilkårResultater(
            setOf(
                VilkårResultat(
                    personResultat = barn1PersonResultat,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2010, 2, 24),
                    periodeTom = LocalDate.of(2010, 6, 1),
                    begrunnelse = "",
                    behandlingId = vilkårsvurdering.behandling.id
                ),
                VilkårResultat(
                    personResultat = barn1PersonResultat,
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2009, 11, 24),
                    periodeTom = LocalDate.of(2010, 6, 1),
                    begrunnelse = "",
                    behandlingId = vilkårsvurdering.behandling.id
                )
            )
        )

        vilkårsvurdering.personResultater = setOf(søkerPersonResultat, barn1PersonResultat, barn2PersonResultat)

        val personerMedUtgjørendeVilkårBorMedSøker = hentPersonerForAlleUtgjørendeVilkår(
            brevPersonResultater = vilkårsvurdering.personResultater.map { it.tilBrevPersonResultat() },
            vedtaksperiode = Periode(
                fom = LocalDate.of(2010, 1, 1),
                tom = LocalDate.of(2010, 6, 1)
            ),
            oppdatertBegrunnelseType = VedtakBegrunnelseType.INNVILGET,
            triggesAv = lagTriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER)),
            aktuellePersonerForVedtaksperiode = personopplysningGrunnlag.personer.toList()
                .map { it.tilBrevPerson() },
            erFørsteVedtaksperiodePåFagsak = false

        )

        assertEquals(2, personerMedUtgjørendeVilkårBorMedSøker.size)
        assertEquals(
            listOf(søkerFnr, barn1Fnr).sorted(),
            personerMedUtgjørendeVilkårBorMedSøker.map { it.aktivPersonIdent }.sorted()
        )

        val personerMedUtgjørendeVilkårBosattIRiket = hentPersonerForAlleUtgjørendeVilkår(
            brevPersonResultater = vilkårsvurdering.personResultater.map { it.tilBrevPersonResultat() },
            vedtaksperiode = Periode(
                fom = LocalDate.of(2010, 1, 1),
                tom = LocalDate.of(2010, 6, 1)
            ),
            oppdatertBegrunnelseType = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET.vedtakBegrunnelseType,
            triggesAv = lagTriggesAv(vilkår = setOf(Vilkår.BOSATT_I_RIKET)),
            aktuellePersonerForVedtaksperiode = personopplysningGrunnlag.personer.toList()
                .map { it.tilBrevPerson() },
            erFørsteVedtaksperiodePåFagsak = false
        )

        assertEquals(1, personerMedUtgjørendeVilkårBosattIRiket.size)
        assertEquals(barn1Fnr, personerMedUtgjørendeVilkårBosattIRiket.first().aktivPersonIdent)
    }
}
