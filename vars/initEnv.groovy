import org.example.CommonConfig

def call(Map args = [:]) {
    def repoKey = args.REPO_NAME ?: env.REPO_NAME
    def branch  = args.COMMON_REPO_BRANCH ?: 'feature'

    env.REPO_NAME   = repoKey
    env.REPO_BRANCH = branch

    def repoConfig = CommonConfig.getConfig(repoKey)

    setEnvFromConfig(repoKey, repoConfig)
}
