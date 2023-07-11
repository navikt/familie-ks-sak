import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion = "1.7.10"
    kotlin("jvm") version kotlinVersion

    id("org.springframework.boot") version "2.7.4"
    id("io.spring.dependency-management") version "1.0.13.RELEASE"
    id("org.jetbrains.kotlin.plugin.spring") version kotlinVersion
    id("org.jetbrains.kotlin.plugin.jpa") version kotlinVersion
    id("org.jetbrains.kotlin.plugin.allopen") version kotlinVersion
    id("com.github.davidmc24.gradle.plugin.avro") version "1.5.0"
}

group = "no.nav"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
    maven {
        url = uri("https://packages.confluent.io/maven")
    }
    maven {
        url = uri("https://maven.pkg.github.com/navikt/familie-felles")
        credentials {
            username = System.getenv("GITHUB_USERNAME")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {

    val springdocVersion = "1.6.13"
    val sentryVersion = "6.8.0"
    val navFellesVersion = "1.20220901103347_4819e55"
    val eksterneKontrakterBisysVersion = "2.0_20220609214258_f30c3ce"
    val fellesKontrakterVersion = "3.0_20230509152247_36d24db"
    val familieKontrakterSaksstatistikkVersion = "2.0_20220216121145_5a268ac"
    val familieKontrakterStønadsstatistikkKsVersion = "2.0_20230330120047_dfdd4f2"
    val familieKontrakterSkatteetatenVersion = "2.0_20210920094114_9c74239"
    val tokenValidationSpringVersion = "2.1.8"
    val navFoedselsnummerVersion = "1.0-SNAPSHOT.6"
    val prosesseringVersion = "1.20221110194901_e9e0d90"
    val restAssuredVersion = "5.3.0"
    val kotlinxVersion = "1.6.4"

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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxVersion")

    // ---------- DB ---------- \\
    implementation("org.flywaydb:flyway-core")
    implementation("org.postgresql:postgresql:42.5.1")

    // ---------- Apache ---------- \\
    implementation("org.apache.httpcomponents:httpclient:4.5.13")
    implementation("org.apache.httpcomponents:httpcore:4.4.15")

    // ----------- AVRO ---------\\
    implementation("org.apache.avro:avro:1.11.1")
    implementation("io.confluent:kafka-avro-serializer:7.4.0")

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
    implementation("no.nav.familie.kontrakter:kontantstotte:$fellesKontrakterVersion")
    implementation("no.nav.familie.eksterne.kontrakter:bisys:$eksterneKontrakterBisysVersion")
    implementation("no.nav.familie.eksterne.kontrakter:stonadsstatistikk-ks:$familieKontrakterStønadsstatistikkKsVersion")
    implementation("no.nav.familie.eksterne.kontrakter:saksstatistikk:$familieKontrakterSaksstatistikkVersion")
    implementation("no.nav.familie.eksterne.kontrakter:skatteetaten:$familieKontrakterSkatteetatenVersion")
    implementation("no.nav.security:token-client-spring:$tokenValidationSpringVersion")
    implementation("no.nav.familie:prosessering-core:$prosesseringVersion")
    implementation("nav-foedselsnummer:core:$navFoedselsnummerVersion")

    implementation("com.papertrailapp:logback-syslog4j:1.0.0")
    implementation("io.getunleash:unleash-client-java:6.1.0")
    implementation("io.sentry:sentry-spring-boot-starter:$sentryVersion")
    implementation("io.sentry:sentry-logback:$sentryVersion")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.0")
    implementation("com.pinterest:ktlint:0.47.1")
    implementation("com.neovisionaries:nv-i18n:1.29")

    testImplementation("io.mockk:mockk:1.13.2")
    testImplementation("com.ninja-squad:springmockk:3.1.1") {
        exclude(module = "mockito-core")
    }
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.cloud:spring-cloud-contract-wiremock:3.1.5")
    testImplementation("io.rest-assured:spring-mock-mvc:$restAssuredVersion")
    testImplementation("io.rest-assured:kotlin-extensions:$restAssuredVersion")
    testImplementation("org.testcontainers:postgresql:1.17.6")
    testImplementation("no.nav.security:mock-oauth2-server:0.5.6")
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
