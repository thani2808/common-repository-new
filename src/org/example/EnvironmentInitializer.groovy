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

        repoList.each { appType, repos ->
            repos.each { repo ->
                def repoName = repo['repo-name']?.toString()?.trim()
                steps.echo "🔍 Comparing repo-name '${repoName}' with currentRepo '${currentRepo}'"
                steps.echo "⚙️ Type check: repo-name = ${repoName?.getClass()?.getName()}, currentRepo = ${currentRepo?.getClass()?.getName()}"

                if (repoName == currentRepo) {
                    result.APP_TYPE           = appType
                    result.IMAGE_NAME         = "${repo['dockerhub_username']}/${repoName}"
                    result.CONTAINER_NAME     = repoName
                    result.HOST_PORT          = repo['host_port']
                    result.DOCKER_PORT        = '8080'
                    result.DOCKERHUB_USERNAME = repo['dockerhub_username']
                    result.GIT_CREDENTIALS_ID = repo['git_credentials_id']
                    result.GIT_URL            = repo['git-url']

                    steps.echo "✅ [EnvLoader] Match found for repo '${currentRepo}'. Environment variables loaded:"
                    result.each { k, v -> steps.echo "➡️ ${k} = ${v}" }

                    found = true
                    return // exit inner loop
                }
            }
        }

        if (!found) {
            steps.echo "📜 [EnvLoader] Listing all repo-names in repoList for debugging:"
            repoList.each { appType, repos ->
                repos.each { repo ->
                    def rn = repo['repo-name']
                    steps.echo "📝 repo-name: '${rn}' | Type: ${rn?.getClass()?.getName()}"
                }
            }

            steps.error("❌ No environment matched for repo '${currentRepo}' in repo list")
        }

        // Return as a list of env key=value pairs
        return result.collect { key, value -> "${key}=${value}" }
    }
}
