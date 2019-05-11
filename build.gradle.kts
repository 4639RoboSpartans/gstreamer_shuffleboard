
import com.diffplug.spotless.extra.wtp.EclipseWtpFormatterStep
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm") version "1.3.31"
    id("org.openjfx.javafxplugin") version "0.0.7"
    id("com.diffplug.gradle.spotless") version "3.23.0"
    id("com.github.johnrengelman.shadow") version "5.0.0"
}

version = "0.0.0-beta"

repositories {
    mavenCentral()
    maven {
        setUrl("https://first.wpi.edu/FRC/roborio/maven/release/")
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    compileOnly("edu.wpi.first.shuffleboard", "api", "2019.4.1")
    compileOnly("edu.wpi.first.shuffleboard.plugin", "networktables", "2019.4.1")

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
    format("xml") {
        target(fileTree(".") {
            include("**/*.xml", "**/*.fxml")
            exclude("**/build/**")
        })
        eclipseWtp(EclipseWtpFormatterStep.XML)
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
tasks.withType<Wrapper> {
    gradleVersion = "5.4.1"
}
application {
    mainClassName = "A"
}
tasks.withType<ShadowJar> {
    archiveClassifier.set("")
    dependencies {
        exclude(dependency("org.openjfx::"))
    }
}
