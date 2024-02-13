UPDATE ENDRET_UTBETALING_ANDEL
SET vedtak_begrunnelse_spesifikasjoner =
        CASE
            WHEN vedtak_begrunnelse_spesifikasjoner = '' THEN ''
            WHEN vedtak_begrunnelse_spesifikasjoner LIKE 'NasjonalEllerFellesBegrunnelse$%'
                THEN vedtak_begrunnelse_spesifikasjoner
            ELSE CONCAT('NasjonalEllerFellesBegrunnelse$',
                        REPLACE(vedtak_begrunnelse_spesifikasjoner, ';', ';NasjonalEllerFellesBegrunnelse$'))
            END