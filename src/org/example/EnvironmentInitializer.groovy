package org.example

class EnvironmentInitializer implements Serializable {
    def steps

    EnvironmentInitializer(steps) {
        this.steps = steps
    }

    List<String> initialize() {
        steps.echo "🛠️ [EnvironmentInitializer] initialize() called"

        def envVars = new EnvLoader(steps).load()
        def currentRepo = steps.env.REPO_NAME?.trim()
        boolean found = false

        if (!envVars || envVars.isEmpty()) {
            steps.error("❌ EnvLoader returned null or empty map! Check 'common-repo-list.js'.")
        }

        def result = [:]
        def repoList = envVars["repoList"]

        repoList.each { appType, repos ->
            repos.each { repo ->
                steps.echo "🔍 Checking repo: ${repo['repo-name']} vs ${currentRepo}"
                if (repo['repo-name'] == currentRepo) {
                    result.APP_TYPE          = appType
                    result.IMAGE_NAME        = "${repo['dockerhub_username']}/${repo['repo-name']}"
                    result.CONTAINER_NAME    = repo['repo-name']
                    result.HOST_PORT         = repo['host_port']
                    result.DOCKER_PORT       = '8080'
                    result.DOCKERHUB_USERNAME= repo['dockerhub_username']
                    result.GIT_CREDENTIALS_ID= repo['git_credentials_id']
                    result.GIT_URL           = repo['git-url']

                    steps.echo "✅ [EnvLoader] Match found for repo '${currentRepo}'. Environment variables loaded:"
                    result.each { k, v -> steps.echo "➡️ ${k} = ${v}" }

                    found = true
                    return
                }
            }
        }

        if (!found) {
            steps.echo "❌ No environment matched for repo '${currentRepo}' in repo list"
            steps.error("❌ Cannot proceed without matching repo environment.")
        }

        // Build envList from result map
        def envList = []
        result.each { k, v -> envList << "${k}=${v}" }

        steps.echo "✅ [EnvironmentInitializer] Final envList to return:"
        envList.each { steps.echo "➡️ ${it}" }

        return envList
    }
}
