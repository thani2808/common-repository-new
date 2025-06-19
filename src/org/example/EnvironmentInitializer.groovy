package org.example

import org.example.CommonConfig

class EnvironmentInitializer implements Serializable {
    def steps

    EnvironmentInitializer(steps) {
        this.steps = steps
    }

    def initialize() {
        def currentRepo = steps.env.REPO_NAME?.trim()
        steps.echo "📦 [EnvInitializer] Matching against REPO_NAME = '${currentRepo}'"

        def result = [:]
        def found = false

        def repoList = new CommonConfig(steps).getRepoList()

        repoList.each { config ->
            def repoName = config['repo-name']?.toString()?.trim()
            steps.echo "🔍 Comparing repo-name '${repoName}' with currentRepo '${currentRepo}'"
            steps.echo "⚙️ Type check: repo-name = ${repoName?.getClass()?.getName()}, currentRepo = ${currentRepo?.getClass()?.getName()}"

            if (repoName == currentRepo) {
                result.APP_TYPE           = config['app-type'] ?: "springboot"
                result.IMAGE_NAME         = "${config['dockerhub_username']}/${repoName}".toLowerCase()
                result.CONTAINER_NAME     = "${repoName}-container".toLowerCase()
                result.HOST_PORT          = config['host_port'] ?: "9000"
                result.DOCKER_PORT        = "8080"
                result.DOCKERHUB_USERNAME = config['dockerhub_username']
                result.GIT_CREDENTIALS_ID = config['git_credentials_id']
                result.GIT_URL            = config['git-url']

                steps.echo "✅ [EnvInitializer] Match found for repo '${currentRepo}'. Environment variables loaded:"
                result.each { k, v -> steps.echo "➡️ ${k} = ${v}" }

                found = true
                return // exit each loop early
            }
        }

        if (!found) {
            steps.echo "📜 [EnvInitializer] Listing all repo-names in repoList for debugging:"
            repoList.each { config ->
                def rn = config['repo-name']
                steps.echo "📝 repo-name: '${rn}' | Type: ${rn?.getClass()?.getName()}"
            }

            steps.error("❌ No environment matched for repo '${currentRepo}' in repo list")
        }

        return result.collect { key, value -> "${key}=${value}" }
    }
}
