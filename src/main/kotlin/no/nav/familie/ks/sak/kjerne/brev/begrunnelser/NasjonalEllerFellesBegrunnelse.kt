package no.nav.familie.ks.sak.kjerne.brev.begrunnelser

import com.fasterxml.jackson.annotation.JsonValue

enum class NasjonalEllerFellesBegrunnelse : IBegrunnelse {
    INNVILGET_IKKE_BARNEHAGE {
        override val sanityApiNavn = "innvilgetIkkeBarnehage"
        override val begrunnelseType = BegrunnelseType.INNVILGET
    },
    INNVILGET_IKKE_BARNEHAGE_ADOPSJON {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetIkkeBarnehageAdopsjon"
    },
    INNVILGET_IKKE_BARNEHAGE_0125 {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetIkkeBarnehage0125"
    },
    INNVILGET_DELTIDSPLASS_BARNEHAGE_0125 {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetDeltidsplassBarnehage0125"
    },
    INNVILGET_OVERGANGSORDNING {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetOvergangsordning"
    },
    INNVILGET_OVERGANGSORDNING_GRADERT_UTBETALING {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetOvergangsordningGradertUtbetaling"
    },
    INNVILGET_OVERGANGSORDNING_DELT_BOSTED {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetOvergangsordningDeltBosted"
    },
    INNVILGET_DELTID_BARNEHAGE {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetDeltidBarnehage"
    },
    INNVILGET_DELTID_BARNEHAGE_ADOPSJON {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetDeltidBarnehageAdopsjon"
    },
    INNVILGET_MÅNEDEN_ETTER_ETT_ÅR {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetMaanedenEtterEttAar"
    },
    INNVILGET_MÅNEDEN_ETTER_SLUTTET_I_BARNEHAGE {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetMaanedenEtterSluttetIBarnehage"
    },
    INNVILGET_BOR_FAST_HOS_SØKER {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetBorFastHosSoker"
    },
    INNVILGET_BOSATT_I_NORGE {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetBosattINorge"
    },
    INNVILGET_BOSATT_I_NORGE_SØKER {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetBosattINorgeSoker"
    },
    INNVILGET_FORELDRENE_BOR_SAMMEN_ENDRET_MOTTAKER {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetForeldreneBorSammenEndretMottaker"
    },
    INNVILGET_DELT_BOSTED_MÅNED_ETTER_SØKNAD {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetDeltBostedMaanedEtterSoknad"
    },
    INNVILGET_DELT_BOSTED_MÅNED_ETTER_ENIGHET {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetDeltBostedMaanedEtterEnighet"
    },
    INNVILGET_MEDLEMSKAP_I_FOLKETRYGDEN {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetMedlemskapIFolketrygden"
    },
    INNVILGET_MEDLEMSKAP_FOLKETRYGDEN_OG_EØS_LAND {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetMedlemskapFolketrygdenOgEOSLand"
    },
    INNVILGET_FRA_MÅNEDEN_ETTER_FORELDREPENGER {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetFraMaanedenEtterForeldrepenger"
    },
    INNVILGET_OPPHOLD_I_EØS_LAND {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetOppholdIEosLand"
    },
    INNVILGET_TREDJELANDSBORGER_LOVLIG_OPPHOLD_FØR_BOSATT_I_NORGE {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetTredjelandsborgerLovligOppholdForBosattINorge"
    },
    INNVILGET_TREDJELANDSBORGER_BOSATT_FØR_LOVLIG_OPPHOLD_I_NORGE {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetTredjelandsborgerBosattForLovligOppholdINorge"
    },
    INNVILGET_INNVANDRET_MED_LOVLIG_OPPHOLD_TREDJELANDSBORGER {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetInnvandretMedLovligOppholdTredjelandsborger"
    },
    INNVILGET_VURDERING_BOR_FAST_HOS_SØKER {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetVurderingBorFastHosSoker"
    },
    INNVILGET_RETTSAVGJØRELSE_BOR_FAST_HOS_SØKER {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetRettsavgjorelseBorFastHosSoker"
    },
    INNVILGET_SATSENDRING {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetSatsendring"
    },
    INNVILGET_ERKLÆRING_OM_MOTREGNING {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetErklaringOmMotregning"
    },
    INNVILGET_13_MND_SAMME_MÅNED_SOM_BARNEHAGEPLASS_0824 {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilget13MndSammeMaanedSomBarnehageplass0824"
    },
    INNVILGET_PÅ_GRUNN_AV_LOVENDRING_2024 {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetInnvilgetPaaGrunnAvLovendring"
    },
    INNVILGET_OVERGANG_EØS_TIL_NASJONAL {
        override val begrunnelseType = BegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetOvergangEosTilNasjonal"
    },
    AVSLAG_UREGISTRERT_BARN {
        override val begrunnelseType = BegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagUregistrertBarn"
    },
    AVSLAG_IKKE_MEDLEM_FOLKETRYGDEN_I_FEM_ÅR {
        override val begrunnelseType = BegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagIkkeMedlemFolketrygdenIFemAar"
    },
    AVSLAG_DEN_ANDRE_FORELDEREN_IKKE_MEDLEM_I_FEM_ÅR {
        override val begrunnelseType = BegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagDenAndreForelderenIkkeMedlemIFemAar"
    },
    AVSLAG_VURDERING_IKKE_MEDLEM_FOLKETRYGDEN_I_FEM_ÅR {
        override val begrunnelseType = BegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagVurderingIkkeMedlemFolketrygdenIFemAar"
    },
    AVSLAG_VURDERING_DEN_ANDRE_FORELDEREN_IKKE_MEDLEM_I_FEM_ÅR {
        override val begrunnelseType = BegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagVurderingDenAndreForelderenIkkeMedlemIFemAar"
    },
    AVSLAG_FULLTIDSPLASS_I_BARNEHAGE {
        override val begrunnelseType = BegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagFulltidsplassIBarnehage"
    },
    AVSLAG_BRUKER_MELDER_FULLTIDSPLASS_I_BARNEHAGE {
        override val begrunnelseType = BegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagBrukerMelderFulltidsplassIBarnehage"
    },
    AVSLAG_KOMMUNEN_MELDER_FULLTIDSPLASS_I_BARNEHAGE {
        override val begrunnelseType = BegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagKommunenMelderFulltidsplassIBarnehage"
    },
    AVSLAG_BARN_OVER_TO_ÅR {
        override val begrunnelseType = BegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagBarnOverToAar"
    },
    AVSLAG_BEGYNT_PÅ_SKOLEN {
        override val begrunnelseType = BegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagBegyntPaaSkolen"
    },
    AVSLAG_MOTTATT_I_11_MÅNEDER {
        override val begrunnelseType = BegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagMottattI11Maaneder"
    },
    AVSLAG_FOSTERBARN {
        override val begrunnelseType = BegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagFosterbarn"
    },
    AVSLAG_INSTITUSJON {
        override val begrunnelseType = BegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagInstitusjon"
    },
    AVSLAG_FLYTTET_FRA_NORGE {
        override val begrunnelseType = BegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagFlyttetFraNorge"
    },
    AVSLAG_IKKE_BOSATT_I_NORGE {
        override val begrunnelseType = BegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagIkkeBosattINorge"
    },
    AVSLAG_VURDERING_IKKE_BOSATT_I_NORGE {
        override val begrunnelseType = BegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagVurderingIkkeBosattINorge"
    },
    AVSLAG_IKKE_OPPHOLDSTILLATELSE {
        override val begrunnelseType = BegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagIkkeOppholdstillatelse"
    },
    AVSLAG_IKKE_OPPHOLDSTILLATELSE_MER_ENN_TOLV_MÅNEDER {
        override val begrunnelseType = BegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagIkkeOppholdstillatelseMerEnnTolvMaaneder"
    },
    AVSLAG_OPPHOLD_UNDER_TOLV_MÅNEDER {
        override val begrunnelseType = BegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagOppholdUnderTolvMaaneder"
    },
    AVSLAG_VURDERING_OPPHOLD_UNDER_TOLV_MÅNEDER {
        override val begrunnelseType = BegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagVurderingOppholdUnderTolvMaaneder"
    },
    AVSLAG_BYTTE_AV_BARNEHAGE_I_SOMMERFERIEN {
        override val begrunnelseType = BegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagBytteAvBarnehageISommerferien"
    },
    AVSLAG_DEN_ANDRE_FORELDEREN_HAR_SØKT {
        override val begrunnelseType = BegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagDenAndreForelderenHarSokt"
    },
    AVSLAG_DEN_ANDRE_FORELDEREN_HAR_FÅTT_KONTANTSTØTTE {
        override val begrunnelseType = BegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagDenAndreForelderenHarFaattKontantstotte"
    },
    AVSLAG_DEN_ANDRE_FORELDEREN_MOTTAR_KONTANTSTØTTE {
        override val begrunnelseType = BegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagDenAndreForelderenMottarKontantstotte"
    },
    AVSLAG_UTENLANDSOPPHOLD_MER_ENN_TRE_MÅNEDER {
        override val begrunnelseType = BegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagUtenlandsoppholdMerEnnTreMaaneder"
    },
    AVSLAG_VURDERING_UTENLANDSOPPHOLD_MER_ENN_TRE_MÅNEDER {
        override val begrunnelseType = BegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagVurderingUtenlandsoppholdMerEnnTreMaaneder"
    },
    AVSLAG_BOR_IKKE_FAST_HOS_SØKER {
        override val begrunnelseType = BegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagBorIkkeFastHosSoker"
    },
    AVSLAG_VURDERING_BOR_IKKE_FAST_HOS_SØKER {
        override val begrunnelseType = BegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagVurderingBorIkkeFastHosSoker"
    },
    AVSLAG_ET_BARN_ER_DØD {
        override val begrunnelseType = BegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagEtBarnErDod"
    },
    AVSLAG_FLERE_BARN_ER_DØDE {
        override val begrunnelseType = BegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagFlereBarnErDode"
    },
    AVSLAG_IKKE_MEDLEM_FOLKETRYGDEN_ELLER_EØS_I_FEM_ÅR {
        override val begrunnelseType = BegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagIkkeMedlemFolketrygdenEllerEOSIFemAar"
    },
    AVSLAG_DEN_ANDRE_FORELDEREN_IKKE_MEDLEM_FOLKETRYGDEN_ELLER_EOS_I_FEM_ÅR {
        override val begrunnelseType = BegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagDenAndreForelderenIkkeMedlemFolketrygdenEllerEOSIFemAar"
    },
    AVSLAG_VURDERING_IKKE_MEDLEM_FOLKETRYGDEN_ELLER_EØS_I_FEM_ÅR {
        override val begrunnelseType = BegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagVurderingIkkeMedlemFolketrygdenEllerEOSIFemAar"
    },
    AVSLAG_VURDERING_DEN_ANDRE_FORELDEREN_IKKE_MEDLEM_FOLKETRYGDEN_ELLER_EOS_I_FEM_ÅR {
        override val begrunnelseType = BegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagVurderingDenAndreForelderenIkkeMedlemFolketrygdenEllerEOSIFemAar"
    },
    AVSLAG_IKKE_ENIG_OM_DELING {
        override val begrunnelseType = BegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagIkkeEnigOmDeling"
    },
    AVSLAG_IKKE_AVTALE_OM_DELT_BOSTED {
        override val begrunnelseType = BegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagIkkeAvtaleOmDeltBosted"
    },
    AVSLAG_AVTALE_OM_DELT_BOSTED_IKKE_GYLDIG {
        override val begrunnelseType = BegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagAvtaleOmDeltBostedIkkeGyldig"
    },
    AVSLAG_SØKER_FOR_SENT {
        override val begrunnelseType = BegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagSokerForSent"
    },
    AVSLAG_EKTEFELLE_ELLER_SAMBOERS_SÆRKULLSBARN {
        override val begrunnelseType = BegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagEktefelleEllerSamboerssearkullsbarn"
    },
    AVSLAG_OPPHOLDSRETT_EØS_BORGER {
        override val begrunnelseType = BegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagOppholdsrettEOSBorger"
    },
    AVSLAG_SØKT_FOR_SENT_ENDRINGSPERIODE {
        override val begrunnelseType = BegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagSokerForSentEndringsperiode"
    },
    AVSLAG_MOTTAR_FULLE_FORELDREPENGER {
        override val begrunnelseType = BegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagMottarFulleForeldrepenger"
    },
    AVSLAG_FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024 {
        override val begrunnelseType = BegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagFulltidsplassIBarnehageAugust2024"
    },
    AVSLAG_BARN_UNDER_12_MÅNEDER {
        override val begrunnelseType = BegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagBarnUnder12maaneder"
    },
    OPPHØR_VURDERING_IKKE_MEDLEM_I_FOLKETRYGDEN_I_5_ÅR {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorVurderingIkkeMedlemIFolketrygdenI5Aar"
    },
    OPPHØR_SØKER_BER_OM_OPPHØR {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorSokerBerOmOpphor"
    },
    OPPHØR_VURDERING_HAR_OG_SKAL_OPPHOLDE_SEG_OVER_12_MÅNEDER {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorVurderingHarOgSkalOppholdeSegOver12Maaneder"
    },
    OPPHØR_VURDERING_ANNEN_FORELDER_IKKE_MEDLEM_FOLKETRYGDEN_I_5_ÅR {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorVurderingAnnenForelderIkkeMedlemFolketrygdenI5Aar"
    },
    OPPHØR_FULLTIDSPLASS_I_BARNEHAGE {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorFulltidsplassIBarnehage"
    },
    OPPHØR_BRUKER_MELDER_FULLTIDSPLASS_I_BARNEHAGEN_FØRSTE_PERIODE {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorBrukerMelderFulltidsplassIBarnehagenForstePeriode"
    },
    OPPHØR_BRUKER_MELDER_FULLTIDSPLASS_I_BARNEHAGE {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorBrukerMelderFulltidsplassIBarnehage"
    },
    OPPHØR_BEGYNT_PÅ_SKOLEN {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorBegyntPaaSkolen"
    },
    OPPHØR_ANNEN_FORELDER_IKKE_MEDLEM_FOLKETRYGDEN_I_5_ÅR {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorAnnenForelderIkkeMedlemFolketrygdenI5Aar"
    },
    OPPHØR_IKKE_MEDLEM_FOLKETRYGDEN_ELLER_EOS_I_5_ÅR {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorIkkeMedlemFolketrygdenEllerEosI5Aar"
    },
    OPPHØR_BARN_OVER_2_ÅR {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorBarnOver2Aar"
    },
    OPPHØR_IKKE_AVALE_OM_DELT_BOSTED {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorIkkeAvtaleOmDeltBosted"
    },
    OPPHØR_VURDERING_UTENLANDSOPPHOLD_MER_ENN_3_MÅNEDER_FRA_INNVILGELSE {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorVurderingUtenlandsoppholdMerEnn3MaanederFraInnvilgelse"
    },
    OPPHØR_IKKE_BOSATT_I_NORGE {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorIkkeBosattINorge"
    },
    OPPHØR_VURDERING_IKKE_BOSATT_I_NORGE {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorVurderingIkkeBosattINorge"
    },
    OPPHØR_VURDERING_BOR_IKKE_FAST_MED_SØKER {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorVurderingBorIkkeFastMedSoker"
    },
    OPPHØR_ET_BARN_ER_DØD {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorEtBarnErDod"
    },
    OPPHØR_IKKE_OPPHOLDSTILLATELSE {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorIkkeOppholdstillatelse"
    },
    OPPHØR_FOSTERHJEM {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorFosterhjem"
    },
    OPPHØR_INSTITUSJON {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorInstitusjon"
    },
    OPPHØR_HAR_OG_SKAL_OPPHOLDE_SEG_IKKE_OVER_12_MÅNEDER {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorHarOgSkalOppholdeSegIkkeOver12Maaneder"
    },
    OPPHØR_BARN_BODDE_IKKE_MED_SØKER {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorBarnBoddeIkkeMedSoker"
    },
    OPPHØR_FULLTIDSPLASS_I_BARNEHAGEN_FØRSTE_PERIODE {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorFulltidsplassIBarnehagenForstePeriode"
    },
    OPPHØR_UTENLANDSOPPHOLD_MER_ENN_3_MÅNEDER {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorUtenlandsoppholdMerEnn3Maaneder"
    },
    OPPHØR_MOTTATT_I_11_MÅNEDER {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorMottattI11Maaneder"
    },
    OPPHØR_FLERE_BARN_ER_DØDE {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorFlereBarnErDode"
    },
    OPPHØR_BRUKER_MELDT_FULLTIDSPLASS_I_BARNEHAGE_FØRSTE_PERIODE {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorBrukerMeldtFulltidsplassIBarnehageForstePeriode"
    },
    OPPHØR_BYTTE_AV_BARNEHAGE_I_SOMMERFERIEN {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorBytteAvBarnehageISommerferien"
    },
    OPPHØR_KOMMUNEN_MELDT_FULLTIDSPLASS_I_BARNEHAGE_FØRSTE_PERIODE {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorKommunenMeldtFulltidsplassIBarnehageForstePeriode"
    },
    OPPHØR_KOMMUNEN_MELDER_FULLTIDSPLASS_I_BARNEHAGE {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorKommunenMelderFulltidsplassIBarnehage"
    },
    OPPHØR_UTENLANDSOPPHOLD_MER_ENN_3_MÅNEDER_FRA_INNVILGELSE {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorUtenlandsoppholdMerEnn3MaanederFraInnvilgelse"
    },
    OPPHØR_VURDERING_UTENLANDSOPPHOLD_MER_ENN_3_MÅNEDER {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorVurderingUtenlandsoppholdMerEnn3Maaneder"
    },
    OPPHØR_FORELDRENE_BOR_SAMMEN {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorForeldreneBorSammen"
    },
    OPPHØR_VURDERING_IKKE_MEDLEM_I_FOLKETRYGDEN_ELLER_EØS_I_5_AAR {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorVurderingIkkeMedlemIFolketrygdenEllerEosI5Aar"
    },
    OPPHØR_ANNEN_FORELDER_IKKE_MEDLEM_FOLKETRYGDEN_I_FEM_ÅR {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorAnnenForelderIkkeMedlemFolketrygdenIFemAar"
    },
    OPPHØR_ANNEN_FORELDER_IKKE_MEDLEM_FOLKETRYGDEN_ELLER_EØS_I_FEM_ÅR {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorAnnenForelderIkkeMedlemFolketrygdenEllerEOSIFemAar"
    },
    OPPHØR_VURDERING_ANNEN_FORELDER_IKKE_MEDLEM_FOLKETRYGDEN_I_FEM_ÅR {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorVurderingAnnenForelderIkkeMedlemFolketrygdenIFemAar"
    },
    OPPHØR_VURDERING_ANNEN_FORELDER_IKKE_MEDLEM_FOLKETRYGDEN_ELLER_EØS_I_FEM_ÅR {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorVurderingAnnenForelderIkkeMedlemFolketrygdenEllerEOSIFemAar"
    },
    OPPHØR_IKKE_OPPHOLDSTILLATELSE_EØS_BORGER {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorIkkeOppholdsrettEOSBorger"
    },
    OPPHØR_FRA_START_IKKE_BOSATT_I_NORGE {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorFraStartIkkeBosattINorge"
    },
    OPPHØR_FLYTTET_FRA_NORGE {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorFlyttetFraNorge"
    },
    OPPHØR_IKKE_MEDLEM_I_FOLKETRYGDEN_I_5_ÅR {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorIkkeMedlemIFolketrygdenI5Aar"
    },
    OPPHØR_IKKE_ENIG_OM_DELING {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorIkkeEnigOmDeling"
    },
    OPPHØR_IKKE_OPPHOLDSTILLATELSE_MER_ENN_12_MÅNEDER {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorIkkeOppholdstillatelseMerEnn12Maaneder"
    },
    OPPHØR_BEGGE_FORELDRENE_HAR_FÅTT_KONTANTSTØTTE {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorBeggeForeldreneHarFottKontantstotte"
    },
    OPPHØR_IKKE_OPPHOLDTILLATELSE {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorIkkeOppholdtillatelse"
    },
    OPPHØR_BOR_IKKE_FAST_HOS_SØKER {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorBorIkkeFastHosSoker"
    },
    OPPHØR_AVTALE_OM_DELT_BOSTED_IKKE_GYLDIG {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorAvtaleOmDeltBostedIkkeGyldig"
    },
    OPPHØR_DELT_BOSTED_BRUKER_MELDER_OM_DELTIDSPLASS {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorDeltBostedBrukerMelderOmDeltidsplass"
    },
    OPPHØR_DELT_BOSTED_KOMMUNEN_MELDER_OM_DELTIDSPLASS {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorDeltBostedKommunenMelderOmDeltidsplass"
    },
    OPPHØR_FRAMTIDIG_OPPHØR_BARNEHAGEPLASS {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorFramtidigOpphorBarnehageplass"
    },
    OPPHØR_NYTT_FRAMTIDIG_OPPHØR_BARNEHAGEPLASS {
        override val begrunnelseType = BegrunnelseType.OPPHØR
        override val sanityApiNavn = "opphorNyttFramtidigOpphorBarnehageplass"
    },
    OPPHØR_OVERGANGSORDNING_OPPHØR {
        override val sanityApiNavn = "opphorOvergangsordningOpphor"
        override val begrunnelseType = BegrunnelseType.OPPHØR
    },
    OPPHØR_TILLEGSTEKST_FOR_REGLER_FØR_01_08_2024 {
        override val sanityApiNavn = "opphorTilleggstekstForReglerFor010824"
        override val begrunnelseType = BegrunnelseType.OPPHØR
    },
    REDUKSJON_BARN_FLYTTET_FRA_SOKER {
        override val begrunnelseType = BegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonBarnFlyttetFraSoker"
    },
    REDUKSJON_TILDELT_BARNEHAGEPLASS {
        override val begrunnelseType = BegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonTildeltBarnehageplass"
    },
    REDUKSJON_SOKER_OPPLYST_OM_BARNEHAGEPLASS {
        override val begrunnelseType = BegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonSokerOpplystOmBarnehageplass"
    },
    REDUKSJON_KOMMUNEN_OPPLYST_OM_BARNEHAGEPLASS {
        override val begrunnelseType = BegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonKommunenOpplystOmBarnehageplass"
    },
    REDUKSJON_FRAMTIDIG_OPPHØR_BARNEHAGEPLASS {
        override val begrunnelseType = BegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonFramtidigOpphorBarnehageplass"
    },
    REDUKSJON_BARN_FLYTTET_FRA_NORGE {
        override val begrunnelseType = BegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonBarnFlyttetFraNorge"
    },
    REDUKSJON_BARN_DOD {
        override val begrunnelseType = BegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonBarnDod"
    },
    REDUKSJON_AVTALE_OM_DELT_BOSTED_OPPHORT {
        override val begrunnelseType = BegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonAvtaleOmDeltBostedOpphort"
    },
    REDUKSJON_HAR_IKKE_AVTALE_OM_DELT_BOSTED {
        override val begrunnelseType = BegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonHarIkkeAvtaleOmDeltBosted"
    },
    REDUKSJON_AVTALE_OM_AT_BARNET_BOR_HOS_DEN_ANDRE_FORELDEREN {
        override val begrunnelseType = BegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonAvtaleOmAtBarnetBorHOsDenAndreForelderen"
    },
    REDUKSJON_AVTALE_OM_DELT_BOSTED {
        override val begrunnelseType = BegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonAvtaleOmDeltBosted"
    },
    REDUKSJON_FORELDRENE_IKKE_LENGER_ENIG_OM_AA_DELE_KONTANTSTOTTEN {
        override val begrunnelseType = BegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonForeldreneIkkeLengerEnigOmAaDeleKontantstotten"
    },
    REDUKSJON_BARN_HAR_FYLT_TO_AAR {
        override val begrunnelseType = BegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonBarnHarFyltToAar"
    },
    REDUKSJON_MOTTATT_KONTANTSTOTTE_I_11_MAANEDER {
        override val begrunnelseType = BegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonMottattKontantstotteI11Maaneder"
    },
    REDUKSJON_BARN_BEGYNT_PAA_SKOLEN {
        override val begrunnelseType = BegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonBarnBegyntPaaSkolen"
    },
    REDUKSJON_SATSENDRING {
        override val begrunnelseType = BegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonSatsendring"
    },
    REDUKSJON_DEN_ANDRE_FORELDEREN_HAR_SOKT {
        override val begrunnelseType = BegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonDenAndreForelderenHarSokt"
    },
    REDUKSJON_SOKER_BER_OM_STANS {
        override val begrunnelseType = BegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonSokerBerOmStans"
    },
    REDUKSJON_IKKE_BOSATT_I_NORGE {
        override val begrunnelseType = BegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonIkkeBosattINorge"
    },
    REDUKSJON_BARNET_BODDE_IKKE_FAST_HOS_SOKER {
        override val begrunnelseType = BegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonBarnetBoddeIkkeFastHosSoker"
    },
    REDUKSJON_IKKE_OPPHOLDSTILLATELSE {
        override val begrunnelseType = BegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonIkkeOppholdstillatelse"
    },
    REDUKSJON_DEN_ANDRE_FORELDEREN_HAR_FOTT_FRA_SAMME_TIDSROM {
        override val begrunnelseType = BegrunnelseType.REDUKSJON
        override val sanityApiNavn = "reduksjonDenAndreForelderenHarFottFraSammeTidsrom"
    },
    FORTSATT_INNVILGET_OPPHOLD_I_NORGE {
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetOppholdINorge"
    },
    FORTSATT_INNVILGET_TREDJELANDSBORGER_FORTSATT_LOVLIGOPPHOLD {
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetTredjelandsborgerFortsattLovligOpphold"
    },
    FORTSATT_INNVILGET_BARN_BOR_MED_SOKER {
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetBarnBorMedSoker"
    },
    FORTSATT_INNVILGET_VURDERING_BARN_BOR_MED_SOKER {
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetVurderingBarnBorMedSoker"
    },
    FORTSATT_INNVILGET_OPPHOLDSRETT {
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetOppholdsrett"
    },
    FORTSATT_INNVILGET_HAR_KONTANTSTOTTEN_DET_ER_SOKT_OM {
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetHarKontantstottenDetErSoktOm"
    },
    FORTSATT_INNVILGET_DELT_KONTANTSTOTTE_PRAKTISERES_FORTSATT {
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetDeltKontantstottePraktiseresFortsatt"
    },
    FORTSATT_INNVILGET_FORTSATT_AVTALE_OM_DELT_BOSTED {
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetFortsattAvtaleOmDeltBosted"
    },
    FORTSATT_INNVILGET_FORTSATT_RETTSAVGJORELSE_OM_DELT_BOSTED {
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetFortsattRettsavgjorelseOmDeltBosted"
    },
    FORTSATT_INNVILGET_IKKE_BARNEHAGE {
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetIkkeBarnehage"
    },
    FORTSATT_INNVILGET_DELTIDSPLASS_I_BARNEHAGE {
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetDeltidsplassIBarnehage"
    },
    FORTSATT_INNVILGET_MEDLEMSKAP_I_FOLKETRYGDEN {
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetMedlemskapIFolketrygden"
    },
    FORTSATT_INNVILGET_MEDLEMSKAP_DEN_ANDRE_FORELDEREN {
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetMedlemskapDenAndreForelderen"
    },
    FORTSATT_INNVILGET_MEDLEMSKAP_FOLKETRYGDEN_OG_EOS_LAND {
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetMedlemskapFolketrygdenOgEosLand"
    },
    FORTSATT_INNVILGET_MEDLEMSKAP_FOLKETRYGDEN_OG_EOS_LAND_DEN_ANDRE_FORELDEREN {
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
        override val sanityApiNavn = "fortsattInnvilgetMedlemskapFolketrygdenOgEosLandDenAndreForelderen"
    },

    // Begrunnelser knyttet til lovendring 2024

    INNVILGET_IKKE_BARNEHAGE_0824 {
        override val sanityApiNavn = "innvilgetIkkeBarnehage0824"
        override val begrunnelseType = BegrunnelseType.INNVILGET
    },

    INNVILGET_DELTIDSPLASS_BARNEHAGE_0824 {
        override val sanityApiNavn = "innvilgetDeltidsplassBarnehage0824"
        override val begrunnelseType = BegrunnelseType.INNVILGET
    },

    INNVILGET_MEDLEMSKAP_FOLKETRYGDEN_0824 {
        override val sanityApiNavn = "innvilgetMedlemskapFolketrygden0824"
        override val begrunnelseType = BegrunnelseType.INNVILGET
    },

    INNVILGET_MEDLEMSKAP_FOLKETRYGDEN_OG_EØS_LAND_0824 {
        override val sanityApiNavn = "innvilgetMedlemskapFolketrygdenOgEosLand0824"
        override val begrunnelseType = BegrunnelseType.INNVILGET
    },

    INNVILGET_MÅNEDEN_BARNET_ER_13_MÅNEDER_0824 {
        override val sanityApiNavn = "innvilgetMaanedenBarneter13Maaneder0824"
        override val begrunnelseType = BegrunnelseType.INNVILGET
    },

    INNVILGET_MÅNEDEN_BARNET_SLUTTET_I_BARNEHAGE_0824 {
        override val sanityApiNavn = "innvilgetMaanedenBarnetSluttetIBarnehage0824"
        override val begrunnelseType = BegrunnelseType.INNVILGET
    },

    INNVILGET_BOR_FAST_HOS_SØKER_0824 {
        override val sanityApiNavn = "innvilgetBorFastHosSoker0824"
        override val begrunnelseType = BegrunnelseType.INNVILGET
    },

    INNVILGET_BOSATT_I_NORGE_0824 {
        override val sanityApiNavn = "innvilgetBosattINorge0824"
        override val begrunnelseType = BegrunnelseType.INNVILGET
    },

    INNVILGET_DELT_BOSTED_SØKNAD_0824 {
        override val sanityApiNavn = "innvilgetDeltBostedSoknad0824"
        override val begrunnelseType = BegrunnelseType.INNVILGET
    },

    INNVILGET_DELT_BOSTED_ENIGHET_0824 {
        override val sanityApiNavn = "innvilgetDeltBostedEnighet0824"
        override val begrunnelseType = BegrunnelseType.INNVILGET
    },

    INNVILGET_MÅNEDEN_FORELDREPENGER_UTLØPER_0824 {
        override val sanityApiNavn = "innvilgetMaanedenForeldrepengerUtloper0824"
        override val begrunnelseType = BegrunnelseType.INNVILGET
    },

    INNVILGET_MÅNEDEN_ETTER_SLUTTET_I_FULLTIDSPLASS_0125 {
        override val sanityApiNavn = "innvilgetMaanedenEtterSluttetIFulltidsplass0125"
        override val begrunnelseType = BegrunnelseType.INNVILGET
    },
    INNVILGET_TREDJELANDSBORGER_MED_LOVLIG_OPPHOLD_SAMTIDIG_SOM_BOSATT_I_NORGE_0824 {
        override val sanityApiNavn = "innvilgetTredjelandsborgerMedLovligOppholdSamtidigSomBosattINorge0824"
        override val begrunnelseType = BegrunnelseType.INNVILGET
    },

    INNVILGET_TREDJELANDSBORGER_LOVLIG_OPPHOLD_FOR_BOSATT_I_NORGE_0824 {
        override val sanityApiNavn = "innvilgetTredjelandsborgerLovligOppholdForBosattINorge0824"
        override val begrunnelseType = BegrunnelseType.INNVILGET
    },

    INNVILGET_TREDJELANDSBORGER_BOSATT_FOR_LOVLIG_OPPHOLD_I_NORGE_0824 {
        override val sanityApiNavn = "innvilgetTredjelandsborgerBosattForLovligOppholdINorge0824"
        override val begrunnelseType = BegrunnelseType.INNVILGET
    },

    INNVILGET_VURDERING_BOR_FAST_HOS_SØKER_0824 {
        override val sanityApiNavn = "innvilgetVurderingBorFastHosSoker0824"
        override val begrunnelseType = BegrunnelseType.INNVILGET
    },

    INNVILGET_RETTSAVGJØRELSE_BOR_FAST_HOS_SØKER_0824 {
        override val sanityApiNavn = "innvilgetRettsavgjorelseBorFastHosSoker0824"
        override val begrunnelseType = BegrunnelseType.INNVILGET
    },

    INNVILGET_BOSATT_ETTER_UTENLANDSOPPHOLD {
        override val sanityApiNavn = "innvilgetBosattEtterUtenlandsopphold"
        override val begrunnelseType = BegrunnelseType.INNVILGET
    },

    INNVILGET_BOSATT_ETTER_UTENLANDSOPPHOLD_0824 {
        override val sanityApiNavn = "innvilgetBosattEtterUtenlandsopphold0824"
        override val begrunnelseType = BegrunnelseType.INNVILGET
    },

    REDUKSJON_BARN_HAR_FYLT_19_MÅNEDER_0824 {
        override val sanityApiNavn = "reduksjonBarnHarFylt19Maaneder0824"
        override val begrunnelseType = BegrunnelseType.REDUKSJON
    },

    REDUKSJON_ADOPSJON_MOTTATT_KONTANTSTØTTE_I_7_MÅNEDER_0824 {
        override val sanityApiNavn = "reduksjonAdopsjonMottattKontantstotteI7Maaneder0824"
        override val begrunnelseType = BegrunnelseType.REDUKSJON
    },

    OPPHØR_BARN_OVER_19_MND_0824 {
        override val sanityApiNavn = "opphorBarnOver19Mnd0824"
        override val begrunnelseType = BegrunnelseType.OPPHØR
    },

    OPPHØR_MOTTATT_I_7_MND_0824 {
        override val sanityApiNavn = "opphorMottattI7Mnd0824"
        override val begrunnelseType = BegrunnelseType.OPPHØR
    },

    AVSLAG_BARN_OVER_19_MND_0824 {
        override val sanityApiNavn = "avslagBarnOver19Mnd0824"
        override val begrunnelseType = BegrunnelseType.AVSLAG
    },

    AVSLAG_MOTTATT_I_7_MÅNEDER_0824 {
        override val sanityApiNavn = "avslagMottattI7Maaneder0824"
        override val begrunnelseType = BegrunnelseType.AVSLAG
    },

    AVSLAG_ENDRINGSPERIODE_ALLEREDE_UTBETALT_SØKER {
        override val sanityApiNavn = "avslagAlleredeUtbetaltSokerEndringsperiode"
        override val begrunnelseType = BegrunnelseType.AVSLAG
    },

    AVSLAG_ENDRINGSPERIODE_ALLEREDE_UTBETALT_ANNEN_FORELDER {
        override val sanityApiNavn = "avslagAlleredeUtbataltAnnenForelderEndringsperiode"
        override val begrunnelseType = BegrunnelseType.AVSLAG
    },

    ;

    @JsonValue
    override fun enumnavnTilString() = NasjonalEllerFellesBegrunnelse::class.simpleName + "$" + this.name
}
