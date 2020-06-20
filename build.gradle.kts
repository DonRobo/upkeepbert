plugins {
    kotlin("jvm") version "1.3.72"
    id("com.github.johnrengelman.shadow") version "6.0.0"
    application
}

group = "at.robbert"
version = "1.0-SNAPSHOT"

application.mainClassName = "at.robbert.upkeep.MainKt"

repositories {
    mavenCentral()
    maven("https://dl.bintray.com/kotlin/exposed/")
    jcenter()
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "at.robbert.upkeep.MainKt"
    }

//    // To add all of the dependencies otherwise a "NoClassDefFoundError" error
//    from(sourceSets.main.get().output)
//
//    dependsOn(configurations.runtimeClasspath)
//    from({
//        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
//    })
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.github.pengrad:java-telegram-bot-api:4.9.0")
    implementation("com.github.salomonbrys.kotson:kotson:2.5.0")
    implementation("com.google.code.gson:gson:2.8.6")
    implementation("org.jetbrains.exposed:exposed-core:0.25.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.25.1")
    implementation("org.jetbrains.exposed:exposed-java-time:0.25.1")
    implementation("org.postgresql:postgresql:42.2.14")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.3.7")
    implementation("org.slf4j:slf4j-api:1.7.30")
    implementation("org.slf4j:slf4j-log4j12:1.7.30")
    implementation("com.github.kittinunf.fuel:fuel:2.2.3")
    implementation("com.github.kittinunf.fuel:fuel-coroutines:2.2.3")
    implementation("org.apache.commons:commons-text:1.8")
}


tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}
