buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.2.51")
    }
}

allprojects {
    repositories {
        google()
        jcenter()
    }
    configurations {
        all {
            resolutionStrategy {
                force("com.google.guava:guava:22.0")
                force("org.jetbrains.kotlin:kotlin-reflect:1.2.60")

                dependencySubstitution {

                    substitute(module("org.jetbrains.kotlin:kotlin-stdlib-jre8:1.2.0"))
                        .with(module("org.jetbrains.kotlin:kotlin-stdlib:1.2.60"))

                    substitute(module("org.jetbrains.kotlin:kotlin-stdlib-jre7:1.2.0"))
                        .with(module("org.jetbrains.kotlin:kotlin-stdlib:1.2.60"))
                }
            }
        }
    }
}
