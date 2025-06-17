package org.example

class BuildSpringBoot implements Serializable {
    def steps

    BuildSpringBoot(steps) {
        this.steps = steps
    }

    void build() {
        steps.dir('target-repo') {
            def pomPath = steps.sh(script: "find . -name pom.xml | head -1", returnStdout: true).trim()
            if (!pomPath) steps.error "❌ No pom.xml found."

            def pomDir = pomPath.replaceFirst('/pom.xml$', '')
            steps.dir(pomDir) {
                steps.sh 'mvn clean package -DskipTests'
            }
        }
    }
}
