import groovy.json.JsonSlurper

def call(String imageName, String dummyAppType = 'springboot') {
    imageName = imageName.toLowerCase()

    def filePath = "resources/common-repo-list.js"
    if (!fileExists(filePath)) {
        error "❌ Required config file not found: ${filePath}"
    }

    def raw = readFile(filePath)
    def jsonText = raw.replaceFirst(/(?s).*?=\s*/, '').replaceAll(/;$/, '')
    def config = new JsonSlurper().parseText(jsonText)

    def repoKey = env.REPO_NAME
    if (!config.containsKey(repoKey)) {
        error "❌ Repo '${repoKey}' not found in common-repo-list.js"
    }

    def repoConfig = config[repoKey]

    // Dynamically set appType from config
    def appType = repoConfig["app-type"]?.toLowerCase() ?: 'springboot'
    env.APP_TYPE       = appType
    env.DOCKER_PORT    = repoConfig.port.toString()
    env.CONTAINER_NAME = "${repoKey}-container"
    env.IMAGE_NAME     = "${repoKey}-image"
    env.PROJECT_DIR    = repoConfig.project_dir ?: '.'

    echo "📦 Updated repo config from common-repo-list.js:"
    echo "  - APP_TYPE       = ${env.APP_TYPE}"
    echo "  - IMAGE_NAME     = ${env.IMAGE_NAME}"
    echo "  - CONTAINER_NAME = ${env.CONTAINER_NAME}"
    echo "  - DOCKER_PORT    = ${env.DOCKER_PORT}"
    echo "  - PROJECT_DIR    = ${env.PROJECT_DIR}"

    // Build Docker image based on app type
    dir('target-repo') {
        def buildDir = '.'

        if (appType == 'springboot') {
            def pomPath = sh(script: "find . -name pom.xml | head -1", returnStdout: true).trim()
            buildDir = pomPath ? pomPath.replaceFirst('/pom.xml$', '') : '.'
        } else if (appType in ['nodejs', 'php']) {
            def defaultDirs = ['.', 'src', 'web', 'html']
            def foundDir = defaultDirs.find { dirName -> fileExists("${dirName}/Dockerfile") }
            buildDir = foundDir ?: '.'
        } else if (appType == 'nginx') {
            buildDir = env.PROJECT_DIR
        } else {
            error "❌ Unsupported app type: ${appType}"
        }

        dir(buildDir) {
            echo "🚧 Building Docker image '${imageName}' from ${pwd()}"
            sh "docker build -t ${imageName} ."
        }
    }
}
