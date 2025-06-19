package org.example

class EnvironmentInitializer implements Serializable {
    def steps

    EnvironmentInitializer(steps) {
        this.steps = steps
    }

    List<String> initialize() {
        steps.echo "🛠️ [EnvironmentInitializer] initialize() called"

        def envVars = new EnvLoader(steps).load()

        if (!envVars || envVars.isEmpty()) {
            steps.error("❌ EnvLoader returned null or empty map! Check 'common-repo-list.js'.")
        }

        def envList = []
        envVars.each { k, v ->
            envList << "${k}=${v}"
        }

        steps.echo "🔎 [EnvironmentInitializer] Final envVars map: ${envVars.inspect()}"
        steps.echo "✅ [EnvironmentInitializer] Returning envList for withEnv:"
        envList.each { steps.echo "➡️ ${it}" }

        return envList
    }
}
