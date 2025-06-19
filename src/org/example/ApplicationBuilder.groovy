package org.example

class ApplicationBuilder implements Serializable {
    def steps

    ApplicationBuilder(steps) {
        this.steps = steps
    }

    def build(String repo, String branch) {
        steps.echo "⚙️ build(repo, branch) invoked"
        buildApp("springboot", repo, repo.toLowerCase())
    }

    def buildApp(String appType, String repoName, String imageName) {
        def path = "target-repo/${repoName}"
        steps.dir(path) {
            switch (appType.toLowerCase()) {
                case 'springboot':
                    def matches = steps.findFiles(glob: '**/pom.xml')
                    if (!matches) steps.error("❌ pom.xml not found in ${path}")
                    steps.dir(matches[0].path.replaceAll('\\\\', '/').replaceAll('/[^/]+$', '')) {
                        steps.sh 'mvn clean package -DskipTests'
                        checkDockerfileExists()
                        steps.sh "docker build -t ${imageName}:latest ."
                    }
                    break
                case 'nodejs':
                    steps.sh 'npm install'
                    steps.sh 'npm run build || echo "⚠️ No build step defined."'
                    checkDockerfileExists()
                    steps.sh "docker build -t ${imageName}:latest ."
                    break
                case 'nginx':
                case 'php':
                    steps.echo "ℹ️ No build required for ${appType}"
                    break
                default:
                    steps.error("❌ Unsupported app type: ${appType}")
            }
        }
    }

    private void checkDockerfileExists() {
        if (!steps.findFiles(glob: 'Dockerfile')) {
            steps.error("❌ Dockerfile missing.")
        }
    }
}
