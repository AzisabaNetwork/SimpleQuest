plugins {
    kotlin("jvm") version libs.versions.kotlin.get()
    kotlin("plugin.serialization") version libs.versions.kotlin.get()
    alias(libs.plugins.ksp)
    alias(libs.plugins.shadow)
    java
}

group = "net.azisaba"
version = "1.0.0-SNAPSHOT"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

val integrationTest by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += output + compileClasspath
}

dependencies {
    // Paper
    compileOnly(libs.paper.api)

    // Exposed
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.migration)

    // Database
    implementation(libs.hikari)
    implementation(libs.mariadb)
    implementation(libs.flyway.core)
    implementation(libs.flyway.mysql)

    // Redis
    implementation(libs.lettuce.core)

    // YAML
    implementation(libs.kaml)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Coroutines
    implementation(libs.kotlinx.coroutines)

    // HTTP (Discord webhook)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)

    // Dagger
    implementation(libs.dagger)
    ksp(libs.dagger.compiler)
    implementation(libs.jakarta.inject)

    // Test
    testImplementation(libs.mockbukkit)
    testImplementation(libs.kotest.runner)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.kotest.framework)
    testImplementation(libs.h2)
    kspTest(libs.dagger.compiler)

    // Adventure (provided by Paper)
    compileOnly(libs.adventure.api)
    compileOnly(libs.adventure.minimessage)

    // GUI (compileOnly — must be installed on server)
    compileOnly(libs.kunectron)

    // Integration test
    add("integrationTestImplementation", libs.kotest.runner)
    add("integrationTestImplementation", libs.kotest.assertions)
    add("integrationTestImplementation", libs.kotest.framework)
    add("integrationTestImplementation", libs.mariadb)
    add("integrationTestImplementation", libs.lettuce.core)
    add("integrationTestImplementation", libs.h2)
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions.jvmTarget.set(
            org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21,
        )
    }

    shadowJar {
        archiveClassifier.set("")

        // Relocate dependencies to avoid conflicts with other plugins
        relocate("org.jetbrains.exposed", "net.azisaba.simplequest.shaded.exposed")
        relocate("com.charleskorn.kaml", "net.azisaba.simplequest.shaded.kaml")
        relocate("org.flywaydb", "net.azisaba.simplequest.shaded.flyway")
        relocate("io.ktor", "net.azisaba.simplequest.shaded.ktor")
        relocate("io.lettuce", "net.azisaba.simplequest.shaded.lettuce")

        // Merge META-INF/services files and relocate class names inside them.
        // Required for Exposed (DatabaseConnectionAutoRegistration) and Flyway (Java ServiceLoader).
        mergeServiceFiles()

        // Exclude signature files that cause issues in fat JARs
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    }

    build {
        dependsOn(shadowJar)
    }

    test {
        useJUnitPlatform()
    }

    val integrationTestTask by registering(Test::class) {
        description =
            "Runs integration tests (requires running Paper servers)"
        group = "verification"
        testClassesDirs = integrationTest.output.classesDirs
        classpath = integrationTest.runtimeClasspath

        // Only run in CI
        onlyIf { System.getenv("CI") == "true" }

        useJUnitPlatform()

        // Environment variables for server connectivity
        environment("MASTER_PORT", "25565")
        environment("SERVER_LOG", "servers/master/server.log")
        environment("MARIADB_URL", "jdbc:mariadb://localhost:3306/simplequest")
        environment("MARIADB_USER", "root")
        environment("MARIADB_PASSWORD", "test")
    }
}
