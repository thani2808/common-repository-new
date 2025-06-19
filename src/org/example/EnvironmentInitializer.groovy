package org.example

class EnvironmentInitializer implements Serializable {
    def steps

    EnvironmentInitializer(steps) {
        this.steps = steps
    }

    Map<String, String> initialize() {
        steps.echo "🛠️ [EnvironmentInitializer] initialize() called"

        def envLoader = new EnvLoader(steps)
        def envVars = envLoader.load()

        steps.echo "🔍 [EnvironmentInitializer] envVars.inspect(): ${envVars.inspect()}"

        if (envVars == null || envVars.isEmpty()) {
            steps.error("❌ EnvLoader returned null or empty map! Check 'common-repo-list.js'.")
        }

        def result = [
            "APP_TYPE=${envVars.APP_TYPE ?: 'springboot'}",
            "IMAGE_NAME=${envVars.IMAGE_NAME}",
            "CONTAINER_NAME=${envVars.CONTAINER_NAME}",
            "HOST_PORT=${envVars.HOST_PORT}",
            "DOCKER_PORT=${envVars.DOCKER_PORT}",
            "DOCKERHUB_USERNAME=${envVars.DOCKERHUB_USERNAME}",
            "GIT_CREDENTIALS_ID=${envVars.GIT_CREDENTIALS_ID}",
            "GIT_URL=${envVars.GIT_URL}"
        ]

        steps.echo "✅ [EnvironmentInitializer] Returning env list for withEnv:"
        result.each { steps.echo "➡️ ${it}" }

        return result
    }
}
