package org.example

class EnvLoader implements Serializable {
    def steps

    EnvLoader(steps) {
        this.steps = steps
    }

    /**
     * Load environment variables such as DockerHub credentials and Git credentials.
     * These values are returned as a map and injected into the pipeline env.
     */
    def load() {
        steps.echo "🔧 Loading environment variables from EnvLoader"

        return [
            DOCKERHUB_USERNAME: 'thanigai2808',
            GIT_CREDENTIALS_ID: 'private-key-jenkins'
        ]
    }
}
