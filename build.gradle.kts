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
version = "1.1.2"

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

            groupId    = "info.scoo-va"
            artifactId = "scoova-routing-android"
            version    = project.version.toString()

            pom {
                name.set("Scoova Routing SDK (Android / JVM)")
                description.set("Standalone Valhalla routing client for the Scoova routing gateway.")
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
        // GitHub Packages — works immediately in Actions via GITHUB_TOKEN.
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Scoova/scoova-routing-android")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: project.findProperty("gpr.user") as? String ?: ""
                password = System.getenv("GITHUB_TOKEN") ?: project.findProperty("gpr.key") as? String ?: ""
            }
        }

        // Local staging dir. `publishReleasePublicationToLocalStagingRepository`
        // writes the signed Maven layout here; the publish-to-central-portal.sh
        // script zips it and uploads to Sonatype Central Portal.
        maven {
            name = "LocalStaging"
            url = uri(layout.buildDirectory.dir("staging-deploy"))
        }
    }
}

// In-memory PGP signing — required by Maven Central. SIGNING_KEY is the
// ASCII-armored secret key; SIGNING_PASSWORD is optional (current Scoova
// release key is passphrase-less). When absent (local builds, GitHub
// Packages), signing is skipped.
signing {
    val signingKey: String? = System.getenv("SIGNING_KEY")
    // For a passphrase-less key, useInMemoryPgpKeys needs empty string, not null
    val signingPassword: String = System.getenv("SIGNING_PASSWORD") ?: ""
    isRequired = signingKey != null
    if (signingKey != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications["release"])
    }
}