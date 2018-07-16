[![Build Status](https://travis-ci.org/beworker/autoplay.svg?branch=master)](https://travis-ci.org/beworker/autoplay)
[![Kotlin version badge](https://img.shields.io/badge/kotlin-1.2.51-blue.svg)](http://kotlinlang.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)

<img src="https://github.com/beworker/autoplay/blob/master/publishing/autoplay-logo.png" alt="autoplay" width=200 />

# Autoplay
Gradle plugin for publishing Android artifacts to Google Play.

# Fetures

- Autoplay is optimized for CI/CD usage:
  - it does **not** trigger artifacts assembly task - you can reuse build artifacts from previous build steps;
  - it accepts JSON key as **base64-encoded string**, which is a secure and convenient way of providing secret data.
  
- Autoplay is developer friendly:
  - it does **not** require storing any dummy keys in source control;
  - it fails just misconfigured publish-task and not the whole build on evaluation step;
  - it has a single publish task for uploading akp, mapping file and release notes.
  
- Autoplay is reliable and future-proof:
  - it has very clean and concise implementation, which is easy to understand;
  - it's covered by unit tests and easy to maintain and extend;
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
    classpath "de.halfbit:autoplay:<version>"
  }
}
```

Releases of Autoplay are published to `mavenCentral()` repository. Check [releases](https://github.com/beworker/autoplay/releases) section to find lastest release version.

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
stages:
  - assemble
  - release

assemble:
  stage: assemble
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

You can encode JSON key file into base64 string using following shell command (linux, mac)

```shell
base64 -i secret.json -o -
```

and provide the value to the build script using a [protected variable](https://docs.gitlab.com/ee/ci/variables/#variables).

# Release Notes

Autoplay takes apk- and mapping-files for uploading from the respective build output directories. Release notes assigned to a release should be provided additionally. Autoplay expects release notes to be stored under `src/main/autoplay/release-notes` directory in accordance with the structure shown down below.

```
src
  +- main
       +- java
       +- autoplay
            +- release-notes
                 +- <track>           e.g. internal
                     +- <locale>.txt  e.g. en-US.txt
```

Happy continuous integration!

# License
```
Copyright 2018 Sergej Shafarenka, www.halfbit.de

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
