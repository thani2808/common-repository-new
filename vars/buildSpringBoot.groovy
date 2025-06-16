def call() {
    dir('target-repo') {
        def pomPath = sh(script: "find . -name pom.xml | head -1", returnStdout: true).trim()
        if (!pomPath) error "❌ No pom.xml found."

        def pomDir = pomPath.replaceFirst('/pom.xml$', '')
        dir(pomDir) {
            sh 'mvn clean package -DskipTests'
        }
    }
}
