package no.nav.familie.ks.sak.kjerne.brev.begrunnelser

import com.fasterxml.jackson.annotation.JsonValue

enum class EØSBegrunnelse : IBegrunnelse {
    INNVILGET_PRIMÆRLAND_BARNET_BOR_I_NORGE {
        override val sanityApiNavn = "innvilgetPrimarlandBarnetBorINorge"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_PRIMÆRLAND_STANDARD {
        override val sanityApiNavn = "innvilgetPrimarlandStandard"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_PRIMÆRLAND_ALENEANSVAR {
        override val sanityApiNavn = "innvilgetPrimarlandAleneansvar"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE {
        override val sanityApiNavn = "innvilgetPrimarlandBeggeForeldreBosattINorge"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_JOBBER_I_NORGE {
        override val sanityApiNavn = "innvilgetPrimarlandBeggeForeldreJobberINorge"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_PRIMÆRLAND_STANDARD_HAR_BARNEHAGE {
        override val sanityApiNavn = "innvilgetPrimarlandStandardHarBarnehage"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_PRIMÆRLAND_ALENEANSVAR_HAR_BARNEHAGE {
        override val sanityApiNavn = "innvilgetPrimarlandAleneansvarHarBarnehage"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE_HAR_BARNEHAGE {
        override val sanityApiNavn = "innvilgetPrimarlandBeggeForeldreBosattINorgeHarBarnehage"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_JOBBER_I_NORGE_HAR_BARNEHAGE {
        override val sanityApiNavn = "innvilgetPrimarlandBeggeForeldreJobberINorgeHarBarnehage"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_PRIMÆRLAND_BARNET_BOR_I_NORGE_HAR_BARNEHAGE {
        override val sanityApiNavn = "innvilgetPrimarlandBarnetBorINorgeHarBarnehage"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_PRIMÆRLAND_BARNET_FLYTTET_TIL_NORGE_HAR_BARNEHAGE {
        override val sanityApiNavn = "innvilgetPrimarlandBarnetFlyttetTilNorgeHarBarnehage"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_PRIMÆRLAND_ANDRE_FORELDEREN_UTSENDT_ARBEIDSTAKER_HAR_BARNEHAGE {
        override val sanityApiNavn = "innvilgetPrimarlandAndreForelderenUtsendtArbeidstakerHarBarnehage"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_SEKUNDÆRLAND_STANDARD_HAR_BARNEHAGE {
        override val sanityApiNavn = "innvilgetSekundarlandStandardHarBarnehage"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_SEKUNDÆRLAND_ALENEANSVAR_HAR_BARNEHAGE {
        override val sanityApiNavn = "innvilgetSekundarlandAleneansvarHarBarnehage"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_SEKUNDÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE_HAR_BARNEHAGE {
        override val sanityApiNavn = "innvilgetSekundarlandBeggeForeldreBosattINorgeHarBarnehage"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_SELVSTENDIG_RETT_PRIMÆRLAND_STANDARD_HAR_BARNEHAGE {
        override val sanityApiNavn = "innvilgetSelvstendigRettPrimarlandStandardHarBarnehage"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_SELVSTENDIG_RETT_PRIMÆRLAND_UTSENDT_ARBEIDSTAKER_HAR_BARNEHAGE {
        override val sanityApiNavn = "innvilgetSelvstendigRettPrimarlandUtsendtArbeidstakerHarBarnehage"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_SELVSTENDIG_RETT_PRIMÆRLAND_FÅR_YTELSE_I_UTLANDET_HAR_BARNEHAGE {
        override val sanityApiNavn = "innvilgetSelvstendigRettPrimarlandFaarYtelseIUtlandetHarBarnehage"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_SELVSTENDIG_RETT_SEKUNDÆRLAND_STANDARD_HAR_BARNEHAGE {
        override val sanityApiNavn = "innvilgetSelvstendigRettSekundarlandStandardHarBarnehage"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_SELVSTENDIG_RETT_SEKUNDÆRLAND_FÅR_YTELSE_I_UTLANDET_HAR_BARNEHAGE {
        override val sanityApiNavn = "innvilgetSelvstendigRettSekundarlandFaarYtelseIUtlandetharBarnehage"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_PRIMÆRLAND_UK_STANDARD {
        override val sanityApiNavn = "innvilgetPrimarlandUKStandard"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_PRIMÆRLAND_UK_ALENEANSVAR {
        override val sanityApiNavn = "innvilgetPrimarlandUKAleneansvar"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_PRIMÆRLAND_UK_OG_UTLAND_STANDARD {
        override val sanityApiNavn = "innvilgetPrimarlandUKOgUtlandStandard"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_PRIMÆRLAND_BARNET_FLYTTET_TIL_NORGE {
        override val sanityApiNavn = "innvilgetPrimarlandBarnetFlyttetTilNorge"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_PRIMÆRLAND_SÆRKULLSBARN_ANDRE_BARN {
        override val sanityApiNavn = "innvilgetPrimarlandSarkullsbarnAndreBarn"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_PRIMÆRLAND_SÆRKULLSBARN_ANDRE_BARN_OVERTATT_ANSVAR {
        override val sanityApiNavn = "innvilgetPrimarlandSarkullsbarnAndreBarnOvertattAnsvar"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_PRIMÆRLAND_TO_ARBEIDSLAND_ANNET_LAND_UTBETALER {
        override val sanityApiNavn = "innvilgetPrimarlandToArbeidslandAnnetLandUtbetaler"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_PRIMÆRLAND_UK_TO_ARBEIDSLAND_NORGE_UTBETALER {
        override val sanityApiNavn = "innvilgetPrimarlandUKToArbeidslandNorgeUtbetaler"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_PRIMÆRLAND_UK_TO_ARBEIDSLAND_ANNET_LAND_UTBETALER {
        override val sanityApiNavn = "innvilgetPrimarlandUKToArbeidslandAnnetLandUtbetaler"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_PRIMÆRLAND_UK_KONTANTSTØTTE_ALLEREDE_UTBETALT {
        override val sanityApiNavn = "innvilgetPrimarlandUKKontantstotteAlleredeUtbetalt"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_SEKUNDÆRLAND_STANDARD {
        override val sanityApiNavn = "innvilgetSekundarlandStandard"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_SEKUNDÆRLAND_ALENEANSVAR {
        override val sanityApiNavn = "innvilgetSekundarlandAleneansvar"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_SEKUNDÆRLAND_UK_STANDARD {
        override val sanityApiNavn = "innvilgetSekundarlandUKStandard"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_SEKUNDÆRLAND_UK_ALENEANSVAR {
        override val sanityApiNavn = "innvilgetSekundarlandUKAleneansvar"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_SEKUNDÆRLAND_UK_OG_UTLAND_STANDARD {
        override val sanityApiNavn = "innvilgetSekundarlandUKogUtlandStandard"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_SEKUNDÆRLAND_TO_ARBEIDSLAND_NORGE_UTBETALER {
        override val sanityApiNavn = "innvilgetSekundarlandToArbeidslandNorgeUtbetaler"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_SEKUNDÆRLAND_UK_TO_ARBEIDSLAND_NORGE_UTBETALER {
        override val sanityApiNavn = "innvilgetSekundarlandUKToArbeidslandNorgeUtbetaler"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_SEKUNDÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE {
        override val sanityApiNavn = "innvilgetSekundarlandBeggeForeldreBosattINorge"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_PRIMÆRLAND_DEN_ANDRE_FORELDEREN_UTSENDET_ARBEIDSTAKER {
        override val sanityApiNavn = "innvilgetPrimarlandDenAndreForelderenUtsendtArbeidstaker"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_SELVSTENDIG_RETT_PRIMÆRLAND_STANDARD {
        override val sanityApiNavn = "innvilgetSelvstendigRettPrimarlandStandard"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_SELVSTENDIG_RETT_PRIMÆRLAND_UK_STANDARD {
        override val sanityApiNavn = "innvilgetSelvstendigRettPrimarlandUKStandard"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_SELVSTENDIG_RETT_PRIMÆRLAND_UTSENDET_ARBEIDSTAKER {
        override val sanityApiNavn = "innvilgetSelvstendigRettPrimarlandUtsendtArbeidstaker"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_SELVSTENDIG_RETT_SEKUNDÆRLAND_STANDARD {
        override val sanityApiNavn = "innvilgetSelvstendigRettSekundarlandStandard"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_SELVSTENDIG_RETT_SEKUNDÆRLAND_UK_STANDARD {
        override val sanityApiNavn = "innvilgetSelvstendigRettSekundarlandUKStandard"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_SELVSTENDIG_RETT_PRIMÆRLAND_FOR_YTELSE_I_UTLANDET {
        override val sanityApiNavn = "innvilgetSelvstendigRettPrimarlandForYtelseIUtlandet"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_SELVSTENDIG_RETT_SEKUNDÆRLAND_FOR_YTELSE_I_UTLANDET {
        override val sanityApiNavn = "innvilgetSelvstendigRettSekundarlandForYtelseIUtlandet"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_TILLEGGSBEGRUNNELSE_UTBETALING_TIL_ANNEN_FORELDER {
        override val sanityApiNavn = "innvilgetTilleggsbegrunnelseUtbetalingTilAnnenForelder"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_KONTANTSTØTTE_ALLEREDE_UTBETALT {
        override val sanityApiNavn = "innvilgetKontantstotteAlleredeUtbetalt"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_TILLEGGSTEKST_NULLUTBETALING {
        override val sanityApiNavn = "innvilgetTilleggstekstNullutbetaling"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_TILLEGGSTEKST_SATSENDRING {
        override val sanityApiNavn = "innvilgetTilleggstekstSatsendring"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_TILLEGGSTEKST_VALUTAJUSTERING {
        override val sanityApiNavn = "innvilgetTilleggstekstValutajustering"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_TILLEGGSTEKST_SATSENDRING_OG_VALUTAJUSTERING {
        override val sanityApiNavn = "innvilgetTilleggstekstSatsendringOgValutajustering"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_TILLEGGSTEKST_SEKUNDÆR_DELT_BOSTED_ANNEN_FORELDER_IKKE_SOKT {
        override val sanityApiNavn = "innvilgetTilleggstekstSekundarDeltBostedAnnenForeldreIkkeSokt"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_TILLEGGSBEGRUNNELSE_VEDTAK_FOR_SED {
        override val sanityApiNavn = "innvilgetTilleggsbegrunnelseVedtakForSed"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_TILLEGGSTEKST_PRIMÆRLAND_DELT_BOSTED_ANNEN_FORELDER_IKKE_RETT {
        override val sanityApiNavn = "innvilgetTilleggstekstPrimarlandDeltBostedAnnenForelderIkkeRett"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_TILLEGGSTEKST_SEKUNDÆR_FULL_UTBETALING {
        override val sanityApiNavn = "innvilgetTilleggstekstSekundarFullUtbetaling"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_TILLEGGSTEKST_SEKUNDÆR_AVTALE_DELT_BOSTED {
        override val sanityApiNavn = "innvilgetTilleggstekstSekundarAvtaleDeltBosted"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_TILLEGGSTEKST_SEKUNDÆR_IKKE_FATT_SVAR_PÅ_SED {
        override val sanityApiNavn = "innvilgetTilleggstekstSekundarIkkeFattSvarPaaSed"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_TILLEGGSTEKST_SEKUNDÆR_IKKE_KONTANTSTØTTE_I_ANNET_LAND {
        override val sanityApiNavn = "innvilgetTilleggstekstSekundarIkkeKontantstotteIAnnetLand"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_TILLEGSTEKST_TO_ARBEIDSLAND_MER_ENN_25_PROSENT_ARBEID_I_NORGE {
        override val sanityApiNavn = "innvilgetTilleggstekstToArbeidslandMerEnn25ProsentArbeidINorge"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_SELVSTENDIG_RETT_TILLEGGSBEGRUNNELSE_VEDTAK_FOR_SED {
        override val sanityApiNavn = "innvilgetSelvstendigRettTilleggsbegrunnelseVedtakForSed"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_SELVSTENDIG_RETT_TILLEGGSTEKST_SEKUNDÆR_FULL_UTBETALING {
        override val sanityApiNavn = "innvilgetSelvstendigRettTilleggstekstSekundarFullUtbetaling"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_SELVSTENDIG_RETT_TILLEGGSTEKST_NULLUTBETALING {
        override val sanityApiNavn = "innvilgetSelvstendigRettTilleggstekstNullutbetaling"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_SELVSTENDIG_RETT_TILLEGGSTEKST_SEKUNDÆRLAND_IKKE_FATT_SVAR_PÅ_SED {
        override val sanityApiNavn = "innvilgetSelvstendigRettTilleggstekstSekundarlandIkkeFattSvarPaaSed"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_SELVSTENDIG_RETT_TILLEGGSTEKST_TO_ARBEIDSLAND_MER_ENN_25_PROSENT_I_NORGE {
        override val sanityApiNavn = "innvilgetSelvstendigRettTilleggstekstToArbeidslandMerEnn25INorge"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_TILLEGGSTEKST_DELT_BOSTED {
        override val sanityApiNavn = "innvilgetTilleggstekstDeltBosted"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_TILLEGGSTEKST_FULL_KONTANTSTØTTE_HAR_AVTALE_DELT {
        override val sanityApiNavn = "innvilgetTilleggstekstFullKontantstotteHarAvtaleDelt"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_SELVSTENDIG_RETT_TILLEGGSTEKST_IKKE_KONTANTSTØTTE_I_ANNET_LAND {
        override val sanityApiNavn = "innvilgetSelvstendigRettTilleggstekstIkkeKontantstotteIAnnetLand"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_TILLEGGSTEKST_SEKUNDÆR_DELT_BOSTED_ANNEN_FORELDER_IKKE_RETT {
        override val sanityApiNavn = "innvilgetTilleggstekstSekundarDeltBostedAnnenForelderIkkeRett"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_PRIMÆRLAND_TO_ARBEIDSLAND_NORGE_UTBETALER {
        override val sanityApiNavn = "innvilgetPrimarlandToArbeidslandNorgeUtbetaler"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    INNVILGET_PRIMÆRLAND_OPPHOLD_ANNET_EØS_LAND_NORGE_LOVVALGSLAND {
        override val sanityApiNavn = "innvilgetPrimarlandOppholdAnnetEosLandNorgeLovvalgsland"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_TILLEGGSTEKST_SEKUNDÆR_FÅR_IKKE_KS_I_ANNET_LAND {
        override val sanityApiNavn = "innvilgetTilleggstekstSekundarFaarIkkeKsIAnnetLand"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_SELVSTENDIG_RETT_TILLEGGSTEKST_SEKUNDÆR_FÅR_IKKE_KS_I_ANNET_LAND {
        override val sanityApiNavn = "innvilgetSelvstendigRettTilleggstekstSekundarFaarIkkeKsIAnnetLand"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_TILLEGGSTEKST_SEKUNDÆR_INFO_OM_MULIG_REFUSJON {
        override val sanityApiNavn = "innvilgetTilleggstekstSekundarInfoOmMuligRefusjon"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_SELVSTENDIG_RETT_TILLEGGSTEKST_SEKUNDÆR_INFO_OM_MULIG_REFUSJON {
        override val sanityApiNavn = "innvilgetSelvstendigRettTilleggstekstSekundarInfoOmMuligRefusjon"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },
    INNVILGET_NASJONAL_RETT_SEKUNDÆRLAND_STANDARD_BOSMANN {
        override val sanityApiNavn = "innvilgetNasjonalRettsekundarlandStandardBosmann"
        override val begrunnelseType = BegrunnelseType.EØS_INNVILGET
    },

    OPPHØR_EØS_STANDARD {
        override val sanityApiNavn = "opphorEosStandard"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_EØS_SØKER_BER_OM_OPPHØR {
        override val sanityApiNavn = "opphorEosSokerBerOmOpphor"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_BARN_BOR_IKKE_I_EØS_LAND {
        override val sanityApiNavn = "opphorBarnBorIkkeIEosLand"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_IKKE_STATSBORGER_I_EØS_LAND {
        override val sanityApiNavn = "opphorIkkeStatsborgerIEosLand"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_SEPARASJONSAVTALEN_GJELDER_IKKE {
        override val sanityApiNavn = "opphorSeparasjonsavtalenGjelderIkke"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_SENTRUM_FOR_LIVSINTERESSE {
        override val sanityApiNavn = "opphorSentrumForLivsinteresse"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_IKKE_ANSVAR_FOR_BARN {
        override val sanityApiNavn = "opphorIkkeAnsvarForBarn"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_IKKE_OPPHOLDSRETT_SOM_FAMILIEMEDLEM {
        override val sanityApiNavn = "opphorIkkeOppholdsrettSomFamiliemedlem"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_SØKER_OG_BARN_BOR_IKKE_I_EØS_LAND {
        override val sanityApiNavn = "opphorSokerOgBarnBorIkkeIEosLand"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_SØKER_BOR_IKKE_I_EØS_LAND {
        override val sanityApiNavn = "opphorSokerBorIkkeIEosLand"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_ARBEIDER_MER_ENN_25_PROSENT_I_ANNET_EØS_LAND {
        override val sanityApiNavn = "opphorArbeiderMerEnn25ProsentIAnnetEosLand"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_UTSENDT_ARBEIDSTAKER_FRA_ANNET_EØS_LAND {
        override val sanityApiNavn = "opphorUtsendtArbeidstakerFraAnnetEosLand"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_ETT_BARN_DOD_EØS {
        override val sanityApiNavn = "opphorEttBarnDodEos"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_FLERE_BARN_ER_DODE_EØS {
        override val sanityApiNavn = "opphorFlereBarnErDodeEos"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_SELVSTENDIG_RETT_OPPHØR {
        override val sanityApiNavn = "opphorSelvstendigRettOpphor"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_SELVSTENDIG_RETT_UTSENDT_ARBEIDSTAKER_FRA_ANNET_EØS_LAND {
        override val sanityApiNavn = "opphorSelvstendigRettUtsendtArbeidstakerFraAnnetEosLand"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_SELVSTENDIG_RETT_OPPHØR_FRA_START {
        override val sanityApiNavn = "opphorSelvstendigRettOpphorFraStart"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_SELVSTENDIG_RETT_VAR_IKKE_UTSENDT_ARBEIDSTAKER_FRA_ANNET_EØS_LAND {
        override val sanityApiNavn = "opphorSelvstendigRettVarIkkeUtsendtArbeidstakerFraAnnetEosLand"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_BARN_BODDE_IKKE_I_ET_EØS_LAND {
        override val sanityApiNavn = "opphorBarnBoddeIkkeIEtEosLand"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_SEPARASJONSAVTALEN_GJALDT_IKKE {
        override val sanityApiNavn = "opphorSeparasjonsavtalenGjaldtIkke"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_NORGE_VAR_IKKE_SENTRUM_FOR_LIVSINTERESSE {
        override val sanityApiNavn = "opphorNorgeVarIkkeSentrumForLivsinteresse"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_HADDE_IKKE_ANSVAR_FOR_BARN {
        override val sanityApiNavn = "opphorHaddeIkkeAnsvarForBarn"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_HADDE_IKKE_OPPHOLDSRETT_SOM_FAMILIEMEDLEM {
        override val sanityApiNavn = "opphorHaddeIkkeOppholdsrettSomFamiliemedlem"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_SØKER_OG_BARN_BODDE_IKKE_I_EØS_LAND {
        override val sanityApiNavn = "opphorSokerOgBarnBoddeIkkeIEosLand"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_SØKER_BODDE_IKKE_I_ET_EØS_LAND {
        override val sanityApiNavn = "opphorSokerBoddeIkkeIEtEosLand"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    OPPHØR_OVERGANGSORDNING_OPPHØR_EØS {
        override val sanityApiNavn = "opphorOvergangsordningOpphorEOS"
        override val begrunnelseType = BegrunnelseType.EØS_OPPHØR
    },

    REDUKSJON_BARN_DØD_EØS {
        override val sanityApiNavn = "reduksjonBarnDodEos"
        override val begrunnelseType = BegrunnelseType.EØS_REDUKSJON
    },

    REDUKSJON_SØKER_BER_OM_OPPHØR_EØS {
        override val sanityApiNavn = "reduksjonSokerBerOmOpphorEos"
        override val begrunnelseType = BegrunnelseType.EØS_REDUKSJON
    },

    REDUKSJON_BARN_BOR_IKKE_I_EØS_LAND {
        override val sanityApiNavn = "reduksjonBarnBorIkkeIEosLand"
        override val begrunnelseType = BegrunnelseType.EØS_REDUKSJON
    },

    REDUKSJON_IKKE_ANSVAR_FOR_BARN {
        override val sanityApiNavn = "reduksjonIkkeAnsvarForBarn"
        override val begrunnelseType = BegrunnelseType.EØS_REDUKSJON
    },

    REDUKSJON_TILLEGGSTEKST_VALUTAJUSTERING {
        override val sanityApiNavn = "reduksjonTilleggstekstValutajustering"
        override val begrunnelseType = BegrunnelseType.EØS_REDUKSJON
    },

    REDUKSJON_UK_MIDLERTIDIG_DIFFERANSEUTBETALING {
        override val sanityApiNavn = "reduksjonUKMidlertidigDifferanseutbetaling"
        override val begrunnelseType = BegrunnelseType.EØS_REDUKSJON
    },

    REDUKSJON_DELT_BOSTED_BEGGE_FORELDRE_IKKE_OMFATTET_NORSK_LOVVALG {
        override val sanityApiNavn = "reduksjonDeltBostedBeggeForeldreIkkeOmfattetNorskLovvalg"
        override val begrunnelseType = BegrunnelseType.EØS_REDUKSJON
    },

    REDUKSJON_SELVSTENDIG_RETT_BARN_FLYTTET_FRA_SØKER {
        override val sanityApiNavn = "reduksjonSelvstendigRettBarnFlyttetFraSoker"
        override val begrunnelseType = BegrunnelseType.EØS_REDUKSJON
    },

    FORTSATT_INNVILGET_PRIMÆRLAND_STANDARD {
        override val sanityApiNavn = "fortsattInnvilgetPrimarlandStandard"
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
    },

    FORTSATT_INNVILGET_PRIMÆRLAND_ALENEANSVAR {
        override val sanityApiNavn = "fortsattInnvilgetPrimarlandAleneansvar"
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
    },

    FORTSATT_INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE {
        override val sanityApiNavn = "fortsattInnvilgetPrimarlandBeggeForeldreBosattINorge"
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
    },

    FORTSATT_INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_JOBBER_I_NORGE {
        override val sanityApiNavn = "fortsattInnvilgetPrimarlandBeggeForeldreJobberINorge"
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
    },

    FORTSATT_INNVILGET_PRIMÆRLAND_UK_STANDARD {
        override val sanityApiNavn = "fortsattInnvilgetPrimarlandUKStandard"
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
    },

    FORTSATT_INNVILGET_PRIMÆRLAND_UK_ALENEANSVAR {
        override val sanityApiNavn = "fortsattInnvilgetPrimarlandUKAleneansvar"
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
    },

    FORTSATT_INNVILGET_PRIMÆRLAND_UK_OG_UTLAND_STANDARD {
        override val sanityApiNavn = "fortsattInnvilgetPrimarlandUKOgUtlandStandard"
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
    },

    FORTSATT_INNVILGET_PRIMÆRLAND_BARNET_BOR_I_NORGE {
        override val sanityApiNavn = "fortsattInnvilgetPrimarlandBarnetBorINorge"
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
    },

    FORTSATT_INNVILGET_PRIMÆRLAND_SÆRKULLSBARN_ANDRE_BARN {
        override val sanityApiNavn = "fortsattInnvilgetPrimarlandSarkullsbarnAndreBarn"
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
    },

    FORTSATT_INNVILGET_PRIMÆRLAND_TO_ARBEIDSLAND_NORGE_UTBETALER {
        override val sanityApiNavn = "fortsattInnvilgetPrimarlandToArbeidslandNorgeUtbetaler"
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
    },

    FORTSATT_INNVILGET_PRIMÆRLAND_TO_ARBEIDSLAND_ANNET_LAND_UTBETALER {
        override val sanityApiNavn = "fortsattInnvilgetPrimarlandToArbeidslandAnnetLandUtbetaler"
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
    },

    FORTSATT_INNVILGET_PRIMÆRLAND_UK_TO_ARBEIDSLAND_NORGE_UTBETALER {
        override val sanityApiNavn = "fortsattInnvilgetPrimarlandUKToArbeidslandNorgeUtbetaler"
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
    },

    FORTSATT_INNVILGET_PRIMÆRLAND_UK_TO_ARBEIDSLAND_ANNET_LAND_UTBETALER {
        override val sanityApiNavn = "fortsattInnvilgetPrimarlandUKToArbeidslandAnnetLandUtbetaler"
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
    },

    FORTSATT_INNVILGET_SEKUNDÆRLAND_STANDARD {
        override val sanityApiNavn = "fortsattInnvilgetSekundarlandStandard"
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
    },

    FORTSATT_INNVILGET_SEKUNDÆRLAND_ALENEANSVAR {
        override val sanityApiNavn = "fortsattInnvilgetSekundarlandAleneansvar"
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
    },

    FORTSATT_INNVILGET_SEKUNDÆRLAND_UK_STANDARD {
        override val sanityApiNavn = "fortsattInnvilgetSekundarlandUKStandard"
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
    },

    FORTSATT_INNVILGET_SEKUNDÆRLAND_UK_ALENEANSVAR {
        override val sanityApiNavn = "fortsattInnvilgetSekundarlandUKAleneansvar"
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
    },

    FORTSATT_INNVILGET_SEKUNDÆRLAND_UK_OG_UTLAND_STANDARD {
        override val sanityApiNavn = "fortsattInnvilgetSekundarlandUKOgUtlandStandard"
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
    },

    FORTSATT_INNVILGET_SEKUNDÆRLAND_TO_ARBEIDSLAND_NORGE_UTBETALER {
        override val sanityApiNavn = "fortsattInnvilgetSekundarlandToArbeidslandNorgeUtbetaler"
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
    },

    FORTSATT_INNVILGET_SEKUNDÆRLAND_UK_TO_ARBEIDSLAND_NORGE_UTBETALER {
        override val sanityApiNavn = "fortsattInnvilgetSekundarlandUKToArbeidslandNorgeUtbetaler"
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
    },

    FORTSATT_INNVILGET_SEKUNDÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE {
        override val sanityApiNavn = "fortsattInnvilgetSekundarlandBeggeForeldreBosattINorge"
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
    },

    FORTSATT_INNVILGET_SELVSTENDIG_RETT_PRIMÆRLAND_STANDARD {
        override val sanityApiNavn = "fortsattInnvilgetSelvstendigRettPrimarlandStandard"
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
    },

    FORTSATT_INNVILGET_SELVSTENDIG_RETT_PRIMÆRLAND_UK_STANDARD {
        override val sanityApiNavn = "fortsattInnvilgetSelvstendigRettPrimarlandUKStandard"
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
    },

    FORTSATT_INNVILGET_SELVSTENDIG_RETT_SEKUNDÆRLAND_STANDARD {
        override val sanityApiNavn = "fortsattInnvilgetSelvstendigRettSekundarlandStandard"
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
    },

    FORTSATT_INNVILGET_SELVSTENDIG_RETT_SEKUNDÆRLAND_UK_STANDARD {
        override val sanityApiNavn = "fortsattInnvilgetSelvstendigRettSekundarlandUKStandard"
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
    },

    FORTSATT_INNVILGET_SELVSTENDIG_RETT_PRIMÆRLAND_FÅR_YTELSE_I_UTLANDET {
        override val sanityApiNavn = "fortsattInnvilgetSelvstendigRettPrimarlandFaarYtelseIUtlandet"
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
    },

    FORTSATT_INNVILGET_SELVSTENDIG_RETT_SEKUNDÆRLAND_FÅR_YTELSE_I_UTLANDET {
        override val sanityApiNavn = "fortsattInnvilgetSelvstendigRettSekundarlandFaarYtelseIUtlandet"
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
    },

    FORTSATT_INNVILGET_PRIMÆRLAND_TO_ARBEIDSLAND_IKKE_KONTANTSTØTTE_I_ANNET_LAND {
        override val sanityApiNavn = "fortsattInnvilgetPrimarlandToArbeidslandIkkeKontantstotteIAnnetLand"
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
    },

    FORTSATT_INNVILGET_TILLEGGSBEGRUNNELSE_UTBETALING_TIL_ANNEN_FORELDER {
        override val sanityApiNavn = "fortsattInnvilgetTilleggsbegrunnelseUtbetalingTilAnnenForelder"
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
    },

    FORTSATT_INNVILGET_TILLEGGSBEGRUNNELSE_VEDTAK_FOR_SED {
        override val sanityApiNavn = "fortsattInnvilgetTilleggsbegrunnelseVedtakForSED"
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
    },

    FORTSATT_INNVILGET_TILLEGGSBEGRUNNELSE_NULLUTBETALING {
        override val sanityApiNavn = "fortsattInnvilgetTilleggstekstNullutbetaling"
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
    },

    FORTSATT_INNVILGET_TILLEGGSBEGRUNNELSE_SEKUNDÆR_IKKE_RETT_FULL_UTBETALING {
        override val sanityApiNavn = "fortsattInnvilgetTilleggstekstSekundarIkkeRettFullUtbetaling"
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
    },

    FORTSATT_INNVILGET_TILLEGGSBEGRUNNELSE_SEKUNDÆR_IKKE_FOTT_SVAR_PÅ_SED {
        override val sanityApiNavn = "fortsattInnvilgetTilleggstekstSekundarIkkeFottSvarPaaSED"
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
    },

    FORTSATT_INNVILGET_TILLEGGSBEGRUNNELSE_FULL_UTBETALING {
        override val sanityApiNavn = "fortsattInnvilgetTilleggstekstFullUtbetaling"
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
    },

    FORTSATT_INNVILGET_TILLEGGSBEGRUNNELSE_TO_ARBEIDSLAND_FULL_UTBETALING {
        override val sanityApiNavn = "fortsattInnvilgetTilleggstekstToArbeidslandFullUtbetaling"
        override val begrunnelseType = BegrunnelseType.FORTSATT_INNVILGET
    },

    AVSLAG_IKKE_EØS_BORGER {
        override val sanityApiNavn = "avslagIkkeEosBorger"
        override val begrunnelseType = BegrunnelseType.EØS_AVSLAG
    },

    AVSLAG_IKKE_BOSATT_I_EØS_LAND {
        override val sanityApiNavn = "avslagIkkeBosattIEosLand"
        override val begrunnelseType = BegrunnelseType.EØS_AVSLAG
    },

    AVSLAG_JOBBER_IKKE {
        override val sanityApiNavn = "avslagJobberIkke"
        override val begrunnelseType = BegrunnelseType.EØS_AVSLAG
    },

    AVSLAG_UTSENDT_ARBEIDSTAKER_FRA_ANNET_EØS_LAND {
        override val sanityApiNavn = "avslagUtsendtArbeidstakerFraAnnetEosLand"
        override val begrunnelseType = BegrunnelseType.EØS_AVSLAG
    },

    AVSLAG_ARBEIDER_MER_ENN_25_PROSENT_I_ANNET_EØS_LAND {
        override val sanityApiNavn = "avslagArbeiderMerEnn25ProsentIAnnetEosLand"
        override val begrunnelseType = BegrunnelseType.EØS_AVSLAG
    },

    AVSLAG_KUN_KORTE_USAMMENHENGENDE_ARBEIDSPERIODER {
        override val sanityApiNavn = "avslagKunKorteUsammenhengendeArbeidsperioder"
        override val begrunnelseType = BegrunnelseType.EØS_AVSLAG
    },

    AVSLAG_IKKE_PENGER_FRA_NAV_SOM_ERSTATTER_LØNN {
        override val sanityApiNavn = "avslagIkkePengerFraNAVSomErstatterLonn"
        override val begrunnelseType = BegrunnelseType.EØS_AVSLAG
    },

    AVSLAG_SEPARASJONSAVTALEN_GJELDER_IKKE {
        override val sanityApiNavn = "avslagSeparasjonsavtalenGjelderIkke"
        override val begrunnelseType = BegrunnelseType.EØS_AVSLAG
    },

    AVSLAG_IKKE_LOVLIG_OPPHOLD_SOM_EØS_BORGER {
        override val sanityApiNavn = "avslagIkkeLovligOppholdSomEosBorger"
        override val begrunnelseType = BegrunnelseType.EØS_AVSLAG
    },

    AVSLAG_IKKE_OPPHOLDSRETT_SOM_FAMILIEMEDLEM_AV_EØS_BORGER {
        override val sanityApiNavn = "avslagIkkeOppholdsrettSomFamiliemedlemAvEosBorger"
        override val begrunnelseType = BegrunnelseType.EØS_AVSLAG
    },

    AVSLAG_IKKE_STUDENT {
        override val sanityApiNavn = "avslagIkkeStudent"
        override val begrunnelseType = BegrunnelseType.EØS_AVSLAG
    },

    AVSLAG_IKKE_ANSVAR_FOR_BARN {
        override val sanityApiNavn = "avslagIkkeAnsvarForBarn"
        override val begrunnelseType = BegrunnelseType.EØS_AVSLAG
    },

    AVSLAG_VURDERING_IKKE_ANSVAR_FOR_BARN {
        override val sanityApiNavn = "avslagVurderingIkkeAnsvarForBarn"
        override val begrunnelseType = BegrunnelseType.EØS_AVSLAG
    },

    AVSLAG_BARN_UTEN_DNUMMER {
        override val sanityApiNavn = "avslagBarnUtenDNummer"
        override val begrunnelseType = BegrunnelseType.EØS_AVSLAG
    },

    AVSLAG_FOR_DAGPENGER_FRA_ANNET_EØS_LAND {
        override val sanityApiNavn = "avslagForDagpengerFraAnnetEosLand"
        override val begrunnelseType = BegrunnelseType.EØS_AVSLAG
    },

    AVSLAG_SELVSTENDIG_NÆRINGSDRIVENDE_NORGE_ARBEIDSTAKER_I_ANNET_EØS_LAND {
        override val sanityApiNavn = "avslagSelvstendigNaringsdrivendeNorgeArbeidstakerIAnnetEosLand"
        override val begrunnelseType = BegrunnelseType.EØS_AVSLAG
    },

    AVSLAG_SELVSTENDIG_RETT_STANDARD_AVSLAG {
        override val sanityApiNavn = "avslagSelvstendigRettStandardAvslag"
        override val begrunnelseType = BegrunnelseType.EØS_AVSLAG
    },

    AVSLAG_SELVSTENDIG_RETT_UTSENDET_ARBEIDSTAKER_FRA_ANNET_EØS_LAND {
        override val sanityApiNavn = "avslagSelvstendigRettUtsendtArbeidstakerFraAnnetEosLand"
        override val begrunnelseType = BegrunnelseType.EØS_AVSLAG
    },

    AVSLAG_SELVSTENDIG_RETT_BOR_IKKE_FAST_MED_BARNET {
        override val sanityApiNavn = "avslagSelvstendigRettBorIkkeFastMedBarnet"
        override val begrunnelseType = BegrunnelseType.EØS_AVSLAG
    },

    AVSLAG_SELVSTENDIG_RETT_FORELDRENE_BOR_SAMMEN {
        override val sanityApiNavn = "avslagSelvstendigRettForeldreneBorSammen"
        override val begrunnelseType = BegrunnelseType.EØS_AVSLAG
    },

    AVSLAG_DELT_BOSTED_BEGGE_FORELDRE_IKKE_OMFATTET_NORSK_LOVVALG {
        override val sanityApiNavn = "avslagDeltBostedBeggeForeldreIkkeOmfattetNorskLovvalg"
        override val begrunnelseType = BegrunnelseType.EØS_AVSLAG
    },

    ;

    @JsonValue
    override fun enumnavnTilString() = EØSBegrunnelse::class.simpleName + "$" + this.name
}
