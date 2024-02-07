UPDATE vilkar_resultat
SET vedtak_begrunnelse_spesifikasjoner = concat('NasjonalEllerFellesBegrunnelse$', vedtak_begrunnelse_spesifikasjoner)
WHERE vedtak_begrunnelse_spesifikasjoner <> '';

UPDATE vilkar_resultat
SET vedtak_begrunnelse_spesifikasjoner = replace(vedtak_begrunnelse_spesifikasjoner, ';', ';NasjonalEllerFellesBegrunnelse$')
WHERE vedtak_begrunnelse_spesifikasjoner like '%;%';