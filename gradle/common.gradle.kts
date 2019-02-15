import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

allprojects {

    plugins.withId("java-library") {
        tasks.withType<JavaCompile>().configureEach {
            with(options) {
                sourceCompatibility = "1.8"
                targetCompatibility = "1.8"
                encoding = "UTF-8"
            }
        }

        tasks.withType<Test>().configureEach {
            failFast = true
            useJUnitPlatform()
            testLogging {
                events = setOf(TestLogEvent.FAILED)
                exceptionFormat = TestExceptionFormat.SHORT
            }

            dependencies {
                "testImplementation"("org.junit.jupiter:junit-jupiter-api")
                "testRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine:5")
            }
        }
    }

}