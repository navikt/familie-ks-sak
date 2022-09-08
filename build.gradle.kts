import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.7.3"
    id("io.spring.dependency-management") version "1.0.13.RELEASE"
    kotlin("jvm") version "1.6.21"
    kotlin("plugin.spring") version "1.6.21"
    id("org.jlleitschuh.gradle.ktlint") version "11.0.0"
}

group = "no.nav"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/navikt/familie-felles")
        credentials {
            username = System.getenv("GITHUB_USERNAME")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {

    val springdocVersion = "1.6.11"
    val sentryVersion = "6.4.1"
    val navFellesVersion = "1.20220901103347_4819e55"
    val eksterneKontrakterBisysVersion = "2.0_20220609214258_f30c3ce"
    val fellesKontrakterVersion = "2.0_20220831094750_9782fd7"
    val familieKontrakterSaksstatistikkVersion = "2.0_20220216121145_5a268ac"
    val familieKontrakterStønadsstatistikkVersion = "2.0_20220905083828_e74ee8a"
    val familieKontrakterSkatteetatenVersion = "2.0_20210920094114_9c74239"
    val tokenValidationSpringVersion = "2.1.4"
    val navFoedselsnummerVersion = "1.0-SNAPSHOT.6"
    val prosesseringVersion = "1.20220624132237_7f5ba9c"

    // ---------- Spring ---------- \\
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-jetty")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-devtools")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.springframework.retry:spring-retry")

    // ---------- Spring ---------- \\
    implementation("org.springdoc:springdoc-openapi-ui:$springdocVersion")
    implementation("org.springdoc:springdoc-openapi-kotlin:$springdocVersion")

    // ---------- Spring ---------- \\
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // ---------- Spring ---------- \\
    implementation("io.micrometer:micrometer-registry-prometheus")

    // ---------- Serialisering/Deserialisering ---------- \\
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.4")
    implementation("org.flywaydb:flyway-core")

    // ---------- NAV ---------- \\
    implementation("no.nav.familie.felles:sikkerhet:$navFellesVersion")
    implementation("no.nav.familie.felles:log:$navFellesVersion")
    implementation("no.nav.familie.felles:leader:$navFellesVersion")
    implementation("no.nav.familie.felles:http-client:$navFellesVersion")
    implementation("no.nav.familie.felles:modell:$navFellesVersion")
    implementation("no.nav.familie.felles:util:$navFellesVersion")
    implementation("no.nav.familie.felles:valutakurs-klient:$navFellesVersion")
    implementation("no.nav.familie.kontrakter:felles:$fellesKontrakterVersion")
    implementation("no.nav.familie.kontrakter:barnetrygd:$fellesKontrakterVersion")
    implementation("no.nav.familie.eksterne.kontrakter:bisys:$eksterneKontrakterBisysVersion")
    implementation("no.nav.familie.eksterne.kontrakter:stonadsstatistikk:$familieKontrakterStønadsstatistikkVersion")
    implementation("no.nav.familie.eksterne.kontrakter:saksstatistikk:$familieKontrakterSaksstatistikkVersion")
    implementation("no.nav.familie.eksterne.kontrakter:skatteetaten:$familieKontrakterSkatteetatenVersion")
    implementation("no.nav.security:token-client-spring:$tokenValidationSpringVersion")
    implementation("no.nav.familie:prosessering-jdbc:$prosesseringVersion")
    implementation("nav-foedselsnummer:core:$navFoedselsnummerVersion")

    implementation("com.papertrailapp:logback-syslog4j:1.0.0")
    implementation("org.postgresql:postgresql:42.5.0")
    implementation("org.apache.httpcomponents:httpclient:4.5.13")
    implementation("org.apache.httpcomponents:httpcore:4.4.15")
    implementation("no.finn.unleash:unleash-client-java:4.4.1")
    implementation("io.sentry:sentry-spring-boot-starter:$sentryVersion")
    implementation("io.sentry:sentry-logback:$sentryVersion")

    implementation("com.pinterest:ktlint:0.47.0")

    testImplementation("io.mockk:mockk:1.12.7")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.cloud:spring-cloud-contract-wiremock:3.1.3")
    testImplementation("org.testcontainers:postgresql:1.17.3")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks {
    bootJar {
        archiveFileName.set("familie-ks-sak.jar")
    }
    test {
        if (project.hasProperty("github")) {
            if (!project.hasProperty("enhetstester")) {
                exclude("no/nav/familie/ks/sak/enhetstester/**")
            }
            if (!project.hasProperty("integrasjonstester")) {
                exclude("no/nav/familie/ks/sak/integrasjonstester/**")
            }
            if (!project.hasProperty("verdikjedetester")) {
                exclude("no/nav/familie/ks/sak/verdikjedetester/**")
            }
        }
    }
}
