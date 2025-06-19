package org.example

class EnvironmentInitializer implements Serializable {
    def steps

    EnvironmentInitializer(steps) {
        this.steps = steps
    }

    void initialize() {
        steps.echo "🛠️ [EnvironmentInitializer] initialize() called"

        def envLoader = new EnvLoader(steps)
        def envVars = envLoader.load()

        steps.echo "🔍 [EnvironmentInitializer] envVars.inspect(): ${envVars.inspect()}"

        if (envVars == null || envVars.isEmpty()) {
            steps.error("❌ EnvLoader returned null or empty map! Check 'common-repo-list.js' or mismatched REPO_NAME.")
        }

        // Apply environment variables from the loaded config
        steps.env.APP_TYPE            = envVars.APP_TYPE ?: 'springboot'
        steps.env.IMAGE_NAME          = envVars.IMAGE_NAME
        steps.env.CONTAINER_NAME      = envVars.CONTAINER_NAME
        steps.env.HOST_PORT           = envVars.HOST_PORT
        steps.env.DOCKER_PORT         = envVars.DOCKER_PORT
        steps.env.DOCKERHUB_USERNAME  = envVars.DOCKERHUB_USERNAME
        steps.env.GIT_CREDENTIALS_ID  = envVars.GIT_CREDENTIALS_ID
        steps.env.GIT_URL             = envVars.GIT_URL

        // Debug print of all key values
        steps.echo "✅ [EnvironmentInitializer] Final Loaded Values:"
        [
            'APP_TYPE',
            'IMAGE_NAME',
            'CONTAINER_NAME',
            'HOST_PORT',
            'DOCKER_PORT',
            'DOCKERHUB_USERNAME',
            'GIT_CREDENTIALS_ID',
            'GIT_URL'
        ].each { key ->
            steps.echo "➡️ ${key} = '${steps.env[key]}'"
        }

        // Fail-fast if essential value missing
        if (!steps.env.APP_TYPE) {
            steps.error("❌ APP_TYPE is null or empty after initialization.")
        }
    }
}
