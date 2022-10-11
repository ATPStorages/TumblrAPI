plugins {
    val kotlinVersion = "1.7.20"
    kotlin("multiplatform") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
    id("maven-publish")
}

group = "me.adenosine3phosphate.tumblr_api"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

publishing {
    repositories {
        maven {
            //...
        }
    }
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
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native") { binaries.staticLib(); binaries.sharedLib() }
        hostOs == "Linux" -> linuxX64("native") { binaries.staticLib(); binaries.sharedLib() }
        isMingwX64 -> mingwX64("native") { binaries.staticLib(); binaries.sharedLib() }
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    targets.all {
        compilations.all {
            kotlinOptions {
                allWarningsAsErrors = true
            }
        }
    }

    
    sourceSets {
        val ktorVersion = "2.1.2"

        val commonMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-core:$ktorVersion")
                implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
            }
        }
        val jvmMain by getting {
            dependsOn(commonMain)
            dependencies { implementation("io.ktor:ktor-client-apache:$ktorVersion") }
        }
        val jvmTest by getting
        val jsMain by getting {
            dependsOn(commonMain)
            dependencies { implementation("io.ktor:ktor-client-js:$ktorVersion") }
        }
        val jsTest by getting {
            dependencies { implementation(kotlin("test-js")) }
        }
        val nativeMain by getting {
            dependsOn(commonMain)
            dependencies { implementation("io.ktor:ktor-client-curl:$ktorVersion") }
        }
        val nativeTest by getting
    }
}

tasks.wrapper {
    gradleVersion = "7.5.1"
    distributionType = Wrapper.DistributionType.ALL
}
