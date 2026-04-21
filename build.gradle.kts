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

    // Test infrastructure only; test classes will be added later.
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
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

