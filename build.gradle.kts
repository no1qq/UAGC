plugins {
    `java-library`
}

group = "io.github.no1qq"
version = "0.1.0-SNAPSHOT"

val paperApiVersion = "1.21-R0.1-SNAPSHOT"
val paperApiVersionNewest = "1.21.11-R0.1-SNAPSHOT"
val adventureVersion = "4.17.0"
val adventureVersionNewest = "4.26.1"
val junitVersion = "5.14.4"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

val newestApi: Configuration by configurations.creating

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")
    compileOnly("net.kyori:adventure-text-minimessage:$adventureVersion")

    newestApi("io.papermc.paper:paper-api:$paperApiVersionNewest")
    newestApi("net.kyori:adventure-text-minimessage:$adventureVersionNewest")

    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 21
    options.compilerArgs.addAll(listOf("-Xlint:all,-serial,-processing", "-parameters"))
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

tasks.processResources {
    val props = mapOf("version" to project.version.toString())
    inputs.properties(props)
    filesMatching("paper-plugin.yml") {
        expand(props)
    }
}

val compileAgainstNewestApi by tasks.registering(JavaCompile::class) {
    source = sourceSets.main.get().allJava
    classpath = newestApi
    destinationDirectory = layout.buildDirectory.dir("classes/java/newest-api")
    options.encoding = "UTF-8"
    options.release = 21
    options.compilerArgs.addAll(listOf("-nowarn", "-parameters"))
    javaCompiler = javaToolchains.compilerFor {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.check {
    dependsOn(compileAgainstNewestApi)
}

tasks.jar {
    archiveBaseName = "UAGC"
}
