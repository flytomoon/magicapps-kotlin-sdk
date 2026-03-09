plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    `java-library`
    `maven-publish`
    signing
}

group = "com.magicapps"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("io.ktor:ktor-client-core:2.3.8")
    implementation("io.ktor:ktor-client-cio:2.3.8")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.8")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.8")

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation("io.ktor:ktor-client-mock:2.3.8")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            groupId = project.group.toString()
            artifactId = "magicapps-sdk"
            version = project.version.toString()

            pom {
                name.set("MagicApps SDK")
                description.set("Official MagicApps SDK for Kotlin/Android - provides authentication, registry, payments, and platform service access")
                url.set("https://github.com/magicapps/magicapps-infra")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("magicapps")
                        name.set("MagicApps Team")
                        email.set("dev@magicapps.com")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/magicapps/magicapps-infra.git")
                    developerConnection.set("scm:git:ssh://github.com:magicapps/magicapps-infra.git")
                    url.set("https://github.com/magicapps/magicapps-infra/tree/main/sdks/kotlin")
                }
            }
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/magicapps/magicapps-infra")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: project.findProperty("gpr.user") as String? ?: ""
                password = System.getenv("GITHUB_TOKEN") ?: project.findProperty("gpr.key") as String? ?: ""
            }
        }
        maven {
            name = "OSSRH"
            val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
            credentials {
                username = System.getenv("OSSRH_USERNAME") ?: project.findProperty("ossrh.username") as String? ?: ""
                password = System.getenv("OSSRH_PASSWORD") ?: project.findProperty("ossrh.password") as String? ?: ""
            }
        }
    }
}

signing {
    // Use in-memory GPG key from environment (CI) or local gradle.properties
    val signingKey = System.getenv("GPG_SIGNING_KEY") ?: project.findProperty("signing.key") as String?
    val signingPassword = System.getenv("GPG_SIGNING_PASSWORD") ?: project.findProperty("signing.password") as String?

    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
    }

    sign(publishing.publications["maven"])
}

// Only require signing for release builds (not snapshots or local dev)
tasks.withType<Sign>().configureEach {
    onlyIf {
        !version.toString().endsWith("SNAPSHOT") &&
            (System.getenv("GPG_SIGNING_KEY") != null || project.findProperty("signing.key") != null)
    }
}
