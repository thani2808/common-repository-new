package org.example

class ApplicationBuilder implements Serializable {
    def steps

    ApplicationBuilder(steps) {
        this.steps = steps
    }

    void build(String repo, String branch) {
        steps.echo "⚙️ build(repo, branch) invoked"
        def appType = steps.env.APP_TYPE?.toLowerCase() ?: 'springboot'
        def imageName = repo.toLowerCase()
        buildApp(appType, repo, imageName)
    }

    void buildApp(String appType, String repoName, String imageName) {
        def basePath = "target-repo/${repoName}"

        steps.dir(basePath) {
            switch (appType) {
                case 'springboot':
                    def matches = steps.findFiles(glob: '**/pom.xml')
                    if (!matches || matches.length == 0) {
                        steps.error("❌ pom.xml not found")
                    }

                    def pomPath = matches[0].path.replaceAll('\\\\', '/')
                    def baseDir = pomPath.replaceAll('/pom.xml$', '')

                    steps.echo "📂 Using build context: ${baseDir}"
                    steps.dir(baseDir) {
                        steps.sh 'mvn clean install -DskipTests'
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
        def dockerfile = steps.findFiles(glob: 'Dockerfile')
        if (!dockerfile || dockerfile.length == 0) {
            steps.error("❌ Dockerfile missing.")
        }
    }
}
