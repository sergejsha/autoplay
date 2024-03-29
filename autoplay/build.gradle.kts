plugins {
    `kotlin-dsl`
    `maven-publish`
    id("org.gradle.signing")
    id("org.jetbrains.dokka") version "0.9.17"
    kotlin("jvm").version(embeddedKotlinVersion).apply(false)
}

dependencies {
    compileOnly("com.android.tools.build:gradle:4.1.3") {
        exclude(group = "org.jetbrains.kotlin")
    }
    implementation("com.google.apis:google-api-services-androidpublisher:v3-rev142-1.25.0")

    testImplementation("junit:junit:4.12")
    testImplementation("com.google.truth:truth:0.40")
    testImplementation("com.android.tools.build:gradle:4.1.3") {
        exclude(group = "org.jetbrains.kotlin")
    }
}

gradlePlugin {
    isAutomatedPublishing = false
    plugins {
        create("android-autoplay") {
            id = "android-autoplay"
            implementationClass = "de.halfbit.tools.autoplay.PlayPublisherPlugin"
        }
    }
}

group = "de.halfbit"
version = "4.0.0"

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
        from(sourceSets["main"].allSource)
    }

    val javadocJar by tasks.creating(Jar::class) {
        classifier = "javadoc"
        from(dokka)
    }

    publications {
        create("Autoplay", MavenPublication::class) {
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

if (project.hasProperty("signing.keyId")) {
    signing {
        sign(publishing.publications["Autoplay"])
    }
}

fun Project.getPropertyOrEmptyString(name: String): String =
    if (hasProperty(name)) property(name) as String? ?: ""
    else ""

tasks.withType<Test> {
    addTestListener(object : TestListener {
        override fun beforeSuite(suite: TestDescriptor) {}
        override fun afterSuite(suite: TestDescriptor, result: TestResult) {}
        override fun beforeTest(testDescriptor: TestDescriptor) {}
        override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {
            if (result.resultType == TestResult.ResultType.FAILURE) {
                result.exception?.printStackTrace()
            }
        }
    })
}
