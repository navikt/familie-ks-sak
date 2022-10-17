import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion = "1.7.10"
    kotlin("jvm") version kotlinVersion

    id("org.springframework.boot") version "2.7.4"
    id("io.spring.dependency-management") version "1.0.13.RELEASE"
    id("org.jetbrains.kotlin.plugin.spring") version kotlinVersion
    id("org.jetbrains.kotlin.plugin.jpa") version kotlinVersion
    id("org.jetbrains.kotlin.plugin.allopen") version kotlinVersion
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
    val sentryVersion = "6.4.2"
    val navFellesVersion = "1.20220901103347_4819e55"
    val eksterneKontrakterBisysVersion = "2.0_20220609214258_f30c3ce"
    val fellesKontrakterVersion = "2.0_20220831094750_9782fd7"
    val familieKontrakterSaksstatistikkVersion = "2.0_20220216121145_5a268ac"
    val familieKontrakterStønadsstatistikkVersion = "2.0_20220905083828_e74ee8a"
    val familieKontrakterSkatteetatenVersion = "2.0_20210920094114_9c74239"
    val tokenValidationSpringVersion = "2.1.4"
    val navFoedselsnummerVersion = "1.0-SNAPSHOT.6"
    val prosesseringVersion = "1.20220624132237_7f5ba9c"
    val restAssuredVersion = "5.2.0"
    val fpsakTidslinje = "2.6.5"

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
    implementation("org.springdoc:springdoc-openapi-ui:$springdocVersion")
    implementation("org.springdoc:springdoc-openapi-kotlin:$springdocVersion")

    // ---------- Kotlin ---------- \\
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // ---------- DB ---------- \\
    implementation("org.flywaydb:flyway-core")
    implementation("org.postgresql:postgresql:42.5.0")

    // ---------- Apache ---------- \\
    implementation("org.apache.httpcomponents:httpclient:4.5.13")
    implementation("org.apache.httpcomponents:httpcore:4.4.15")

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
    implementation("no.nav.fpsak.tidsserie:fpsak-tidsserie:$fpsakTidslinje")

    implementation("com.papertrailapp:logback-syslog4j:1.0.0")
    implementation("io.getunleash:unleash-client-java:6.0.1")
    implementation("io.sentry:sentry-spring-boot-starter:$sentryVersion")
    implementation("io.sentry:sentry-logback:$sentryVersion")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.4")
    implementation("com.pinterest:ktlint:0.47.1")

    testImplementation("io.mockk:mockk:1.12.8")
    testImplementation("com.ninja-squad:springmockk:3.1.1") {
        exclude(module = "mockito-core")
    }
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.cloud:spring-cloud-contract-wiremock:3.1.4")
    testImplementation("io.rest-assured:spring-mock-mvc:$restAssuredVersion")
    testImplementation("io.rest-assured:kotlin-extensions:$restAssuredVersion")
    testImplementation("org.testcontainers:postgresql:1.17.3")
    testImplementation("no.nav.security:mock-oauth2-server:0.5.3")
    testImplementation("no.nav.security:token-validation-test-support:2.0.5")
    testImplementation("no.nav.security:token-validation-spring-test:$tokenValidationSpringVersion")
    testImplementation("nav-foedselsnummer:testutils:1.0-SNAPSHOT.6")
}

sourceSets.getByName("test") {
    java.srcDir("src/test/enhetstester/kotlin")
    java.srcDir("src/test/integrasjonstester/kotlin")
    java.srcDir("src/test/common/")
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
}

allprojects {
    plugins.withId("java") {
        this@allprojects.tasks {
            val test = "test"(Test::class) {
                maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
                useJUnitPlatform {
                    excludeTags("integrationTest")
                }
            }
            val integrationTest = register<Test>("integrationTest") {
                useJUnitPlatform {
                    includeTags("integrationTest")
                }
                shouldRunAfter(test)
            }
            "check" {
                dependsOn(integrationTest)
            }
        }
    }
}
