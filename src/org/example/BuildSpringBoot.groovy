package org.example

class BuildSpringBoot {
    def script
    BuildSpringBoot(script) {
        this.script = script
    }

    def build() {
        script.echo "🧪 Building Spring Boot JAR..."
        script.sh "cd target-repo/${script.env.PROJECT_DIR} && mvn clean package -DskipTests"
    }
}
