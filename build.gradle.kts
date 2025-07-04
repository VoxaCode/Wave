plugins {
    id("com.android.application") version "8.2.0" apply false
    id("com.android.library") version "8.2.0" apply false
    id("com.google.dagger.hilt.android") version "2.44" apply false 
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}