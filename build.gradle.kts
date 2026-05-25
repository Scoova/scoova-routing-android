// JVM library — works in Android apps AND server-side Kotlin / KMP projects.
// Consumers depend on this via JitPack, GitHub Packages, or Maven Central:
// `info.scoo-va:scoova-routing:<version>`.
plugins {
    kotlin("jvm") version "2.2.0"
    `maven-publish`
    signing
    `java-library`
}

group = "info.scoo-va"
version = "1.1.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.json:json:20231013")
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}

kotlin { jvmToolchain(17) }
tasks.test { useJUnitPlatform() }

// Maven Central requires source + javadoc jars alongside the main jar.
java {
    withSourcesJar()
    withJavadocJar()
}

// ─── Publishing ──────────────────────────────────────────────────────────
// Three publish targets are wired here. None requires credentials at build
// time — only at `publish` task time.
//   - JitPack        → automatic. Push a GitHub tag, JitPack builds on demand.
//   - GitHubPackages → requires GITHUB_ACTOR + GITHUB_TOKEN (PAT with `write:packages`).
//   - MavenCentral   → requires OSSRH_USERNAME / OSSRH_PASSWORD + a GPG key on
//                      the path.
publishing {
    publications {
        create<MavenPublication>("release") {
            from(components["java"])

            groupId = "info.scoo-va"
            artifactId = "scoova-routing"
            version = project.version.toString()

            pom {
                name.set("Scoova Routing SDK (Android / JVM)")
                description.set("Standalone Valhalla routing client for the Scoova routing gateway (api.scoo-va.info/api/v1/routing) — route, optimizedRoute, isochrone, matrix, height (elevation), mapMatch, locate, status. Polyline6 decode included.")
                url.set("https://github.com/Scoova/scoova-routing-android")

                licenses {
                    license {
                        name.set("Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }

                developers {
                    developer {
                        id.set("scoova")
                        name.set("Scoova")
                        email.set("info@scoo-va.info")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/Scoova/scoova-routing-android.git")
                    developerConnection.set("scm:git:ssh://github.com:Scoova/scoova-routing-android.git")
                    url.set("https://github.com/Scoova/scoova-routing-android")
                }
            }
        }
    }

    repositories {
        // GitHub Packages — push immediately with a PAT, no Sonatype dance.
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Scoova/scoova-routing-android")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: project.findProperty("gpr.user") as? String ?: ""
                password = System.getenv("GITHUB_TOKEN") ?: project.findProperty("gpr.key") as? String ?: ""
            }
        }

        // Maven Central via the Sonatype OSSRH s01 staging.
        maven {
            name = "MavenCentral"
            val releasesUrl  = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsUrl else releasesUrl
            credentials {
                username = System.getenv("OSSRH_USERNAME") ?: project.findProperty("ossrh.username") as? String ?: ""
                password = System.getenv("OSSRH_PASSWORD") ?: project.findProperty("ossrh.password") as? String ?: ""
            }
        }
    }
}

// GPG signing — Sonatype requires every artifact to be signed. We only run
// the signing tasks when actually publishing to Maven Central, so a developer
// who only wants to build locally does not need a GPG key.
signing {
    isRequired = gradle.taskGraph.hasTask("publishReleasePublicationToMavenCentralRepository")
    sign(publishing.publications["release"])
}
