package org.example

import groovy.json.JsonSlurper

class ApplicationBuilder implements Serializable {
    def steps

    // ✅ Reusable common variables
    String repoName
    String appType
    String imageName
    String containerName
    String dockerPort
    String hostPort

    ApplicationBuilder(steps) {
        this.steps = steps
    }

    // ========== InitEnv Logic ==========
    def initialize() {
        try {
            repoName = steps.params.REPO_NAME
            if (!repoName?.trim()) steps.error("❌ 'REPO_NAME' must be provided.")

            def configText = steps.libraryResource("common-repo-list.js")
            steps.writeFile(file: "common-repo-list.js", text: configText)

            def parsedMap = parseAndNormalizeJson(configText)
            def appTypeKey = findAppType(repoName, parsedMap)
            if (!appTypeKey) steps.error("❌ Repository '${repoName}' not found.")

            appType = appTypeKey.toLowerCase()
            def isEureka = (appType == 'eureka')
            def isNginx = (appType == 'nginx')

            hostPort = isEureka ? '8761' : isNginx ? '8081' : findAvailablePort(9001, 9010)
            if (!hostPort) steps.error("❌ No available port found between 9001–9010.")

            imageName = "${repoName.toLowerCase()}-image"
            containerName = "${repoName.toLowerCase()}-container"
            dockerPort = isEureka ? '8761' : '8080'

            steps.env.APP_TYPE = appType
            steps.env.PROJECT_DIR = repoName
            steps.env.IMAGE_NAME = imageName
            steps.env.CONTAINER_NAME = containerName
            steps.env.DOCKER_PORT = dockerPort
            steps.env.IS_EUREKA = isEureka.toString()
            steps.env.HOST_PORT = hostPort

            steps.echo "✅ Environment initialized for '${repoName}'"

        } catch (Exception e) {
            steps.error("❌ InitEnv failed: ${e.message ?: e.toString()}")
        }
    }

    @NonCPS
    def parseAndNormalizeJson(String configText) {
        def raw = new JsonSlurper().parseText(configText)
        def normalized = [:]
        raw.each { type, list ->
            normalized[type] = list.collect { item ->
                item.collectEntries { k, v -> [(k): v.toString()] }
            }
        }
        return normalized
    }

    @NonCPS
    def findAppType(String repoName, Map parsedMap) {
        parsedMap.find { type, repos ->
            repos.find { it['repo-name'] == repoName }
        }?.key
    }

    String findAvailablePort(int start, int end) {
        for (int port = start; port <= end; port++) {
            if (steps.sh(script: "netstat -an | findstr :${port}", returnStatus: true) != 0) {
                return port.toString()
            }
        }
        return null
    }

    void cleanWorkspace() {
        steps.cleanWs()
    }

    void checkout(String branch = 'feature') {
        steps.checkout([
            $class: 'GitSCM',
            branches: [[name: "*/${branch}"]],
            extensions: [[
                $class: 'RelativeTargetDirectory',
                relativeTargetDir: "target-repo/${repoName}"
            ]],
            userRemoteConfigs: [[
                url: "git@github.com:thani2808/${repoName}.git",
                credentialsId: 'private-key-jenkins',
                refspec: "+refs/heads/${branch}:refs/remotes/origin/${branch}"
            ]]
        ])
    }

    void preRunDebug() {
        steps.echo "🔧 Pre-Run – env.APP_TYPE       = '${steps.env.APP_TYPE}'"
        steps.echo "🔧 Pre-Run – env.IMAGE_NAME     = '${steps.env.IMAGE_NAME}'"
        steps.echo "🔧 Pre-Run – env.CONTAINER_NAME = '${steps.env.CONTAINER_NAME}'"

        if (!steps.env.APP_TYPE) {
            steps.error "❌ Pre-Run check failed: APP_TYPE still null!"
        }
    }

    void build(String branch) {
        steps.echo "⚙️ build(repo, branch) invoked"
        buildApp(appType, repoName, imageName)
    }

    void buildApp(String appType, String repoName, String imageName) {
        def basePath = "target-repo/${repoName}"
        steps.dir(basePath) {
            switch (appType) {
                case 'springboot': buildSpringBootApp(imageName); break
                case 'nodejs':     buildNodeApp(imageName); break
                case 'python':     buildPythonApp(imageName); break
                case 'ruby':       buildRubyApp(imageName); break
                case 'nginx':
                case 'php':        buildStaticApp(imageName, appType); break
                default:           steps.error("❌ Unsupported app type: ${appType}")
            }
        }
    }

    private void buildSpringBootApp(String imageName) {
        def matches = steps.findFiles(glob: '**/pom.xml')
        if (!matches) steps.error("❌ pom.xml not found in project.")

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
        runCommand('npm run build || echo \"⚠️ No build step defined.\"')
        checkDockerfileExists()
        runCommand("docker build -t ${imageName}:latest .")
    }

    private void buildPythonApp(String imageName) {
        steps.echo "🐍 Python app build"
        runCommand('pip install -r requirements.txt || echo \"⚠️ No requirements.txt or install failed.\"')
        checkDockerfileExists()
        runCommand("docker build -t ${imageName}:latest .")
    }

    private void buildRubyApp(String imageName) {
        steps.echo "💎 Ruby app build"
        runCommand('bundle install || echo \"⚠️ bundle install failed or Gemfile missing.\"')
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
        if (!dockerfile) steps.error("❌ Dockerfile missing.")
    }

    private void runCommand(String command) {
        if (steps.isUnix()) steps.sh command else steps.bat command
    }

    void startMySQLContainer() {
        def mysqlContainerName = "mysql-db"
        steps.echo "🛠 Checking if MySQL container '${mysqlContainerName}' exists..."
        def runCmd = """
            if ! docker ps -a --format '{{.Names}}' | grep -q '^${mysqlContainerName}\$'; then
              echo "📦 Creating new MySQL container..."
              docker run -d --name ${mysqlContainerName} --network spring-net \\
                -e MYSQL_ROOT_PASSWORD=Thani@01 \\
                -e MYSQL_DATABASE=world \\
		-v mysql-db-data:/var/lib/mysql \\
                mysql:8
            else
              echo "✅ ${mysqlContainerName} already exists."
              docker start ${mysqlContainerName}
            fi
        """
        steps.sh(runCmd)
    }

    void runContainer() {
        if (!containerName || !imageName || !hostPort || !dockerPort || !appType)
            steps.error("❌ Missing required parameters.")

        // ✅ Start MySQL if needed
        startMySQLContainer()

        def contextDir = steps.sh(script: "find . -name Dockerfile -print -quit", returnStdout: true).trim()?.replaceAll('/Dockerfile$', '')
        if (!contextDir) steps.error("❌ Dockerfile not found.")

        steps.sh "docker stop '${containerName}' || true"
        steps.sh "docker rm '${containerName}' || true"
        steps.sh "docker build -t '${imageName}:latest' '${contextDir}'"

        switch (appType) {
            case 'nginx':
                steps.sh """
                    docker run -d --name ${containerName} \
                      --network spring-net \
                      -p ${hostPort}:80 \
                      ${imageName}:latest
                """
                break
            case 'springboot':
                steps.sh """
                    docker run -d --name ${containerName} \
                      --network spring-net \
                      -p ${hostPort}:8080 \
                      ${imageName}:latest \
                      --server.port=${dockerPort} \
                      --server.address=0.0.0.0 \
                      --spring.datasource.url=jdbc:mysql://mysql-db:3306/world \
                      --spring.datasource.username=root \
                      --spring.datasource.password=Thani@01 \
                      --spring.jpa.hibernate.ddl-auto=update
                """
                break
            default:
                steps.error("❌ Unsupported appType '${appType}'. Supported: springboot, nginx")
        }
    }

    void healthCheck() {
        def endpoint = getHealthEndpoint(appType)
        def url = "http://localhost:${hostPort}${endpoint}"

        steps.echo "⏳ Starting health check for '${appType}' app on ${url}"
        steps.sh "sleep 15"

        steps.sh """
            for i in \$(seq 1 10); do
                CODE=\$(curl -s -o /dev/null -w '%{http_code}' ${url} || echo 000)
                echo "Attempt \$i: HTTP \$CODE"
                if [[ "\$CODE" == "200" || "\$CODE" == "403" || "\$CODE" == "302" ]]; then
                    echo "✅ Health check passed with code \$CODE"
                    exit 0
                fi
                sleep 3
            done
            echo "❌ Health check failed for ${containerName} (${appType})"
            docker logs ${containerName} || true
            exit 1
        """
    }

    private String getHealthEndpoint(String appType) {
        switch (appType?.toLowerCase()) {
            case 'springboot': return "/actuator/health"
            case 'nodejs':
            case 'nginx':
            case 'php':
            case 'python':
            case 'ruby': return "/"
            default:
                steps.echo "⚠️ Unknown app type '${appType}', defaulting to root endpoint"
                return "/"
        }
    }
}
