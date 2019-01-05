buildscript {
    repositories {
        google()
        mavenCentral()
        jcenter()
    }
}

tasks.withType<Wrapper>().configureEach {
    distributionType = Wrapper.DistributionType.ALL
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
            }
        }
    }
}
