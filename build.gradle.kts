buildscript {
    repositories {
        google()
        jcenter()
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
            }
        }
    }
}
