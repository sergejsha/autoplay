plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    id("org.gradle.signing")
    id("org.jetbrains.dokka") version "0.9.17"
}

repositories {
    google()
    jcenter()
}

dependencies {
    compileOnly("com.android.tools.build:gradle:3.0.1")

    implementation(kotlin("stdlib", "1.2.61"))
    implementation("com.google.apis:google-api-services-androidpublisher:v3-rev12-1.23.0")

    testImplementation("junit:junit:4.12")
    testImplementation("com.google.truth:truth:0.40")
    testImplementation("com.android.tools.build:gradle:3.1.3")
}

gradlePlugin {
    isAutomatedPublishing = false
    (plugins) {
        "android-autoplay" {
            id = "android-autoplay"
            implementationClass = "de.halfbit.tools.autoplay.PlayPublisherPlugin"
        }
    }
}

group = "de.halfbit"
version = "0.3.3"

publishing {

    repositories {
        maven {
            name = "local"
            url = uri("$buildDir/repository")
        }
        maven {
            name = "central"
            url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2")
            credentials {
                username = project.getPropertyOrEmptyString("NEXUS_USERNAME")
                password = project.getPropertyOrEmptyString("NEXUS_PASSWORD")
            }
        }
    }

    val dokka by tasks.getting(org.jetbrains.dokka.gradle.DokkaTask::class) {
        outputFormat = "javadoc"
        outputDirectory = "$buildDir/javadoc"
    }

    val sourcesJar by tasks.creating(Jar::class) {
        classifier = "sources"
        from(java.sourceSets["main"].allSource)
    }

    val javadocJar by tasks.creating(Jar::class) {
        classifier = "javadoc"
        from(dokka)
    }

    (publications) {
        "Autoplay"(MavenPublication::class) {
            from(components["java"])
            artifact(sourcesJar)
            artifact(javadocJar)
            pom {
                name.set("Gradle plugin for publishing Android artifacts to Google Play.")
                description.set("Gradle plugin for publishing Android artifacts to Google Play.")
                url.set("http://www.halfbit.de")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("beworker")
                        name.set("Sergej Shafarenka")
                        email.set("info@halfbit.de")
                    }
                }
                scm {
                    connection.set("scm:git:git@github.com:beworker/autoplay.git")
                    developerConnection.set("scm:git:ssh://github.com:beworker/autoplay.git")
                    url.set("http://www.halfbit.de")
                }
            }
        }
    }

}

signing {
    sign(publishing.publications["Autoplay"])
}

fun Project.getPropertyOrEmptyString(name: String): String {
    return if (hasProperty(name)) {
        property(name) as String? ?: ""
    } else {
        ""
    }
}

tasks.withType<Test> {
    addTestListener(object : TestListener {
        override fun beforeSuite(suite: TestDescriptor) {}
        override fun beforeTest(testDescriptor: TestDescriptor) {}
        override fun afterSuite(suite: TestDescriptor, result: TestResult) {}
        override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {
            if (result.resultType == TestResult.ResultType.FAILURE) {
                result.exception?.printStackTrace()
            }
        }
    })
}