[![Build Status](https://travis-ci.org/beworker/autoplay.svg?branch=master)](https://travis-ci.org/beworker/autoplay)
[![Maven central](http://img.shields.io/maven-central/v/de.halfbit/autoplay.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22de.halfbit%22%20a%3A%22autoplay%22)
[![Kotlin version badge](https://img.shields.io/badge/kotlin-1.2.61-blue.svg)](http://kotlinlang.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)

<img src="https://github.com/beworker/autoplay/blob/master/publishing/autoplay-logo.png" alt="autoplay" width=200 />

# Autoplay
Gradle plugin for publishing Android artifacts to Google Play.

# Features

- Autoplay is optimized for CI/CD usage:
  - it does **not** trigger assembly task - you can reuse build artifacts from previous build steps;
  - it accepts JSON key as **base64-encoded string**, which is a secure and convenient way of providing sensitive data.
  
- Autoplay is developer friendly:
  - it does **not** require storing any dummy keys in source control;
  - it can be used without credentials provided in development;
  - it has a single publish task for uploading artifacts (apk or app bundle) and release notes.
  
- Autoplay is reliable and future-proof:
  - it has clean and concise implementation, which is easy to understand, extend and fix;
  - it's covered by unit tests;
  - it's built using latest technologies and tools.
 
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

Latest published version can be found at [![Maven central](http://img.shields.io/maven-central/v/de.halfbit/autoplay.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22de.halfbit%22%20a%3A%22autoplay%22)

## Gradle compatibility

Gradle Version | Autoplay Version
-----------|---------------
|4.8 | 1.0.0|
|4.10.1 | 1.1.0|

## Publishing apk

In the application module's `build.gradle`

```gradle
apply plugin: 'com.android.application'
apply plugin: 'android-autoplay'

autoplay {
    track "internal"
    secretJsonBase64 project.hasProperty('SECRET_JSON') ? project.property('SECRET_JSON') : ''
}
```

Execute `./gradlew tasks` and you will see a new publishing task `publishApk<BuildVariant>` in the list. Autoplay adds this task for each build variant of `release` type. For a project without custom build flavors the task is named `publishApkRelease`.

## Publishing app bundle

In the application module's `build.gradle`

```gradle
apply plugin: 'com.android.application'
apply plugin: 'android-autoplay'

autoplay {
    track "internal"
    artifactType "bundle"
    secretJsonBase64 project.hasProperty('SECRET_JSON') ? project.property('SECRET_JSON') : ''
}
```

Execute `./gradlew tasks` and you will see a new publishing task `publishBundle<BuildVariant>` in the list. Autoplay adds this task for each build variant of `release` type. For a project without custom build flavors the task is named `publishBundleRelease`.

## Central build

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
    - ./gradlew clean bundleRelease -PSTORE_PASS=${STORE_PASS} -PKEY_PASS=${KEY_PASS}
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
    - ./gradlew publishBundleRelease -PSECRET_JSON=${SECRET_JSON}
```

You can encode JSON key file into base64 string using following shell command (linux, mac)

```shell
base64 -i secret.json -o -
```

and provide the value to the build script using a [protected variable](https://docs.gitlab.com/ee/ci/variables/#variables).

## Publishing Release Notes

Autoplay takes apk and obfuscation mapping files (or app bundle file, if `artifactType "bundle"` is set) for uploading from the default build output directories. Release notes are to be stored under `src/main/autoplay/release-notes` directory in accordance to the structure shown down below.

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
