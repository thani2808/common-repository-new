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

    def initialize() {
        try {
            // === 🐛 Startup Debug Info ===
            steps.echo "\uD83D\uDE80 Jenkins Debug Info"
            steps.echo "\uD83D\uDD67 Jenkins Build Number: ${steps.env.BUILD_NUMBER}"
            steps.echo "\uD83E\uDDF1 Jenkins Version: ${steps.env.JENKINS_VERSION ?: 'Unavailable (may need global env setting)'}"
            steps.echo "\uD83D\uDCA5 Agent Name: ${steps.env.NODE_NAME}"
            steps.echo "\uD83C\uDFF7️ Node Labels: ${steps.env.NODE_LABELS}"
            steps.echo "\uD83D\uDD17 Git URL: ${steps.env.GIT_URL ?: 'Not available in env'}"
            steps.echo "\uD83D\uDCCC Git Commit: ${steps.env.GIT_COMMIT ?: 'Not available in env'}"
            steps.echo "\uD83D\uDCC1 Workspace: ${steps.env.WORKSPACE}"

            repoName = steps.params.REPO_NAME
            if (!repoName?.trim()) steps.error("\u274C 'REPO_NAME' must be provided.")

            def configText = steps.libraryResource("common-repo-list.js")
            steps.writeFile(file: "common-repo-list.js", text: configText)

            def parsedMap = parseAndNormalizeJson(configText)
            def appTypeKey = findAppType(repoName, parsedMap)
            if (!appTypeKey) steps.error("\u274C Repository '${repoName}' not found.")

            appType = appTypeKey.toLowerCase()
            def isEureka = (appType == 'eureka')
            def isNginx  = (appType == 'nginx')

            if (isEureka) {
                hostPort = '8761'
            } else if (isNginx) {
                hostPort = findAvailablePort(8081, 9000)
            } else {
                hostPort = findAvailablePort(9001, 9010)
            }

            if (!hostPort) steps.error("\u274C No available port found for appType: ${appType}")

            def portMessage = isEureka
                ? "\uD83D\uDD0C Reserved static port 8761 for Eureka Server"
                : isNginx
                    ? "\uD83C\uDF10 Assigned available port ${hostPort} for Nginx reverse proxy"
                    : "\uD83E\uDDEA Assigned available port ${hostPort} for ${appType} application"

            steps.echo(portMessage)

            imageName     = "${repoName.toLowerCase()}-image"
            containerName = "${repoName.toLowerCase()}-container"
            dockerPort    = isEureka ? '8761' : '8080'

            // Export to env
            steps.env.APP_TYPE       = appType
            steps.env.PROJECT_DIR    = repoName
            steps.env.IMAGE_NAME     = imageName
            steps.env.CONTAINER_NAME = containerName
            steps.env.DOCKER_PORT    = dockerPort
            steps.env.IS_EUREKA      = isEureka.toString()
            steps.env.HOST_PORT      = hostPort

            steps.echo "\u2705 Environment initialized for '${repoName}'"
            steps.echo "\uD83D\uDCE1 HOST_PORT=${hostPort} will map to internal DOCKER_PORT=${dockerPort}"

            // === \uD83D\uDCC4 Build Report File ===
            def reportText = """
==== Build Report ====
\uD83D\uDD67 Jenkins Build Number: ${steps.env.BUILD_NUMBER}
\uD83E\uDDF1 Jenkins Version: ${steps.env.JENKINS_VERSION ?: 'Unavailable'}
\uD83D\uDCA5 Agent: ${steps.env.NODE_NAME}
\uD83C\uDFF7️ Labels: ${steps.env.NODE_LABELS}
\uD83D\uDD17 Git URL: ${steps.env.GIT_URL ?: 'N/A'}
\uD83D\uDCCC Git Commit: ${steps.env.GIT_COMMIT ?: 'N/A'}
\uD83D\uDCC1 Workspace: ${steps.env.WORKSPACE}
\uD83D\uDCE6 Repo: ${repoName}
\uD83D\uDEE0️ App Type: ${appType}
\uD83D\uDCE1 Host Port: ${hostPort}
\uD83D\uDD12 Docker Port: ${dockerPort}
${portMessage}
=======================
"""
            steps.writeFile file: 'build-report.txt', text: reportText

        } catch (Exception e) {
            steps.error("\u274C InitEnv failed: ${e.message ?: e.toString()}")
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
                default:           steps.error("\u274C Unsupported app type: ${appType}")
            }
        }
    }

    private void buildSpringBootApp(String imageName) {
        def matches = steps.findFiles(glob: '**/pom.xml')
        if (!matches) steps.error("\u274C pom.xml not found in project.")

        def pomPath = matches[0].path.replaceAll('\\\\', '/')
        def pomDir = pomPath.contains('/') ? pomPath.substring(0, pomPath.lastIndexOf('/')) : '.'
        steps.echo "\uD83D\uDCC2 Spring Boot context: ${pomDir}"

        steps.dir(pomDir) {
            runCommand('mvn clean install -DskipTests')
            runCommand('mvn package -DskipTests')
            checkDockerfileExists()
            runCommand("docker build -t ${imageName}:latest .")
        }
    }

    private void buildNodeApp(String imageName) {
        steps.echo "\uD83D\uDCE6 Node.js build"
        runCommand('npm install')
        runCommand('npm run build || echo \"\u26A0\uFE0F No build step defined.\"')
        checkDockerfileExists()
        runCommand("docker build -t ${imageName}:latest .")
    }

    private void buildPythonApp(String imageName) {
        steps.echo "\uD83D\uDC0D Python app build"
        runCommand('pip install -r requirements.txt || echo \"\u26A0\uFE0F No requirements.txt or install failed.\"')
        checkDockerfileExists()
        runCommand("docker build -t ${imageName}:latest .")
    }

    private void buildRubyApp(String imageName) {
        steps.echo "\uD83D\uDC8E Ruby app build"
        runCommand('bundle install || echo \"\u26A0\uFE0F bundle install failed or Gemfile missing.\"')
        checkDockerfileExists()
        runCommand("docker build -t ${imageName}:latest .")
    }

    private void buildStaticApp(String imageName, String appType) {
        steps.echo "\u2139\uFE0F No build steps for static ${appType} app. Verifying Dockerfile..."
        checkDockerfileExists()
        runCommand("docker build -t ${imageName}:latest .")
    }

    private void checkDockerfileExists() {
        def dockerfile = steps.findFiles(glob: 'Dockerfile')
        if (!dockerfile) steps.error("\u274C Dockerfile missing.")
    }

    private void runCommand(String command) {
        if (steps.isUnix()) steps.sh command else steps.bat command
    }

    void runContainer() {
        if (!containerName || !imageName || !hostPort || !dockerPort || !appType)
            steps.error("\u274C Missing required parameters.")

        def contextDir = steps.sh(script: "find . -name Dockerfile -print -quit", returnStdout: true).trim()?.replaceAll('/Dockerfile$', '')
        if (!contextDir) steps.error("\u274C Dockerfile not found.")

        steps.sh "docker stop '${containerName}' || true"
        steps.sh "docker rm '${containerName}' || true"
        steps.sh "docker build -t '${imageName}:latest' '${contextDir}'"

        switch (appType) {
            case 'nginx':
                steps.sh """
                    docker run -d --name '${containerName}' \
                      --network spring-net \
                      -p ${hostPort}:80 \
                      '${imageName}:latest'
                """
                break
            case 'springboot':
                steps.sh """
                    docker run -d --name '${containerName}' \
                      --network spring-net \
                      -p ${hostPort}:8080 \
                      '${imageName}:latest' \
                      --server.port=${dockerPort} --server.address=0.0.0.0
                """
                break
            default:
                steps.error("\u274C Unsupported appType '${appType}'. Supported: springboot, nginx")
        }
    }

    void healthCheck() {
        def endpoint = getHealthEndpoint(appType)
        def url = "http://localhost:${hostPort}${endpoint}"

        steps.echo "\u23F3 Starting health check for '${appType}' app on ${url}"
        steps.sh "sleep 15"

        steps.sh """
            for i in \$(seq 1 10); do
                CODE=\$(curl -s -o /dev/null -w '%{http_code}' ${url} || echo 000)
                echo "Attempt \$i: HTTP \$CODE"
                if [[ "\$CODE" == "200" || "\$CODE" == "403" || "\$CODE" == "302" ]]; then
                    echo "\u2705 Health check passed with code \$CODE"
                    exit 0
                fi
                sleep 3
            done
            echo "\u274C Health check failed for ${containerName} (${appType})"
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
                steps.echo "\u26A0\uFE0F Unknown app type '${appType}', defaulting to root endpoint"
                return "/"
        }
    }
}
