# Autoplay
Gradle plugin for publishing Android artifacts to Google Play.

# Fetures

- Autoplay is optimized for CI/CD usage:
  - it does **not** trigger artifacts assembly task - you can reuse build artifacts from previous build steps.
  - it accepts JSON key as **base64-encoded string**, which is a secure and convenient way of providing secret data.
  
- Autoplay is developer friendly:
  - it does **not** require storing any dummy keys in source control.
  - it fails the misconfigured publish-task only and not the whole build on evaluation step.
  - it has a single publish task for uploading akp, mapping file and release notes.
  
- Autoplay is reliable and future-proof:
  - it has very a clean and concise implementation, which is easy to understant.
  - it's covered by unit tests and easy to maintain and extend.
  - it's built using latest technologies and tools like Kotlin, Kotlin-DSL for Gradle, Android Gradle plugin etc.
 
# Usage

In the main `build.gradle`

```gradle
buildscript {

  repositories {
    google()
    jcenter()  
  }
  
  dependencies {
    classpath "de.halfbit:android-autoplay:<version>"
  }
}
```

In the application module's `build.gradle`

```gradle
apply plugin: 'com.android.application'
apply plugin: 'android-autoplay'

autoplay {
    track "internal"
    secretJsonBase64 project.hasProperty('SECRET_JSON') ? project.property('SECRET_JSON') : ''
}
```

Call `./gradlew tasks` and you will see a new publishing task `publishApk<BuildVariant>` in the list. Autoplay adds this task for each build variant of `release` type. For a project without custom build flavors configured, the task is called `publishApkRelease`.

Now you can call this task from a central build script. Here is an example of how to use it with Gitlab CI.

```yml
...

stages:
  - build
  - release

assemble:
  stage: build
  only:
    - master
  script:
    - ./gradlew clean assembleRelease -PSTORE_PASS=${STORE_PASS} -PKEY_PASS=${KEY_PASS}
  artifacts:
    paths:
      - app/build/outputs/

release:
  stage: release
  dependencies:
    - assemble
  only:
    - master
  script:
    - ./gradlew publishApkRelease -PSECRET_JSON=${SECRET_JSON}
    

```

# Metadata

Autoplay takes apk- and mapping-files from the respective build output directories. The other listing files to upload (like release notes) should be provided additionally. Autoplay expects listing files to be stored under `src/main/autoplay` in accoradance to the structure shown below.

```
src
  +- main
       +- java
       +- autoplay
            +- release-notes
                 +- <locale>          e.g. en-US
                     +- <track>.txt   e.g. internal.txt
```

Happy coding and enjoy!
