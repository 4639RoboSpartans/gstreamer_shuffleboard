
import com.diffplug.spotless.extra.wtp.EclipseWtpFormatterStep
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.jvm.tasks.Jar

plugins {
    kotlin("jvm") version "1.3.31"
    id("org.openjfx.javafxplugin") version "0.0.8"
    id("com.diffplug.gradle.spotless") version "3.25.0"
}

version = "0.1.1-beta"

repositories {
    mavenCentral()
    maven {
        setUrl("https://first.wpi.edu/FRC/roborio/maven/release/")
    }
}

dependencies {
    compileOnly("edu.wpi.first.shuffleboard", "api", "2019.4.1")
    compileOnly("edu.wpi.first.shuffleboard.plugin", "networktables", "2019.4.1")

    implementation(kotlin("stdlib-jdk8"))
    implementation("net.java.dev.jna", "jna-platform", "5.3.1")
    implementation("org.freedesktop.gstreamer", "gst1-java-core", "1.0.0")
    implementation("org.apache.commons", "commons-lang3", "3.9")
    testImplementation("org.junit.jupiter", "junit-jupiter", "5.4.2")
}

javafx {
    modules("javafx.controls", "javafx.fxml", "javafx.controls", "javafx.swing")
    configuration = "compileOnly"
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
            include("**/*.fxml")
            exclude("**/build/**")
        })
        eclipseWtp(EclipseWtpFormatterStep.XML)
    }
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_11
}

val fatJar = task<Jar>("fatJar") {
    group = "build"
    duplicatesStrategy = DuplicatesStrategy.FAIL
    exclude("META-INF/*")
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get() as CopySpec)
}

tasks {
    withType<KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = "11"
    }
    withType<Test>().configureEach {
        useJUnitPlatform()
    }
    withType<Wrapper>().configureEach {
        gradleVersion = "5.5.1"
    }
}
