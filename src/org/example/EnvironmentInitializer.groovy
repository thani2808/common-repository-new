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

        steps.echo "🔍 [EnvironmentInitializer] envVars.inspect() = ${envVars.inspect()}"

        steps.env.APP_TYPE            = envVars.APP_TYPE ?: 'springboot'
        steps.echo "➡️ [EnvironmentInitializer] Set APP_TYPE = '${steps.env.APP_TYPE}'"

        steps.env.IMAGE_NAME          = envVars.IMAGE_NAME
        steps.env.CONTAINER_NAME      = envVars.CONTAINER_NAME
        steps.env.HOST_PORT           = envVars.HOST_PORT
        steps.env.DOCKER_PORT         = envVars.DOCKER_PORT
        steps.env.DOCKERHUB_USERNAME  = envVars.DOCKERHUB_USERNAME
        steps.env.GIT_CREDENTIALS_ID  = envVars.GIT_CREDENTIALS_ID
        steps.env.GIT_URL             = envVars.GIT_URL

        steps.echo "✅ [EnvironmentInitializer] Completed"
    }
}
