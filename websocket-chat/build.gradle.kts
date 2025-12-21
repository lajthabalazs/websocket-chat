plugins {
    id("java")
    id("io.micronaut.application") version "4.2.1"
}

micronaut {
    version("4.2.1")
    runtime("netty")
}

application {
    mainClass.set("ca.lajtha.websocketchat.Application")
}

group = "ca.lajtha"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.netty:netty-all:4.1.104.Final")
    implementation("com.google.inject:guice:7.0.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("de.mkammerer:argon2-jvm:2.12")
    implementation("com.auth0:java-jwt:4.4.0")
    
    // Micronaut dependencies
    annotationProcessor("io.micronaut:micronaut-http-validation")
    implementation("io.micronaut:micronaut-http-server-netty")
    implementation("io.micronaut:micronaut-runtime")
    implementation("io.micronaut:micronaut-jackson-databind")
    
    // Logging
    runtimeOnly("ch.qos.logback:logback-classic")
    
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.11.0")
    testImplementation("io.micronaut:micronaut-http-client")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("-Dnet.bytebuddy.experimental=true")
}