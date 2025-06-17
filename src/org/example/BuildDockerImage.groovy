package org.example

class BuildDockerImage implements Serializable {
    def steps

    BuildDockerImage(steps) {
        this.steps = steps
    }

    void build(String imageName = null, String appType = null) {
        // Load from env if not explicitly passed
        imageName = imageName ?: steps.env.IMAGE_NAME
        appType   = appType ?: steps.env.APP_TYPE

        // Fallback logic if IMAGE_NAME or APP_TYPE is not set
        if (!imageName || !appType) {
            def repoKey = steps.env.REPO_NAME
            if (!repoKey) {
                steps.error "❌ Missing REPO_NAME in environment for buildDockerImage"
            }

            def repoConfig = org.example.CommonConfig.getConfig(repoKey)
            new SetEnvFromConfig(steps).set(repoKey, repoConfig)

            imageName = steps.env.IMAGE_NAME
            appType   = steps.env.APP_TYPE
        }

        def projectDir = steps.env.PROJECT_DIR ?: '.'

        steps.echo "📦 Starting Docker build for image: ${imageName}"
        steps.dir("target-repo/${projectDir}") {
            steps.sh "docker build -t ${imageName} ."
        }

        steps.echo "✅ Docker image built: ${imageName} (App Type: ${appType})"
    }
}
