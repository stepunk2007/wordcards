dependencies {
    implementation(platform(libs.spring.boot.dependencies))
    implementation(project(":application"))

    implementation(libs.spring.boot.starter)
    implementation(libs.anthropic.java)
    implementation(libs.openai.java)

    testImplementation(platform(libs.spring.boot.dependencies))
    testImplementation(libs.spring.boot.starter.test)
}

tasks.test {
    useJUnitPlatform {
        excludeTags("manual")
    }
}

// Real Claude API calls: costs money, run by hand.
val manualTest by tasks.registering(Test::class) {
    description = "Runs tests that call the real Claude API."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("manual")
    }
    testLogging {
        showStandardStreams = true
    }
    outputs.upToDateWhen { false }
}
