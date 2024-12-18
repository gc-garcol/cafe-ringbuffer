plugins {
    `java-library`
    `maven-publish`
}

group = "io.github.gc-garcol"
version = "1.3.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

var jupiterVersion = "5.11.3"

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${jupiterVersion}")
}

tasks.test {
    jvmArgs("--add-opens", "java.base/java.nio=ALL-UNNAMED")
    useJUnitPlatform()
}

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "io.github.gc-garcol"
            artifactId = "cafe-ringbuffer"

            from(components["java"])

            pom {
                name.set("cafe-ringbuffer")
                description.set("cafe-ringbuffer")
                url.set("https://gc-garcol.github.io/cafe-ringbuffer/gc/garcol/libcore/package-summary.html")
                inceptionYear.set("2024")
                licenses {
                    license {
                        name.set("Apache-2.0")
                        url.set("https://spdx.org/licenses/Apache-2.0.html")
                    }
                }
                developers {
                    developer {
                        id.set("gc-garcol")
                        name.set("cafe")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/gc-garcol/cafe-ringbuffer.git")
                    developerConnection.set("scm:git:ssh://github.com/gc-garcol/cafe-ringbuffer.git")
                    url.set("https://github.com/gc-garcol/cafe-ringbuffer")
                }
            }
        }
    }

    repositories {
//        maven {
//            name = "GitHubPackages"
//            url = uri("https://maven.pkg.github.com/gc-garcol/cafe-ringbuffer")
//            credentials {
//                username = System.getenv("GITHUB_ACTOR")
//                password = System.getenv("GITHUB_TOKEN")
//            }
//        }
        maven {
            url = layout.buildDirectory.dir("staging-deploy").get().asFile.toURI()
        }
    }
}
