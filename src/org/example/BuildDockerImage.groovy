package org.example

class BuildDockerImage implements Serializable {
    def steps
    BuildDockerImage(steps) {
        this.steps = steps
    }

    void build(String imageName = null, String appType = null) {
        imageName = imageName ?: steps.env.IMAGE_NAME
        appType   = appType ?: steps.env.APP_TYPE

        if (!imageName || !appType) {
            def repoKey = steps.env.REPO_NAME
            if (!repoKey) {
                steps.error "❌ Missing REPO_NAME for BuildDockerImage"
            }

            def repoConfig = org.example.CommonConfig.getConfig(repoKey)
            new org.example.SetEnvFromConfig(steps).set(repoKey, repoConfig)

            imageName = steps.env.IMAGE_NAME
            appType   = steps.env.APP_TYPE
        }

        def projectDir = steps.env.PROJECT_DIR ?: '.'

        steps.echo "📦 Building Docker image '${imageName}' for appType=${appType}"
        steps.dir("target-repo/${projectDir}") {
            steps.sh "docker build -t ${imageName} ."
        }

        steps.echo "✅ Docker image built: ${imageName}"
    }
}
