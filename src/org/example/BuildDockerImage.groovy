package org.example

class BuildDockerImage implements Serializable {
    def steps

    BuildDockerImage(steps) {
        this.steps = steps
    }

    void build(String imageName = steps.env.IMAGE_NAME, String appType = steps.env.APP_TYPE) {
        if (!imageName || !appType) {
            def repoKey = steps.env.REPO_NAME
            if (!repoKey) {
                steps.error "❌ Missing REPO_NAME in environment for buildDockerImage"
            }

            def repoConfig = org.example.CommonConfig.getConfig(repoKey)
            new SetEnvFromConfig(steps).set(repoKey, repoConfig)
            imageName = steps.env.IMAGE_NAME
            appType = steps.env.APP_TYPE
        }

        steps.dir("target-repo/${steps.env.PROJECT_DIR}") {
            steps.sh "docker build -t ${imageName} ."
        }

        steps.echo "🐳 Docker Image Built: ${imageName} (App Type: ${appType})"
    }
}
