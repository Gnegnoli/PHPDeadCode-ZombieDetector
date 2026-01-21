import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.24"
    id("org.jetbrains.intellij") version "1.17.4"
}

group = "com.zombiedetector"
version = "1.0.0"

repositories {
    mavenCentral()
}

intellij {
    // PHP PSI is provided by the bundled PHP plugin, which is available in IntelliJ IDEA Ultimate / PhpStorm.
    type.set("IU")
    version.set("2024.3.2")
    plugins.set(listOf("com.jetbrains.php"))
    updateSinceUntilBuild.set(true)
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
            freeCompilerArgs += listOf("-Xjvm-default=all")
        }
    }

    patchPluginXml {
        sinceBuild.set("243")
        untilBuild.set("243.*")
    }

    runIde {
        jvmArgs = listOf("-Xmx2g")
    }

    signPlugin {
        // For Marketplace publishing: provide env vars when needed.
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}

dependencies {
    // No extra runtime deps; we rely on IntelliJ + PHP plugin.
}

