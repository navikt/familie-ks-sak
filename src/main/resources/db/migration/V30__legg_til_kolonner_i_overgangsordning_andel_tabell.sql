ALTER TABLE overgangsordning_andel
    ADD COLUMN antall_timer NUMERIC,
    ADD COLUMN delt_bosted BOOLEAN,
    DROP COLUMN IF EXISTS prosent,
    ADD CONSTRAINT overgangsordning_andel_delt_bosted_false_eller_antall_timer_0 CHECK (delt_bosted = FALSE OR antall_timer = 0);
