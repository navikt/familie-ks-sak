import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion = "1.9.20"
    kotlin("jvm") version kotlinVersion

    id("org.springframework.boot") version "3.1.5"
    id("io.spring.dependency-management") version "1.1.4"
    id("org.jetbrains.kotlin.plugin.spring") version kotlinVersion
    id("org.jetbrains.kotlin.plugin.jpa") version kotlinVersion
    id("org.jetbrains.kotlin.plugin.allopen") version kotlinVersion
    id("org.jetbrains.kotlin.plugin.noarg") version kotlinVersion
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"

    // ------------- SLSA -------------- //
    id("org.cyclonedx.bom") version "1.8.1"
}

configurations {
    implementation.configure {
        exclude(module = "spring-boot-starter-tomcat")
        exclude("org.apache.tomcat")
    }
}

group = "no.nav"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17
val ktlint by configurations.creating

repositories {
    mavenCentral()
    maven {
        url = uri("https://packages.confluent.io/maven")
    }
    maven {
        url = uri("https://maven.pkg.github.com/navikt/maven-release")
        credentials {
            username = System.getenv("GITHUB_USERNAME")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {

    val springdocVersion = "2.2.0"
    val sentryVersion = "6.33.1"
    val navFellesVersion = "2.20231023162434_fa320ce"
    val eksterneKontrakterBisysVersion = "2.0_20230214104704_706e9c0"
    val fellesKontrakterVersion = "3.0_20231101161029_4d64011"
    val familieKontrakterSaksstatistikkVersion = "2.0_20230214104704_706e9c0"
    val familieKontrakterStønadsstatistikkKsVersion = "2.0_20230825103733_1ac52c2"
    val familieKontrakterSkatteetatenVersion = "2.0_20230214104704_706e9c0"
    val tokenValidationSpringVersion = "3.1.8"
    val navFoedselsnummerVersion = "1.0-SNAPSHOT.6"
    val prosesseringVersion = "2.20231026103714_2f17845"
    val restAssuredVersion = "5.3.2"
    val kotlinxVersion = "1.7.3"

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
    implementation("org.springdoc:springdoc-openapi-starter-common:$springdocVersion")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocVersion")

    // ---------- Kotlin ---------- \\
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxVersion")

    // ---------- DB ---------- \\
    implementation("org.flywaydb:flyway-core")
    implementation("org.postgresql:postgresql:42.6.0")

    // ---------- Apache ---------- \\
    implementation("org.apache.httpcomponents:httpclient:4.5.14")
    implementation("org.apache.httpcomponents:httpcore:4.4.16")

    // ----------- AVRO ---------\\
    implementation("org.apache.avro:avro:1.11.3")
    implementation("io.confluent:kafka-avro-serializer:7.5.2")
    implementation("org.eclipse.jetty:jetty-server")

    // ---------- NAV ---------- \\
    implementation("no.nav.familie.felles:sikkerhet:$navFellesVersion")
    implementation("no.nav.familie.felles:log:$navFellesVersion")
    implementation("no.nav.familie.felles:unleash:$navFellesVersion")
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
    implementation("no.nav.security:token-client-spring:$tokenValidationSpringVersion")
    implementation("no.nav.familie:prosessering-core:$prosesseringVersion")
    implementation("nav-foedselsnummer:core:$navFoedselsnummerVersion")

    implementation("com.papertrailapp:logback-syslog4j:1.0.0")
    implementation("io.getunleash:unleash-client-java:8.4.0")
    implementation("io.sentry:sentry-spring-boot-starter:$sentryVersion")
    implementation("io.sentry:sentry-logback:$sentryVersion")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.3")
    implementation("com.neovisionaries:nv-i18n:1.29")
    ktlint("com.pinterest.ktlint:ktlint-cli:1.0.1") {
        attributes {
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
        }
    }

    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("com.ninja-squad:springmockk:4.0.2") {
        exclude(module = "mockito-core")
    }
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.cloud:spring-cloud-contract-wiremock:4.0.4")
    testImplementation("io.rest-assured:spring-mock-mvc:$restAssuredVersion")
    testImplementation("io.rest-assured:kotlin-extensions:$restAssuredVersion")
    testImplementation("org.testcontainers:postgresql:1.19.1")
    testImplementation("no.nav.security:mock-oauth2-server:2.0.1")
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

val ktlintCheck by tasks.registering(JavaExec::class) {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Check Kotlin code style"
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    // see https://pinterest.github.io/ktlint/install/cli/#command-line-usage for more information
    args(
        "src/**/*.kt",
    )
}

tasks.check {
    dependsOn(ktlintCheck)
}

tasks.register<JavaExec>("ktlintFormat") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Check Kotlin code style and format"
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
    // see https://pinterest.github.io/ktlint/install/cli/#command-line-usage for more information
    args(
        "-F",
        "src/**/*.kt",
    )
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
