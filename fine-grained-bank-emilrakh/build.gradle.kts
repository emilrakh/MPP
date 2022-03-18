plugins {
    kotlin("jvm") version "1.5.0"
    application
}

group = "ru.ifmo.mpp"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.6.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.6.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.6.0")
    testImplementation("org.jetbrains.kotlinx:lincheck:2.14.1")
}

sourceSets["main"].java.setSrcDirs(listOf("src"))
sourceSets["test"].java.setSrcDirs(listOf("test"))

