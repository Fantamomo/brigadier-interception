plugins {
    kotlin("jvm") version "2.3.0"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
    `maven-publish`
}

group = "com.fantamomo.mc"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
}

dependencies {
    testImplementation(kotlin("test"))
    paperweight.paperDevBundle("1.21.11-R0.1-SNAPSHOT")
}

paperweight.reobfArtifactConfiguration =
    io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

kotlin {
    jvmToolchain(24)
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifact(tasks.named("reobfJar"))
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Fantamomo/brigadier-interception")

            credentials {
                username = project.findProperty("gpr.user") as String?
                    ?: System.getenv("GITHUB_ACTOR")

                password = project.findProperty("gpr.key") as String?
                    ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}