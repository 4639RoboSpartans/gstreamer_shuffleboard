import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.3.31"
    id("org.openjfx.javafxplugin") version "0.0.7"
    id("com.diffplug.gradle.spotless") version "3.14.0"
}

version = "0.0.0"

repositories {
    mavenCentral()
    maven {
        setUrl("https://first.wpi.edu/FRC/roborio/maven/release/")
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("edu.wpi.first.shuffleboard", "api", "2019.4.1")
    implementation("edu.wpi.first.shuffleboard.plugin", "networktables", "2019.4.1")

    implementation("net.java.dev.jna", "jna-platform", "5.3.1")
    implementation("org.freedesktop.gstreamer", "gst1-java-core", "1.0.0")

    testImplementation("org.junit.jupiter", "junit-jupiter", "5.4.2")
}

javafx {
    modules("javafx.controls", "javafx.fxml", "javafx.controls", "javafx.swing")
}

spotless {
    kotlin {
        ktlint()
        trimTrailingWhitespace()
        indentWithSpaces()
        endWithNewline()
    }
    kotlinGradle {
        ktlint()
        trimTrailingWhitespace()
        indentWithSpaces()
        endWithNewline()
    }
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_11
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}
tasks.withType<Test> {
    useJUnitPlatform()
}
