import groovy.json.JsonSlurper

def call(Map args = [:]) {
    def repoKey = args.REPO_NAME ?: env.REPO_NAME
    def branch  = args.COMMON_REPO_BRANCH ?: 'main'
    env.REPO_NAME   = repoKey
    env.REPO_BRANCH = branch

    def filePath = "resources/common-repo-list.js"
    if (!fileExists(filePath)) {
        error "❌ Required config file not found: ${filePath}"
    }

    def raw = readFile(filePath)
    def jsonText = raw.replaceFirst(/(?s).*?=\s*/, '').replaceAll(/;$/, '')
    def configList = new JsonSlurper().parseText(jsonText)

    def repoConfig = configList.find { it["repo-name"] == repoKey }
    if (!repoConfig) {
        error "❌ Repo '${repoKey}' not found in common-repo-list.js"
    }

    // Set environment variables
    env.APP_TYPE       = repoConfig["app-type"]?.toLowerCase() ?: 'springboot'
    env.PROJECT_DIR    = repoConfig["project_dir"] ?: '.'
    env.DOCKER_PORT    = repoConfig["host_port"]?.toString() ?: '8080'
    env.IMAGE_NAME     = "${repoKey.toLowerCase()}-image"
    env.CONTAINER_NAME = "${repoKey.toLowerCase()}-container"
    env.GIT_CREDENTIALS_ID = repoConfig["git_credentials_id"] ?: ''
    env.REPO_URL       = repoConfig["git-url"] ?: ''
    env.IS_EUREKA      = (repoConfig["app-type"] == "eureka").toString()

    echo "📦 Initialized Environment:"
    echo "   - REPO_NAME      = ${env.REPO_NAME}"
    echo "   - REPO_BRANCH    = ${env.REPO_BRANCH}"
    echo "   - REPO_URL       = ${env.REPO_URL}"
    echo "   - APP_TYPE       = ${env.APP_TYPE}"
    echo "   - PROJECT_DIR    = ${env.PROJECT_DIR}"
    echo "   - IMAGE_NAME     = ${env.IMAGE_NAME}"
    echo "   - CONTAINER_NAME = ${env.CONTAINER_NAME}"
    echo "   - DOCKER_PORT    = ${env.DOCKER_PORT}"
    echo "   - IS_EUREKA      = ${env.IS_EUREKA}"
}
