plugins {
	java
	id("org.springframework.boot") version "4.1.1"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.soundservice"
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
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	compileOnly("org.projectlombok:lombok:1.18.48")
	annotationProcessor("org.projectlombok:lombok:1.18.48")
	testCompileOnly("org.projectlombok:lombok:1.18.48")
	testAnnotationProcessor("org.projectlombok:lombok:1.18.48")

	implementation("ch.qos.logback:logback-classic:1.5.21")
	implementation("com.bucket4j:bucket4j_jdk17-core:8.19.0")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
