package org.example

class ApplicationBuilder implements Serializable {
    def script

    ApplicationBuilder(script) {
        this.script = script
    }

    def buildApp(String appType, String repoName, String imageName) {
        def repoPath = "target-repo/${repoName}"

        switch (appType?.toLowerCase()) {
            case 'springboot':
                script.echo "🔧 Detected Spring Boot application"
                script.dir(repoPath) {
                    def matches = script.findFiles(glob: '**/pom.xml')
                    if (matches.length == 0) {
                        script.error("❌ No pom.xml found in ${repoPath}")
                    }
                    def pomPath = matches[0].path.replaceAll('\\\\', '/')
                    def pomDir = pomPath.contains('/') ? pomPath.substring(0, pomPath.lastIndexOf('/')) : '.'
                    script.dir(pomDir) {
                        script.sh 'mvn clean package -DskipTests'
                        checkDockerfileExists()
                        script.sh "docker build -t ${imageName.toLowerCase()}:latest ."
                    }
                }
                break

            case 'nodejs':
                script.echo "🔧 Detected Node.js application"
                script.dir(repoPath) {
                    script.sh 'npm install'
                    script.sh 'npm run build || echo "⚠️ No build step defined in package.json"'
                    checkDockerfileExists()
                    script.sh "docker build -t ${imageName.toLowerCase()}:latest ."
                }
                break

            case 'nginx':
            case 'php':
                script.echo "ℹ️ No build step needed for '${appType}' applications"
                break

            default:
                script.error("❌ Unsupported APP_TYPE: '${appType}'")
        }
    }

    private void checkDockerfileExists() {
        def dockerFiles = script.findFiles(glob: 'Dockerfile')
        if (dockerFiles.length == 0) {
            script.error("❌ Dockerfile not found in expected build directory")
        }
    }
}
