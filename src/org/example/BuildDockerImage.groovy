package org.example

class BuildDockerImage implements Serializable {
    def steps

    BuildDockerImage(steps) {
        this.steps = steps
    }

    /**
     * Builds a Docker image using the given image name and Dockerfile path.
     *
     * @param imageName Optional. Docker image name. Defaults to steps.env.IMAGE_NAME.
     * @param dockerfilePath Optional. Path to Dockerfile. Defaults to "Dockerfile".
     * @param appType Optional. Not used currently, but can be used for multi-app builds.
     */
    void build(String imageName = null, String dockerfilePath = "Dockerfile", String appType = null) {
        // Resolve image name
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

        def projectDir = "${steps.env.WORKSPACE}/target-repo/${steps.env.REPO_NAME}"

        steps.echo "📂 Switching to project directory: ${projectDir}"
        steps.dir(projectDir) {
            // Debug: show current directory structure
            steps.sh "pwd && ls -l"

            // 🔍 Validate Dockerfile exists
            steps.sh """
                if [ ! -f "${dockerfilePath}" ]; then
                    echo "❌ Dockerfile not found at: ${dockerfilePath}"
                    exit 1
                fi
            """

            // 🔍 Validate pom.xml exists
            steps.sh '''
                if [ ! -f "pom.xml" ]; then
                    echo "❌ pom.xml not found in current directory: $(pwd)"
                    exit 1
                fi
            '''

            // Build the application using Maven (optional, if needed before Docker)
            steps.echo "🔧 Running Maven build"
            steps.sh "mvn clean package -DskipTests"

            // Build the Docker image
            steps.echo "📦 Building Docker image '${imageName}' using Dockerfile at '${dockerfilePath}'"
            steps.sh "docker build -t ${imageName}:latest -f ${dockerfilePath} ."
            steps.echo "✅ Docker image built: ${imageName}:latest"
        }
    }
}
