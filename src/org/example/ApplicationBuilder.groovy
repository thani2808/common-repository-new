package org.example

class ApplicationBuilder implements Serializable {
    def steps

    ApplicationBuilder(steps) {
        this.steps = steps
    }

    // Entry point when only repo and branch are passed; assumes default appType
    def build(String repo, String branch) {
        steps.echo "⚙️  Running build(repo, branch)"
        buildApp("springboot", repo, "${repo.toLowerCase()}") // Default imageName = repo name
    }

    // Main method with appType, repo name, and image name
    def buildApp(String appType, String repoName, String imageName) {
        def repoPath = "target-repo/${repoName}"
        def lowerAppType = appType?.toLowerCase()

        switch (lowerAppType) {
            case 'springboot':
                steps.echo "🔧 Detected Spring Boot application"
                steps.dir(repoPath) {
                    def matches = steps.findFiles(glob: '**/pom.xml')
                    if (matches.length == 0) {
                        steps.error("❌ No pom.xml found in ${repoPath}")
                    }
                    def pomPath = matches[0].path.replaceAll('\\\\', '/')
                    def pomDir = pomPath.contains('/') ? pomPath.substring(0, pomPath.lastIndexOf('/')) : '.'

                    steps.dir(pomDir) {
                        steps.sh 'mvn clean package -DskipTests'
                        checkDockerfileExists()
                        steps.sh "docker build -t ${imageName.toLowerCase()}:latest ."
                    }
                }
                break

            case 'nodejs':
                steps.echo "🔧 Detected Node.js application"
                steps.dir(repoPath) {
                    steps.sh 'npm install'
                    steps.sh 'npm run build || echo "⚠️ No build step defined in package.json"'
                    checkDockerfileExists()
                    steps.sh "docker build -t ${imageName.toLowerCase()}:latest ."
                }
                break

            case 'nginx':
            case 'php':
                steps.echo "ℹ️ No build step needed for '${appType}' applications"
                break

            default:
                steps.error("❌ Unsupported APP_TYPE: '${appType}'")
        }
    }

    private void checkDockerfileExists() {
        def dockerFiles = steps.findFiles(glob: 'Dockerfile')
        if (dockerFiles.length == 0) {
            steps.error("❌ Dockerfile not found in expected build directory")
        }
    }
}
