plugins {
    id("org.jetbrains.kotlin.jvm") version "2.2.21" apply false
    id("org.jetbrains.dokka") version "1.9.20" apply false
}

group = "com.github.vitech-team"
version = "0.1.1"

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "maven-publish")

    configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        jvmToolchain(17)
    }

    // Provide sources and javadoc jars
    the<JavaPluginExtension>().apply {
        withSourcesJar()
    }

    tasks.register<Jar>("javadocJar") {
        dependsOn(tasks.named("dokkaJavadoc"))
        from(layout.buildDirectory.dir("dokka/javadoc"))
        archiveClassifier.set("javadoc")
    }

    plugins.withId("java") {
        extensions.configure<PublishingExtension>("publishing") {
            publications {
                create<MavenPublication>("maven") {
                    from(components["java"])
                    artifact(tasks.named("javadocJar"))
                    pom {
                        name.set(project.name)
                        description.set(project.description)
                        url.set("https://github.com/vitech-team")
                        licenses {
                            license {
                                name.set("Apache License, Version 2.0")
                                url.set("https://www.apache.org/licenses/LICENSE-2.0")
                            }
                        }
                        scm {
                            url.set("https://github.com/vitech-team/kotlin-notebook-utils")
                        }
                        developers {
                            developer { name.set("vitechteam") }
                        }
                    }
                }
            }
        }
    }
}
