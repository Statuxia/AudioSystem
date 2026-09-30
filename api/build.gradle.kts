plugins {
	java
	id("org.springframework.boot") version "4.1.1"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.audiosystem"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("org.springframework.kafka:spring-kafka")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	compileOnly("org.projectlombok:lombok:1.18.48")
	annotationProcessor("org.projectlombok:lombok:1.18.48")
	testCompileOnly("org.projectlombok:lombok:1.18.48")
	testAnnotationProcessor("org.projectlombok:lombok:1.18.48")

	implementation("com.bucket4j:bucket4j_jdk17-core:8.19.0")

	implementation("com.fasterxml.uuid:java-uuid-generator:5.1.0")

	implementation("software.amazon.awssdk:s3:2.55.3")
	implementation("org.apache.tika:tika-core:4.0.0")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
