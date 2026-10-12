dependencies {
    implementation(platform(libs.spring.boot.dependencies))
    implementation(project(":application"))

    implementation(libs.spring.boot.starter)
    implementation(libs.anthropic.java)

    testImplementation(platform(libs.spring.boot.dependencies))
    testImplementation(libs.spring.boot.starter.test)
}
