plugins {
    val kotlinVersion = "1.7.20"
    kotlin("multiplatform") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
    `maven-publish`
    signing
}

group = "me.adenosine3phosphate"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

publishing {
    publications {
        create<MavenPublication>("mavenKotlin") {
            groupId = project.group.toString()
            artifactId = "tumblr-api"
            version = project.version.toString()

            pom {
                name.set("Tumblr API")
                description.set("A general purpose Tumblr API, with support for NPF (Neue Post Format,) as well as OAuth 1/2.")
                inceptionYear.set("2022")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                issueManagement {
                    system.set("GitHub Issues")
                    url.set("https://github.com/ATPStorages/TumblrAPI/issues")
                }

                developers {
                    developer {
                        id.set("mikoe")
                        name.set("Miko Elbrecht")
                        email.set("pmt.mailservice@gmail.com")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/ATPStorages/TumblrAPI.git")
                    developerConnection.set("scm:git:ssh:git@github.com:ATPStorages/TumblrAPI.git")
                    url.set("https://github.com/ATPStorages/TumblrAPI")
                }
            }
        }
    }

    repositories {
        maven {
            name = "tumblr-api"
            url = uri(layout.buildDirectory.dir("repo"))
        }
    }
}

signing {
    sign(publishing.publications["mavenKotlin"])
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    js(IR) {
        binaries.executable()
        browser { testTask { useMocha { timeout = "10s" } } }
        nodejs { testTask { useMocha { timeout = "10s" } } }
    }
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    when {
        hostOs == "Mac OS X" -> macosX64("native") { binaries.staticLib(); binaries.sharedLib() }
        hostOs == "Linux" -> linuxX64("native") { binaries.staticLib(); binaries.sharedLib() }
        isMingwX64 -> mingwX64("native") { binaries.staticLib(); binaries.sharedLib() }
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    targets.all {
        compilations.all {
            kotlinOptions {
                //allWarningsAsErrors = true
            }
        }
    }
    
    sourceSets {
        val ktorVersion = "2.1.2"
        val coroutineVersion = "1.6.4"

        val commonMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-core:$ktorVersion")
                implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutineVersion")

                implementation("com.javiersc.kotlinx:coroutines-run-blocking-all:0.1.0-rc.1")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutineVersion")
            }
        }
        val jvmMain by getting {
            dependencies { implementation("io.ktor:ktor-client-apache:$ktorVersion") }
        }
        val jvmTest by getting
        val jsMain by getting {
            dependencies { implementation("io.ktor:ktor-client-js:$ktorVersion") }
        }
        val jsTest by getting {
            dependencies { implementation(kotlin("test-js")) }
        }
        val nativeMain by getting {
            dependencies { implementation("io.ktor:ktor-client-curl:$ktorVersion") }
        }
        val nativeTest by getting
    }
}

tasks.wrapper {
    gradleVersion = "7.5.1"
    distributionType = Wrapper.DistributionType.ALL
}
