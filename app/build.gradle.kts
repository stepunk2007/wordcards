plugins {
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))
    implementation(project(":application"))
    implementation(project(":infra-llm"))
    implementation(project(":infra-pronunciation"))
    implementation(project(":infra-db"))
    implementation(project(":api-rest"))
    implementation(project(":bot-telegram"))

    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.actuator)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.starter.jdbc)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.testcontainers.postgresql)
}

// One-off import of English IPA from Wiktionary (kaikki.org), streamed without saving the ~520 MB dump.
// Needs the database: docker compose up -d postgres. Override the source with -Pkaikki=<file or URL>.
tasks.register<org.springframework.boot.gradle.tasks.run.BootRun>("importIpa") {
    description = "Imports English IPA from a kaikki.org Wiktionary dump into the pronunciation table."
    group = "application"
    mainClass = "dev.words.WordsApplication"
    classpath = sourceSets.main.get().runtimeClasspath
    val source = providers.gradleProperty("kaikki")
        .orElse("https://kaikki.org/dictionary/English/kaikki.org-dictionary-English.jsonl.gz")
    args(
        source.map { "--words.pronunciation.import.kaikki=$it" }.get(),
        "--spring.main.web-application-type=none",
    )
    jvmArgs("-Xmx2g")
}
