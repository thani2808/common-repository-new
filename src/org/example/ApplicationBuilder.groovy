package org.example

import groovy.json.JsonSlurper

class ApplicationBuilder implements Serializable {
    def steps

    String repoName
    String appType
    String imageName
    String containerName
    String dockerPort
    String hostPort

    ApplicationBuilder(steps) {
        this.steps = steps
    }

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

            def envStage = steps.params.ENV_STAGE ?: 'dev'

            steps.env.APP_TYPE = appType
            steps.env.PROJECT_DIR = repoName
            steps.env.IMAGE_NAME = imageName
            steps.env.CONTAINER_NAME = containerName
            steps.env.DOCKER_PORT = dockerPort
            steps.env.IS_EUREKA = isEureka.toString()
            steps.env.HOST_PORT = hostPort
            steps.env.ENV_STAGE = envStage

            steps.echo "✅ Environment initialized for '${repoName}' in '${envStage}' mode"
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
        steps.withEnv(['GIT_CURL_VERBOSE=1']) {
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
    }

    void preRunDebug() {
        steps.echo "🔧 Pre-Run – env.APP_TYPE       = '${steps.env.APP_TYPE}'"
        steps.echo "🔧 Pre-Run – env.IMAGE_NAME     = '${steps.env.IMAGE_NAME}'"
        steps.echo "🔧 Pre-Run – env.CONTAINER_NAME = '${steps.env.CONTAINER_NAME}'"

        if (!steps.env.APP_TYPE) {
            steps.error "❌ Pre-Run check failed: APP_TYPE is null or not initialized!"
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
        steps.dir(pomDir) {
            runCommand('mvn clean install -DskipTests')
            runCommand('mvn package -DskipTests')
            checkDockerfileExists()
            runCommand("docker build -t ${imageName}:latest .")
        }
    }

    private void buildNodeApp(String imageName) {
        runCommand('npm install')
        runCommand('npm run build || echo \"⚠️ No build step defined.\"')
        checkDockerfileExists()
        runCommand("docker build -t ${imageName}:latest .")
    }

    private void buildPythonApp(String imageName) {
        runCommand('pip install -r requirements.txt || echo \"⚠️ No requirements.txt or install failed.\"')
        checkDockerfileExists()
        runCommand("docker build -t ${imageName}:latest .")
    }

    private void buildRubyApp(String imageName) {
        runCommand('bundle install || echo \"⚠️ bundle install failed or Gemfile missing.\"')
        checkDockerfileExists()
        runCommand("docker build -t ${imageName}:latest .")
    }

    private void buildStaticApp(String imageName, String appType) {
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
        steps.echo "🔍 Checking MySQL containers and volume..."

        steps.sh '''
            docker stop mysql-db || true
            docker rm mysql-db || true

            echo "📦 Ensuring MySQL volume exists"
            if ! docker volume ls --format '{{.Name}}' | grep -q '^mysql-db-data$'; then
              docker volume create mysql-db-data
            fi

            echo "🚀 Starting fresh MySQL container on host port 3306"
            docker run -d --name mysql-db \
                --network spring-net \
		-e MYSQL_ROOT_PASSWORD=Thani@01 \
                -v mysql-db-data:/var/lib/mysql \
                mysql:8
        '''
    }

    void runContainer() {
        if (!containerName || !imageName || !hostPort || !dockerPort || !appType)
            steps.error("❌ Missing required parameters.")

        steps.sh "docker stop '${containerName}' || true"
        steps.sh "docker rm '${containerName}' || true"

        def runCommand = ""

        if (appType == "springboot") {
            runCommand = """
                docker run -d --name ${containerName} \\
                  --network spring-net \\
                  -p ${hostPort}:8080 \\
                  ${imageName}:latest \\
                  --server.port=${dockerPort} \\
                  --server.address=0.0.0.0 \\
                  --spring.datasource.url=jdbc:mysql://host.docker.internal:3306/world \\
                  --spring.datasource.username=root \\
                  --spring.datasource.password=Thani@01 \\
                  --spring.jpa.hibernate.ddl-auto=update
            """
        } else if (appType == "nginx") {
            runCommand = """
                docker run -d --name ${containerName} \\
                  --network spring-net \\
                  -p ${hostPort}:80 \\
                  ${imageName}:latest
            """
        } else {
            steps.error("❌ Unsupported appType: ${appType}")
        }

        steps.sh(runCommand)
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
