plugins {
	java
	alias(libs.plugins.spring.boot)
	alias(libs.plugins.spring.dependency.management)
}

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
	implementation("org.springframework.boot:spring-boot-starter-websocket")
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("org.springframework.kafka:spring-kafka")

	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.boot:spring-boot-starter-websocket-test")
	testImplementation("org.springframework.kafka:spring-kafka-test")
	testImplementation("org.testcontainers:testcontainers-kafka")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	compileOnly(libs.lombok)
	annotationProcessor(libs.lombok)
	testCompileOnly(libs.lombok)
	testAnnotationProcessor(libs.lombok)
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.register("resolveDependencies") {
	val classpaths = files(
		configurations.compileClasspath,
		configurations.runtimeClasspath,
		configurations.annotationProcessor
	)
	inputs.files(classpaths)
	doLast { println("Resolved ${classpaths.files.size} files") }
}

tasks.bootJar { archiveFileName = "app.jar" }
