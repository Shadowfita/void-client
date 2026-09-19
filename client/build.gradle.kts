import proguard.gradle.ProGuardTask

plugins {
    application
    id("com.gradleup.shadow") version "8.3.10"
}

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.guardsquare:proguard-gradle:7.7.0")
    }
}

group = "world.gregs.void"
version = "0.3.0"

repositories {
    mavenCentral()
    maven("https://repo.runelite.net")
}

dependencies {
    implementation("org.apache.commons:commons-csv:1.9.0")
    implementation("org.apache.commons:commons-lang3:3.0")
    implementation("org.apache.commons:commons-text:1.9")
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("com.google.guava:guava:31.0.1-jre")
    implementation("com.google.inject:guice:5.1.0")
    implementation("net.sf.jopt-simple:jopt-simple:6.0-alpha-3")
    implementation("ch.qos.logback:logback-classic:1.2.10")
    implementation("com.formdev:flatlaf:3.2.5")
    implementation("org.slf4j:slf4j-api:1.7.7")
    implementation("com.google.code.findbugs:jsr305:3.0.2")

    implementation(files(
        "../libs/clientlibs.jar",
        "../libs/graphics.jar",
        "../libs/trident-1.5.00.jar"
        ))

    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
}

java {
    sourceSets {
        test { java.setSrcDirs(listOf("test")) }
        main {
            java.srcDirs("src")
            resources.srcDirs("resources", "src")
            resources.exclude("**/*.java")
        }
    }
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }

    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

application {
    mainClass = "Loader"
}

tasks.shadowJar {
    archiveBaseName.set("void-client")
    archiveClassifier.set("")
}

val proguardJar by tasks.registering(ProGuardTask::class) {
    dependsOn(tasks.shadowJar)

    val inputJar = tasks.shadowJar.flatMap { it.archiveFile }
    val outputFile = layout.buildDirectory.file("libs/void-client-$version-release.jar")

    injars(inputJar)
    outjars(outputFile)

    val javaHome = System.getProperty("java.home")
    if (file("$javaHome/jmods").isDirectory) {
        libraryjars(mapOf("jarfilter" to "!**.jar", "filter" to "!module-info.class"), "$javaHome/jmods")
    } else {
        libraryjars("$javaHome/lib/rt.jar")
        libraryjars(fileTree("$javaHome/lib/ext") { include("*.jar") })
    }

    configuration("proguard-rules.pro")

    printmapping(layout.buildDirectory.file("proguard/mapping.txt"))
    printseeds(layout.buildDirectory.file("proguard/seeds.txt"))
    printusage(layout.buildDirectory.file("proguard/usage.txt"))
}

val copyJarToOut by tasks.registering(Copy::class) {
    dependsOn(tasks.shadowJar)
    from(tasks.shadowJar.flatMap { it.archiveFile })
    into(rootProject.layout.projectDirectory.dir("out"))
}

tasks.build {
    finalizedBy(copyJarToOut)
}

// Must be a 32-bit jre - ideally with jlink
val jrePath = file("${System.getProperty("user.home")}/.jdks/jdk1.8.0_171/")

// Build a bundle with an in-built 32-bit jre.
tasks.register<Zip>("bundleApp") {
    dependsOn(tasks.named("shadowJar"))

    archiveFileName.set("void-bundle.zip")
    destinationDirectory.set(layout.buildDirectory.dir("dist"))

    val shadowJar = tasks.shadowJar.get()
    from(shadowJar.archiveFile) {
        rename { "client.jar" }
        into("void-bundle")
    }
    from(jrePath) {
        into("void-bundle/jre")
    }
}

// Dependency-free deterministic core and actual native-hook regression checks.
val mobileTest by tasks.registering(JavaExec::class) {
    dependsOn(tasks.testClasses)
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("MobileNativeRegression")
    jvmArgs("-Djava.awt.headless=true")
}
tasks.test { dependsOn(mobileTest) }

// Same verified release payload, a different explicit launch entry point.
val jarRunnerJar by tasks.registering(Jar::class) {
    dependsOn(proguardJar)
    archiveFileName.set("void-client-jarrunner-mobile.jar")
    destinationDirectory.set(layout.buildDirectory.dir("libs"))
    from({ zipTree(layout.buildDirectory.file("libs/void-client-$version-release.jar").get().asFile) }) {
        exclude("META-INF/MANIFEST.MF", "META-INF/*.SF", "META-INF/*.RSA", "META-INF/*.DSA")
    }
    manifest { attributes("Main-Class" to "JarRunnerLauncher", "Implementation-Version" to "JR1") }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Run separately under Xvfb; headless native/core tests remain usable without a display.
val standaloneHostTest by tasks.registering(JavaExec::class) {
    dependsOn(tasks.testClasses)
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("StandaloneMobileRegression")
    jvmArgs("-Djava.awt.headless=false")
}

val standaloneReleaseTest by tasks.registering(JavaExec::class) {
    dependsOn(jarRunnerJar, tasks.testClasses)
    // Deliberately exclude main output: these checks must exercise the obfuscated distribution.
    classpath = files(sourceSets["test"].output, layout.buildDirectory.file("libs/void-client-jarrunner-mobile.jar"))
    mainClass.set("ReleasedJarRunnerSmoke")
    jvmArgs("-Djava.awt.headless=false")
}
