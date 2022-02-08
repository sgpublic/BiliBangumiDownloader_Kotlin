// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
        @Suppress("JcenterRepositoryObsolete", "DEPRECATION")
        jcenter()

        maven("https://jitpack.io")
    }

    dependencies {
        classpath("com.android.tools.build:gradle:7.1.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.10")

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        @Suppress("JcenterRepositoryObsolete", "DEPRECATION")
        jcenter()

        maven("https://jitpack.io")
    }
}

tasks.create("clean") {
    delete(rootProject.buildDir)
}