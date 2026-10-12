dependencies {
    implementation(platform(libs.spring.boot.dependencies))
    implementation(project(":application"))

    implementation(libs.spring.boot.starter.jdbc)
    implementation(libs.spring.boot.starter.flyway)
    runtimeOnly(libs.flyway.database.postgresql)
    runtimeOnly(libs.postgresql)
}
