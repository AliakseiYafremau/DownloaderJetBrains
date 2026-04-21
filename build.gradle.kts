plugins {
    kotlin("jvm") version "2.1.21"
    application
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.mockito:mockito-core:5.2.0")
}

application {
    mainClass.set("downloader.MainKt")
}

sourceSets {
    named("main") {
        java.setSrcDirs(listOf("src/downloader"))
    }
    named("test") {
        java.setSrcDirs(listOf("src/test/kotlin"))
        resources.setSrcDirs(listOf("src/test/resources"))
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<Test>("unit") {
    description = "Runs unit tests only."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath

    useJUnitPlatform {
        excludeTags("e2e")
    }
}

tasks.register<Test>("e2e") {
    description = "Runs end-to-end tests."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath

    val configuredE2eFileSizeMb = (project.findProperty("sizeMb") as String?)
        ?: (project.findProperty("e2eSizeMb") as String?) // Backward-compatible alias.
        ?: (project.findProperty("e2eFileSizeMb") as String?) // Backward-compatible alias.
        ?: System.getProperty("e2e.file.size.mb")
        ?: "50"
    systemProperty("e2e.file.size.mb", configuredE2eFileSizeMb)
    val e2eTempDir = layout.buildDirectory.dir("tmp/e2e-jvm").get().asFile
    e2eTempDir.mkdirs()
    systemProperty("java.io.tmpdir", e2eTempDir.absolutePath)

    useJUnitPlatform {
        includeTags("e2e")
    }
    shouldRunAfter(tasks.test)
}
