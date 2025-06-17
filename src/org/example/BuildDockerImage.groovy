package org.example

class BuildDockerImage implements Serializable {
    def steps

    BuildDockerImage(steps) {
        this.steps = steps
    }

    void build(String imageName = null, String dockerfilePath = "Dockerfile") {
        // Fallback to env if not passed explicitly
        imageName = imageName ?: steps.env.IMAGE_NAME

        if (!imageName) {
            def repoKey = steps.env.REPO_NAME
            if (!repoKey) {
                steps.error "❌ Missing REPO_NAME for BuildDockerImage"
            }

            def repoConfig = org.example.CommonConfig.getConfig(repoKey)
            new org.example.SetEnvFromConfig(steps).set(repoKey, repoConfig)

            imageName = steps.env.IMAGE_NAME
        }

        def projectDir = "target-repo/${steps.env.REPO_NAME}"

        steps.echo "📦 Building Docker image '${imageName}' using Dockerfile at '${dockerfilePath}'"

        steps.dir(projectDir) {
            steps.sh """
                docker build -t ${imageName}:latest -f ${dockerfilePath} .
            """
        }

        steps.echo "✅ Docker image built: ${imageName}:latest"
    }
}
