plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "org.manz"
version = "0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    // https://mvnrepository.com/artifact/org.apache.commons/commons-lang3
    implementation("org.apache.commons:commons-lang3:3.0")
    // https://mvnrepository.com/artifact/org.slf4j/slf4j-api
    implementation("org.slf4j:slf4j-api:2.0.9")
    // https://mvnrepository.com/artifact/ch.qos.logback/logback-classic
    implementation("ch.qos.logback:logback-classic:1.4.11")



}

tasks.test {
    useJUnitPlatform()
}

tasks.build {
    dependsOn(tasks.getByName("shadowJar"))
}

tasks.jar {
    manifest.attributes["Main-Class"] = "org.manz.Main"
}
