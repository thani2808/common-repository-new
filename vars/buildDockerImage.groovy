import org.example.CommonConfig

def call(String imageName = env.IMAGE_NAME, String appType = env.APP_TYPE) {
    if (!imageName || !appType) {
        def repoKey = env.REPO_NAME
        if (!repoKey) {
            error "❌ Missing REPO_NAME in environment for buildDockerImage"
        }

        def repoConfig = CommonConfig.getConfig(repoKey)
        setEnvFromConfig(repoKey, repoConfig)
        imageName = env.IMAGE_NAME
        appType = env.APP_TYPE
    }

    dir("target-repo/${env.PROJECT_DIR}") {
        sh "docker build -t ${imageName} ."
    }

    echo "🐳 Docker Image Built: ${imageName} (App Type: ${appType})"
}
