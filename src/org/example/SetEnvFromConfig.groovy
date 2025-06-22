package org.example

class SetEnvFromConfig implements Serializable {
    def steps

    SetEnvFromConfig(steps) {
        this.steps = steps
    }

    void set(String repoKey, Map repoConfig) {
        def appType = repoConfig["app-type"]?.toLowerCase() ?: 'springboot'

        steps.env.APP_TYPE       = appType
        steps.env.PROJECT_DIR    = repoConfig["project_dir"] ?: '.'
        steps.env.DOCKER_PORT    = repoConfig["host_port"]?.toString() ?: '8080'
        steps.env.IMAGE_NAME     = "${repoKey.toLowerCase()}-image"
        steps.env.CONTAINER_NAME = "${repoKey.toLowerCase()}-container"
        steps.env.GIT_CREDENTIALS_ID = repoConfig["git_credentials_id"] ?: ''
        steps.env.REPO_URL       = repoConfig["git-url"] ?: ''
        steps.env.IS_EUREKA      = (appType == "eureka").toString()

        steps.echo """
        ✅ Environment Loaded:
           - REPO_NAME      = ${steps.env.REPO_NAME}
           - REPO_BRANCH    = ${steps.env.REPO_BRANCH}
           - REPO_URL       = ${steps.env.REPO_URL}
           - APP_TYPE       = ${steps.env.APP_TYPE}
           - PROJECT_DIR    = ${steps.env.PROJECT_DIR}
           - IMAGE_NAME     = ${steps.env.IMAGE_NAME}
           - CONTAINER_NAME = ${steps.env.CONTAINER_NAME}
           - DOCKER_PORT    = ${steps.env.DOCKER_PORT}
           - IS_EUREKA      = ${steps.env.IS_EUREKA}
        """
    }
}
