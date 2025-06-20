package org.example

class ApplicationBuilder implements Serializable {
    def steps

    ApplicationBuilder(steps) {
        this.steps = steps
    }

    void build(String repo, String branch) {
        steps.echo "⚙️ build(repo, branch) invoked"
        def appType = steps.env.APP_TYPE?.toLowerCase() ?: 'springboot'
        def imageName = steps.env.IMAGE_NAME ?: repo.toLowerCase()
        buildApp(appType, repo, imageName)
    }

    void buildApp(String appType, String repoName, String imageName) {
        def basePath = "target-repo/${repoName}"
        steps.dir(basePath) {
            switch (appType) {
                case 'springboot':
                    buildSpringBootApp(imageName)
private void buildSpringBootApp(String imageName) {
        def matches = steps.findFiles(glob: '**/pom.xml')
        if (!matches || matches.length == 0) {
            steps.error("❌ pom.xml not found in project.")
        }

        def pomPath = matches[0].path.replaceAll('\\\\', '/')
        def pomDir = pomPath.contains('/') ? pomPath.substring(0, pomPath.lastIndexOf('/')) : '.'
        steps.echo "📂 Spring Boot context: ${pomDir}"

        steps.dir(pomDir) {
            runCommand('mvn clean install -DskipTests')
            runCommand('mvn package -DskipTests')
            checkDockerfileExists()
            runCommand("docker build -t ${imageName}:latest .")
        }
    }														  
                    break
                case 'nodejs':
                    buildNodeApp(imageName)
                    break
                case 'python':
                    buildPythonApp(imageName)
                    break
                case 'ruby':
                    buildRubyApp(imageName)
                    break
                case 'nginx':
                case 'php':
                    buildStaticApp(imageName, appType)
                    break
                default:
                    steps.error("❌ Unsupported app type: ${appType}")
            }
        }
    }

    private void buildSpringBootApp(String imageName) {
        def matches = steps.findFiles(glob: '**/pom.xml')
        if (!matches || matches.length == 0) {
            steps.error("❌ pom.xml not found in project.")
        }

        def pomPath = matches[0].path.replaceAll('\\\\', '/')
        def pomDir = pomPath.contains('/') ? pomPath.substring(0, pomPath.lastIndexOf('/')) : '.'
        steps.echo "📂 Spring Boot context: ${pomDir}"

        steps.dir(pomDir) {
            runCommand('mvn clean install -DskipTests')
            runCommand('mvn package -DskipTests')
            checkDockerfileExists()
            runCommand("docker build -t ${imageName}:latest .")
        }
    }

    private void buildNodeApp(String imageName) {
        steps.echo "📦 Node.js build"
        runCommand('npm install')
        runCommand('npm run build || echo "⚠️ No build step defined."')
        checkDockerfileExists()
        runCommand("docker build -t ${imageName}:latest .")
    }

    private void buildPythonApp(String imageName) {
        steps.echo "🐍 Python app build"
        runCommand('pip install -r requirements.txt || echo "⚠️ No requirements.txt or install failed."')
        checkDockerfileExists()
        runCommand("docker build -t ${imageName}:latest .")
    }

    private void buildRubyApp(String imageName) {
        steps.echo "💎 Ruby app build"
        runCommand('bundle install || echo "⚠️ bundle install failed or Gemfile missing."')
        checkDockerfileExists()
        runCommand("docker build -t ${imageName}:latest .")
    }

    private void buildStaticApp(String imageName, String appType) {
        steps.echo "ℹ️ No build steps for static ${appType} app. Verifying Dockerfile..."
        checkDockerfileExists()
        runCommand("docker build -t ${imageName}:latest .")
    }

    private void checkDockerfileExists() {
        def dockerfile = steps.findFiles(glob: 'Dockerfile')
        if (!dockerfile || dockerfile.length == 0) {
            steps.error("❌ Dockerfile missing.")
        }
    }

    private void runCommand(String command) {
        if (steps.isUnix()) {
            steps.sh command
        } else {
            steps.bat command
        }
    }
}
