package org.example

import groovy.json.JsonSlurper

class ApplicationBuilder implements Serializable {
    def steps

    // Common variables
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
            steps.echo "🚀 Jenkins Debug Info"
            steps.echo "🔢 Build: ${steps.env.BUILD_NUMBER}"
            steps.echo "🧭 Jenkins: ${steps.env.JENKINS_VERSION ?: 'N/A'}"
            steps.echo "🖥️ Agent: ${steps.env.NODE_NAME}"
            steps.echo "🏷️ Labels: ${steps.env.NODE_LABELS}"
            steps.echo "🔗 Git URL: ${steps.env.GIT_URL ?: 'N/A'}"
            steps.echo "📌 Commit: ${steps.env.GIT_COMMIT ?: 'N/A'}"
            steps.echo "📁 Workspace: ${steps.env.WORKSPACE}"

            repoName = steps.params.REPO_NAME?.trim()
            if (!repoName) steps.error("❌ 'REPO_NAME' must be provided")

            def configText = steps.libraryResource('common-repo-list.js')
            steps.writeFile file: 'common-repo-list.js', text: configText

            def parsedMap = parseAndNormalizeJson(configText)
            def appTypeKey = findAppType(repoName, parsedMap)
            if (!appTypeKey) steps.error("❌ App type for '${repoName}' not found")

            appType = appTypeKey.toLowerCase()
            def isEureka = (appType == 'eureka')
            def isNginx  = (appType == 'nginx')

            hostPort = isEureka ? '8761' :
                       isNginx  ? findAvailablePort(8081, 9000) :
                                  findAvailablePort(9001, 9010)

            if (!hostPort) steps.error("❌ No free port found for '${appType}'")

            imageName     = "${repoName.toLowerCase()}-image"
            containerName = "${repoName.toLowerCase()}-container"
            dockerPort    = isEureka ? '8761' : '8080'

            // Export to env
            steps.env.APP_TYPE       = appType
            steps.env.PROJECT_DIR    = repoName
            steps.env.IMAGE_NAME     = imageName
            steps.env.CONTAINER_NAME = containerName
            steps.env.DOCKER_PORT    = dockerPort
            steps.env.HOST_PORT      = hostPort
            steps.env.IS_EUREKA      = isEureka.toString()

            def portMsg = isEureka ? "🔌 Static port 8761 for Eureka" :
                          isNginx  ? "🌐 Port ${hostPort} for Nginx" :
                                     "🧪 Port ${hostPort} for ${appType}"

            steps.echo portMsg
            steps.echo "✅ Env ready for '${repoName}'"
            steps.echo "📡 HOST_PORT=${hostPort} maps to DOCKER_PORT=${dockerPort}"

            def report = """
==== Build Report ====
🔢 Build Number: ${steps.env.BUILD_NUMBER}
🧭 Jenkins: ${steps.env.JENKINS_VERSION ?: 'N/A'}
🖥️ Agent: ${steps.env.NODE_NAME}
🏷️ Labels: ${steps.env.NODE_LABELS}
🔗 Git URL: ${steps.env.GIT_URL ?: 'N/A'}
📌 Commit: ${steps.env.GIT_COMMIT ?: 'N/A'}
📁 Workspace: ${steps.env.WORKSPACE}
📦 Repo: ${repoName}
🛠️ App Type: ${appType}
📡 Host Port: ${hostPort}
🔒 Docker Port: ${dockerPort}
${portMsg}
======================
"""
            steps.writeFile file: "build-report.txt", text: report

        } catch (Exception e) {
            steps.error("❌ initialize() failed: ${e.message}")
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
                case 'nodejs':     buildNode(imageName); break
                case 'python':     buildPython(imageName); break
                case 'ruby':       buildRuby(imageName); break
                case 'nginx':
                case 'php':        buildStatic(imageName); break
                default:           steps.error("❌ Unsupported app type '${appType}'")
            }
        }
    }

    private void buildSpringBoot(String imageName) {
        def pom = steps.findFiles(glob: '**/pom.xml')[0]?.path
        if (!pom) steps.error("❌ pom.xml missing")

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
        runCommand('npm run build || echo "⚠️ Skipped build"')
        verifyDockerfile()
        runCommand("docker build -t ${imageName}:latest .")
    }

    private void buildPython(String imageName) {
        runCommand('pip install -r requirements.txt || echo "⚠️ Missing requirements"')
        verifyDockerfile()
        runCommand("docker build -t ${imageName}:latest .")
    }

    private void buildRuby(String imageName) {
        runCommand('bundle install || echo "⚠️ Missing Gemfile or failed"')
        verifyDockerfile()
        runCommand("docker build -t ${imageName}:latest .")
    }

    private void buildStatic(String imageName) {
        verifyDockerfile()
        runCommand("docker build -t ${imageName}:latest .")
    }

    void runContainer() {
        steps.sh "docker stop '${containerName}' || true"
        steps.sh "docker rm '${containerName}' || true"

        def base = steps.sh(script: "find . -name Dockerfile -print -quit", returnStdout: true).trim()?.replaceAll('/Dockerfile$', '')
        if (!base) steps.error("❌ Dockerfile not found in tree")

        steps.sh "docker build -t '${imageName}:latest' '${base}'"

        switch (appType) {
            case 'nginx':
                steps.sh "docker run -d --name '${containerName}' --network spring-net -p ${hostPort}:80 '${imageName}:latest'"
                break
            case 'springboot':
                steps.sh "docker run -d --name '${containerName}' --network spring-net -p ${hostPort}:${dockerPort} '${imageName}:latest' --server.port=${dockerPort} --server.address=0.0.0.0"
                break
            default:
                steps.error("❌ runContainer unsupported for '${appType}'")
        }
	
	// ✅ Debug the container status and logs right after running it
	steps.sh "docker ps -a --filter name='${containerName}'"
        steps.sh "docker logs '${containerName}' || true"
    }

    void healthCheck() {
        def endpoint = getHealthEndpoint(appType)
        def url = "http://localhost:${hostPort}${endpoint}"

        steps.echo "🩺 Checking ${url}"
        steps.sh "sleep 20"

        steps.sh """
        for i in \$(seq 1 10); do
            CODE=\$(curl -s -o /dev/null -w '%{http_code}' ${url})
            STATUS=\$?
            if [ \$STATUS -eq 0 ] && [[ "\$CODE" =~ ^(200|302|403)\$ ]]; then
                echo "✅ Healthy"
                exit 0
            else
                echo "Attempt \$i: HTTP \$CODE (curl status: \$STATUS)"
            fi
            sleep 3
        done
        echo "❌ Health check failed"
        docker logs ${containerName} || true
        exit 1
        """
    }

    // === Utility & Helpers ===

    private String getHealthEndpoint(String type) {
        switch (type?.toLowerCase()) {
            case 'springboot': return "/actuator/health"
            case 'nodejs':
            case 'nginx':
            case 'php':
            case 'python':
            case 'ruby': return "/"
            default:
                steps.echo "⚠️ Unknown app type '${type}', using root"
                return "/"
        }
    }

    private void runCommand(String cmd) {
        steps.isUnix() ? steps.sh(cmd) : steps.bat(cmd)
    }

    private void verifyDockerfile() {
        if (!steps.findFiles(glob: 'Dockerfile')) {
            steps.error("❌ Dockerfile missing")
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
