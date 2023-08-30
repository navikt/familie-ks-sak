package no.nav.familie.ks.sak.config

enum class SpringProfile(val navn: String) {
    DevPostgresPreprod("dev-postgres-preprod"),
    Integrasjonstest("integrasjonstest"),
    Prod("prod"),
    Preprod("preprod"),
    Dev("dev"),
    Postgres("postgres"),
}
