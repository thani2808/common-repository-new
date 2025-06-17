def call(String repoKey, Map repoConfig) {
    def appType = repoConfig["app-type"]?.toLowerCase() ?: 'springboot'

    env.APP_TYPE       = appType
    env.PROJECT_DIR    = repoConfig["project_dir"] ?: '.'
    env.DOCKER_PORT    = repoConfig["host_port"]?.toString() ?: '8080'
    env.IMAGE_NAME     = "${repoKey.toLowerCase()}-image"
    env.CONTAINER_NAME = "${repoKey.toLowerCase()}-container"
    env.GIT_CREDENTIALS_ID = repoConfig["git_credentials_id"] ?: ''
    env.REPO_URL       = repoConfig["git-url"] ?: ''
    env.IS_EUREKA      = (appType == "eureka").toString()

    echo """
    ✅ Environment Loaded:
       - REPO_NAME      = ${env.REPO_NAME}
       - REPO_BRANCH    = ${env.REPO_BRANCH}
       - REPO_URL       = ${env.REPO_URL}
       - APP_TYPE       = ${env.APP_TYPE}
       - PROJECT_DIR    = ${env.PROJECT_DIR}
       - IMAGE_NAME     = ${env.IMAGE_NAME}
       - CONTAINER_NAME = ${env.CONTAINER_NAME}
       - DOCKER_PORT    = ${env.DOCKER_PORT}
       - IS_EUREKA      = ${env.IS_EUREKA}
    """
}
