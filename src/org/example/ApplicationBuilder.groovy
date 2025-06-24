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
            steps.echo "\uD83D\uDE80 Jenkins Debug Info"
            steps.echo "\uD83D\uDD39 Build: ${steps.env.BUILD_NUMBER}"
            steps.echo "\uD83D\uDD2D Jenkins: ${steps.env.JENKINS_VERSION ?: 'N/A'}"
            steps.echo "\uD83D\uDDA5️ Agent: ${steps.env.NODE_NAME}"
            steps.echo "\uD83C\uDF7F Labels: ${steps.env.NODE_LABELS}"
            steps.echo "\uD83D\uDD17 Git URL: ${steps.env.GIT_URL ?: 'N/A'}"
            steps.echo "\uD83D\uDD4C Commit: ${steps.env.GIT_COMMIT ?: 'N/A'}"
            steps.echo "\uD83D\uDCC1 Workspace: ${steps.env.WORKSPACE}"

            repoName = steps.params.REPO_NAME?.trim()
            if (!repoName) steps.error("\u274C 'REPO_NAME' must be provided")

            def configText = steps.libraryResource('common-repo-list.js')
            steps.writeFile file: 'common-repo-list.js', text: configText

            def parsedMap = parseAndNormalizeJson(configText)
            def appTypeKey = findAppType(repoName, parsedMap)
            if (!appTypeKey) steps.error("\u274C App type for '${repoName}' not found")

            appType = appTypeKey.toLowerCase()
            def isEureka = appType == 'eureka'
            def isNginx  = appType == 'nginx'

            hostPort = isEureka ? '8761' :
                       isNginx  ? findAvailablePort(8081, 9000) :
                                  findAvailablePort(9001, 9010)
            if (!hostPort) steps.error("\u274C No free port found for '${appType}'")

            imageName     = "${repoName.toLowerCase()}-image"
            containerName = "${repoName.toLowerCase()}-container"
            dockerPort    = isEureka ? '8761' : '8080'

            steps.env.APP_TYPE       = appType
            steps.env.PROJECT_DIR    = repoName
            steps.env.IMAGE_NAME     = imageName
            steps.env.CONTAINER_NAME = containerName
            steps.env.DOCKER_PORT    = dockerPort
            steps.env.HOST_PORT      = hostPort
            steps.env.IS_EUREKA      = isEureka.toString()

            def portMsg = isEureka ? "\uD83D\uDD0C Static port 8761 for Eureka" :
                          isNginx  ? "\uD83C\uDF10 Port ${hostPort} for Nginx" :
                                     "\uD83E\uDDEA Port ${hostPort} for ${appType}"

            steps.echo portMsg
            steps.echo "\u2705 Env ready for '${repoName}'"
            steps.echo "\uD83D\uDCF1 HOST_PORT=${hostPort} maps to DOCKER_PORT=${dockerPort}"

            def report = """
==== Build Report ====
\uD83D\uDD39 Build Number: ${steps.env.BUILD_NUMBER}
\uD83D\uDD2D Jenkins: ${steps.env.JENKINS_VERSION ?: 'N/A'}
\uD83D\uDDA5️ Agent: ${steps.env.NODE_NAME}
\uD83C\uDF7F Labels: ${steps.env.NODE_LABELS}
\uD83D\uDD17 Git URL: ${steps.env.GIT_URL ?: 'N/A'}
\uD83D\uDD4C Commit: ${steps.env.GIT_COMMIT ?: 'N/A'}
\uD83D\uDCC1 Workspace: ${steps.env.WORKSPACE}
\uD83D\uDCE6 Repo: ${repoName}
\uD83D\uDEE0️ App Type: ${appType}
\uD83D\uDCF1 Host Port: ${hostPort}
\uD83D\uDD12 Docker Port: ${dockerPort}
${portMsg}
======================
"""
            steps.writeFile file: "build-report.txt", text: report

        } catch (Exception e) {
            steps.error("\u274C initialize() failed: ${e.message}")
        }
    }

    void checkout(String branch = steps.params.REPO_BRANCH ?: 'feature') {
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

    void build(String branch = steps.params.REPO_BRANCH ?: 'feature') {
        buildApp(appType, repoName, imageName)
    }

    void buildApp(String appType, String repoName, String imageName) {
        steps.dir("target-repo/${repoName}") {
            switch (appType) {
                case 'springboot': buildSpringBoot(imageName); break
                case 'nodejs'    : buildNode(imageName); break
                case 'python'    : buildPython(imageName); break
                case 'ruby'      : buildRuby(imageName); break
                case 'nginx':
                case 'php'       : buildStatic(imageName); break
                default: steps.error("\u274C Unsupported app type '${appType}'")
            }
        }
    }

    private void buildSpringBoot(String imageName) {
        def pom = steps.findFiles(glob: '**/pom.xml')[0]?.path
        if (!pom) steps.error("\u274C pom.xml missing")
        def dir = pom.contains('/') ? pom.substring(0, pom.lastIndexOf('/')) : '.'
        steps.dir(dir) {
            runCommand('mvn clean install -DskipTests')
            runCommand('mvn package -DskipTests')
            verifyDockerfile()
            runCommand("docker build -t ${imageName}:latest .")
        }
    }

    private void buildNode(String imageName) {
        runCommand('npm install')
        runCommand('npm run build || echo "\u26A0\uFE0F Skipped build"')
        verifyDockerfile()
        runCommand("docker build -t ${imageName}:latest .")
    }

    private void buildPython(String imageName) {
        runCommand('pip install -r requirements.txt || echo "\u26A0\uFE0F Missing requirements"')
        verifyDockerfile()
        runCommand("docker build -t ${imageName}:latest .")
    }

    private void buildRuby(String imageName) {
        runCommand('bundle install || echo "\u26A0\uFE0F Missing Gemfile or failed"')
        verifyDockerfile()
        runCommand("docker build -t ${imageName}:latest .")
    }

    private void buildStatic(String imageName) {
        verifyDockerfile()
        runCommand("docker build -t ${imageName}:latest .")
    }

    void startMySQLContainer() {
        def mysqlContainerName = "mysql-db"
        steps.sh "docker rm -f ${mysqlContainerName} || true"
        def mysqlRunCmd = """
            docker run --rm --name ${mysqlContainerName} \
            --network spring-net \
            -e MYSQL_ROOT_PASSWORD=root \
            -e MYSQL_DATABASE=world \
            -p 3307:3306 \
            -d mysql:8
        """.stripIndent().trim()
        steps.sh mysqlRunCmd
        steps.echo "\u2705 MySQL container '${mysqlContainerName}' started on host port 3307"
    }

    void runContainer() {
        if (appType == 'springboot') {
            startMySQLContainer()
            steps.sleep 30
        }

        steps.sh "docker stop '${containerName}' || true"
        steps.sh "docker rm '${containerName}' || true"

        def base = steps.sh(script: "find . -name Dockerfile -print -quit", returnStdout: true).trim()?.replaceAll('/Dockerfile$', '')
        if (!base) steps.error("\u274C Dockerfile not found in tree")

        steps.sh "docker build -t '${imageName}:latest' '${base}'"

        def cmd = null
        switch (appType) {
            case 'nginx':
                cmd = "docker run -d --name '${containerName}' --network spring-net -p ${hostPort}:80 '${imageName}:latest'"
                break
            case 'springboot':
                cmd = "docker run -d --name '${containerName}' --add-host=host.docker.internal:host-gateway --network spring-net -p ${hostPort}:${dockerPort} '${imageName}:latest' --server.port=${dockerPort} --server.address=0.0.0.0"
                break
            default:
                steps.error("\u274C runContainer unsupported for '${appType}'")
        }

        steps.sh cmd
        steps.sh "docker ps -a --filter name='${containerName}'"
        steps.sh "docker logs '${containerName}' || true"
    }

    void healthCheck() {
        def endpoint = getHealthEndpoint(appType)
        def url = "http://localhost:${dockerPort}${endpoint}"

        steps.echo "\uD83E\uDEBA Checking health from inside container: ${url}"
        steps.sh "sleep 30"

        steps.sh """
        for i in \$(seq 1 10); do
          CODE=\$(curl -s -o /dev/null -w '%{http_code}' ${url})
          STATUS=\$?
          if [ \$STATUS -eq 0 ] && [[ "\$CODE" =~ ^(200|302|403)\$ ]]; then
              echo "\u2705 Healthy"
              exit 0
          else
              echo "Attempt \$i: HTTP \$CODE (curl status: \$STATUS)"
              echo "\uD83D\uDD0D Logs:"
              docker logs ${containerName} || true
          fi
          sleep 5
        done
        echo "\u274C Health check failed"
        docker logs ${containerName} || true
        exit 1
        """
    }

    private String getHealthEndpoint(String type) {
        switch (type?.toLowerCase()) {
            case 'springboot': return "/actuator/health"
            case 'nodejs':
            case 'nginx':
            case 'php':
            case 'python':
            case 'ruby' : return "/"
            default:
                steps.echo "\u26A0\uFE0F Unknown app type '${type}', using root"
                return "/"
        }
    }

    private void runCommand(String cmd) {
        steps.isUnix() ? steps.sh(cmd) : steps.bat(cmd)
    }

    private void verifyDockerfile() {
        if (!steps.findFiles(glob: 'Dockerfile')) {
            steps.error("\u274C Dockerfile missing")
        }
    }

    @NonCPS
    def parseAndNormalizeJson(String input) {
        def raw = new JsonSlurper().parseText(input)
        def out = [:]
        raw.each { type, list ->
            out[type] = list.collect { it.collectEntries { k, v -> [(k): v.toString()] } }
        }
        return out
    }

    @NonCPS
    def findAppType(String name, Map data) {
        data.find { k, v -> v.find { it['repo-name'] == name } }?.key
    }

    String findAvailablePort(int start, int end) {
        for (int i = start; i <= end; i++) {
            if (steps.sh(script: "netstat -an | findstr :${i}", returnStatus: true) != 0) {
                return i.toString()
            }
        }
        return null
    }

    void cleanWorkspace() {
        steps.cleanWs()
    }
}
