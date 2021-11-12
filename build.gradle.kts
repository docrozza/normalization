val kotlinVersion = "1.5.31"
val sdVersion = "7.7.2"
val jupiterVersion = "5.8.1"

repositories {
    mavenCentral()
    maven("https://maven.stardog.com")
}

plugins {
    kotlin("jvm") version "1.5.31"
    idea
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8", kotlinVersion))

    api("com.complexible.stardog:client-http:$sdVersion")

    testApi("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
    testApi("org.junit.jupiter:junit-jupiter-api:$jupiterVersion")
    testApi("org.junit.jupiter:junit-jupiter-params:$jupiterVersion")

    testApi("com.complexible.stardog:client-embedded:$sdVersion")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$jupiterVersion")
}

tasks.compileKotlin {
    kotlinOptions {
        jvmTarget = "1.8"
        javaParameters = true
    }
}

tasks {
    test {
        useJUnitPlatform()

        val stardogHome: String? by project
        val stardogLibs: String? by project

        stardogHome?.also { systemProperty("stardog.home", it) }
        stardogLibs?.also { systemProperty("java.library.path", it) }
    }

    java {
        withSourcesJar()
    }
}