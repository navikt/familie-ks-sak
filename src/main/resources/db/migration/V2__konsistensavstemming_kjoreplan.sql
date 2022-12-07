CREATE TABLE konsistensavstemming_kjoreplan
(
    id        bigint                                                   NOT NULL,
    kjoredato timestamp(3) without time zone                           NOT NULL,
    status    character varying(50) DEFAULT 'LEDIG'::character varying NOT NULL
);

CREATE SEQUENCE konsistensavstemming_kjoreplan_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


INSERT INTO konsistensavstemming_kjoreplan (id, kjoredato)
VALUES (nextval('konsistensavstemming_kjoreplan_seq'), TO_TIMESTAMP('05-01-2023', 'DD-MM-YYYY SS:MS')),
       (nextval('konsistensavstemming_kjoreplan_seq'), TO_TIMESTAMP('30-01-2023', 'DD-MM-YYYY SS:MS')),
       (nextval('konsistensavstemming_kjoreplan_seq'), TO_TIMESTAMP('27-02-2023', 'DD-MM-YYYY SS:MS')),
       (nextval('konsistensavstemming_kjoreplan_seq'), TO_TIMESTAMP('28-03-2023', 'DD-MM-YYYY SS:MS')),
       (nextval('konsistensavstemming_kjoreplan_seq'), TO_TIMESTAMP('25-04-2023', 'DD-MM-YYYY SS:MS')),
       (nextval('konsistensavstemming_kjoreplan_seq'), TO_TIMESTAMP('30-05-2023', 'DD-MM-YYYY SS:MS')),
       (nextval('konsistensavstemming_kjoreplan_seq'), TO_TIMESTAMP('29-06-2023', 'DD-MM-YYYY SS:MS')),
       (nextval('konsistensavstemming_kjoreplan_seq'), TO_TIMESTAMP('28-07-2023', 'DD-MM-YYYY SS:MS')),
       (nextval('konsistensavstemming_kjoreplan_seq'), TO_TIMESTAMP('30-08-2023', 'DD-MM-YYYY SS:MS')),
       (nextval('konsistensavstemming_kjoreplan_seq'), TO_TIMESTAMP('29-09-2023', 'DD-MM-YYYY SS:MS')),
       (nextval('konsistensavstemming_kjoreplan_seq'), TO_TIMESTAMP('30-10-2023', 'DD-MM-YYYY SS:MS')),
       (nextval('konsistensavstemming_kjoreplan_seq'), TO_TIMESTAMP('22-11-2023', 'DD-MM-YYYY SS:MS'));
