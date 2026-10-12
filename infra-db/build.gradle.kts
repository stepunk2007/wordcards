dependencies {
    implementation(platform(libs.spring.boot.dependencies))
    implementation(project(":application"))

    implementation(libs.spring.boot.starter.jdbc)
    runtimeOnly(libs.postgresql)
}
