import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

allprojects {

  group = "io.knotx"

  plugins.withId("java-library") {
    tasks.withType<JavaCompile>().configureEach {
      with(options) {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
        encoding = "UTF-8"
      }
    }

    tasks.withType<Test>().configureEach {
      environment("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory")

      failFast = true
      useJUnitPlatform()
      testLogging {
        events = setOf(TestLogEvent.FAILED)
        exceptionFormat = TestExceptionFormat.SHORT
      }

      dependencies {
        "testImplementation"("org.junit.jupiter:junit-jupiter-api")
        "testRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine")
      }
    }
  }

}


