plugins {
    kotlin("jvm") version "2.0.20"

    `java-gradle-plugin`
    `kotlin-dsl`

    `maven-publish`
}

group = "at.ssw"
version = "0.0.2"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("at.ssw:KIRHelperKit:0.0.2")
    implementation(kotlin("stdlib"))
    implementation(kotlin("compiler-embeddable"))

    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:2.0.20")
    implementation(gradleApi())
    implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.5.3")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    testImplementation("com.bennyhuo.kotlin:kotlin-compile-testing-extensions:2.0.0-1.3.0")
}

tasks.test {
    useJUnitPlatform()
}
