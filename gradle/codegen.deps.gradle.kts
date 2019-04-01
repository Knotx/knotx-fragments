dependencies {
  "annotationProcessor"(platform("io.knotx:knotx-dependencies:${project.version}"))
  "annotationProcessor"(group = "io.vertx", name = "vertx-codegen", classifier = "processor")
  "annotationProcessor"(group = "io.vertx", name = "vertx-service-proxy", classifier = "processor")
  "annotationProcessor"(group = "io.vertx", name = "vertx-rx-java2-gen")
}

tasks.named<JavaCompile>("compileJava") {
  options.annotationProcessorGeneratedSourcesDirectory = file("src/main/generated")
  options.compilerArgs = listOf(
          "-processor", "io.vertx.codegen.CodeGenProcessor",
          "-Acodegen.output=${project.projectDir}/docs"
  )
}

tasks.named<Delete>("clean") {
  delete.add("src/main/generated")
}