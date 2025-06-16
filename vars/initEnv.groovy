import groovy.json.JsonSlurper
import org.example.CommonConfig

def call(Map args = [:]) {
    def repoName = args.REPO_NAME?.trim()
    def branchName = args.COMMON_REPO_BRANCH?.trim()

    if (!repoName || !branchName) {
        error "❌ REPO_NAME or COMMON_REPO_BRANCH is empty."
    }

    echo "📦 Initializing for repo: ${repoName}, branch: ${branchName}"
    env.REPO_NAME = repoName
    env.REPO_BRANCH = branchName
    env.REPO_URL = "git@github.com:thani2808/${repoName}.git"

    // Load JSON config from resources
    def jsonText = libraryResource('common-repo-list.js')
    def config = new JsonSlurper().parseText(jsonText)
    def target = config.find { it['repo-name'] == repoName }

    if (!target) error "❌ No config found for ${repoName}"

    env.APP_TYPE = target['app-type']
    env.IMAGE_NAME = "thanigai2808/${repoName}".toLowerCase()
    env.DOCKER_PORT = target['host_port'] ?: '9004'
    env.CONTAINER_NAME = "${repoName}-container".toLowerCase()

    echo "✅ Loaded config for ${repoName}: ${target}"
}
