import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion = "1.9.24"
    kotlin("jvm") version kotlinVersion

    id("org.springframework.boot") version "3.3.1"
    id("io.spring.dependency-management") version "1.1.5"
    id("org.jetbrains.kotlin.plugin.spring") version kotlinVersion
    id("org.jetbrains.kotlin.plugin.jpa") version kotlinVersion
    id("org.jetbrains.kotlin.plugin.allopen") version kotlinVersion
    id("org.jetbrains.kotlin.plugin.noarg") version kotlinVersion
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"

    // ------------- SLSA -------------- //
    id("org.cyclonedx.bom") version "1.9.0"
}

configurations {
    implementation.configure {
        exclude(module = "spring-boot-starter-tomcat")
        exclude("org.apache.tomcat")
    }
}

group = "no.nav"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_21
val ktlint by configurations.creating

repositories {
    mavenCentral()
    mavenLocal()
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

    val springdocVersion = "2.6.0"
    val sentryVersion = "7.12.1"
    val navFellesVersion = "3.20240715145751_fc025c1"
    val eksterneKontrakterBisysVersion = "2.0_20230214104704_706e9c0"
    val fellesKontrakterVersion = "3.0_20240722091153_6e40a2c"
    val familieKontrakterSaksstatistikkVersion = "2.0_20230214104704_706e9c0"
    val familieKontrakterStønadsstatistikkKsVersion = "2.0_20240131125409_e3d0f6d"
    val tokenValidationSpringVersion = "5.0.1"
    val navFoedselsnummerVersion = "1.0-SNAPSHOT.6"
    val prosesseringVersion = "2.20240603145215_c56e179"
    val restAssuredVersion = "5.5.0"
    val kotlinxVersion = "1.8.1"
    val utbetalingsgeneratorVersion = "1.0_20240729145155_57174aa"

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
    implementation("org.postgresql:postgresql:42.7.3")

    // ---------- Apache ---------- \\
    implementation("org.apache.httpcomponents:httpclient:4.5.14")
    implementation("org.apache.httpcomponents:httpcore:4.4.16")

    // ----------- AVRO ---------\\
    implementation("org.apache.avro:avro:1.11.3")
    implementation("io.confluent:kafka-avro-serializer:7.7.0")
    implementation("org.eclipse.jetty:jetty-server")

    // ---- Junit og Cucumber ---- \\

    testImplementation(platform("org.junit:junit-bom:5.10.3"))
    testImplementation(platform("io.cucumber:cucumber-bom:7.18.0"))

    testImplementation("io.cucumber:cucumber-java")
    testImplementation("io.cucumber:cucumber-junit-platform-engine")
    testImplementation("org.junit.platform:junit-platform-suite")
    testImplementation("org.junit.jupiter:junit-jupiter")

    // ---------- NAV ---------- \\
    implementation("no.nav.familie.felles:sikkerhet:$navFellesVersion")
    implementation("no.nav.familie.felles:log:$navFellesVersion")
    implementation("no.nav.familie.felles:unleash:$navFellesVersion")
    implementation("no.nav.familie.felles:leader:$navFellesVersion")
    implementation("no.nav.familie.felles:http-client:$navFellesVersion")
    implementation("no.nav.familie.felles:modell:$navFellesVersion")
    implementation("no.nav.familie.felles:familie-utbetalingsgenerator:$utbetalingsgeneratorVersion")

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
    implementation("io.getunleash:unleash-client-java:9.2.2")
    implementation("io.sentry:sentry-spring-boot-starter-jakarta:$sentryVersion")
    implementation("io.sentry:sentry-logback:$sentryVersion")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.neovisionaries:nv-i18n:1.29")
    implementation("com.github.jsqlparser:jsqlparser:4.9")

    ktlint("com.pinterest.ktlint:ktlint-cli:1.2.1") {
        attributes {
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
        }
    }

    testImplementation("io.mockk:mockk:1.13.11")
    testImplementation("com.ninja-squad:springmockk:4.0.2") {
        exclude(module = "mockito-core")
    }
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.cloud:spring-cloud-contract-wiremock:4.1.3")
    testImplementation("io.rest-assured:spring-mock-mvc:$restAssuredVersion")
    testImplementation("io.rest-assured:kotlin-extensions:$restAssuredVersion")
    testImplementation("org.testcontainers:postgresql:1.20.0")
    testImplementation("no.nav.security:mock-oauth2-server:2.1.8")
    testImplementation("no.nav.security:token-validation-test-support:2.0.5")
    testImplementation("no.nav.security:token-validation-spring-test:$tokenValidationSpringVersion")
    testImplementation("nav-foedselsnummer:testutils:1.0-SNAPSHOT.6")

    runtimeOnly("org.flywaydb:flyway-database-postgresql")
}

sourceSets.getByName("test") {
    java.srcDir("src/test/enhetstester/kotlin")
    java.srcDir("src/test/integrasjonstester/kotlin")
    java.srcDir("src/test/common/")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "21"
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

tasks.cyclonedxBom {
    setIncludeConfigs(listOf("runtimeClasspath"))
    setSkipConfigs(listOf("compileClasspath", "testCompileClasspath"))
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
            val test =
                "test"(Test::class) {
                    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
                    useJUnitPlatform {
                        excludeTags("integrationTest")
                    }
                }
            val integrationTest =
                register<Test>("integrationTest") {
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
