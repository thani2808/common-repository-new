package org.example

class BuildSpringBoot implements Serializable {
    def steps

    BuildSpringBoot(steps) {
        this.steps = steps
    }

    void build(String repoName) {
        steps.echo "🧪 Building Spring Boot JAR..."
        steps.dir("target-repo/${repoName}") {
            steps.sh 'mvn clean package -DskipTests'
        }
    }
}
