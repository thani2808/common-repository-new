package org.example

class PreRunDebug implements Serializable {
    def steps

    PreRunDebug(steps) {
        this.steps = steps
    }

    void check() {
        steps.echo "🔧 Pre-Run – env.APP_TYPE       = '${steps.env.APP_TYPE}'"
        steps.echo "🔧 Pre-Run – env.IMAGE_NAME     = '${steps.env.IMAGE_NAME}'"
        steps.echo "🔧 Pre-Run – env.CONTAINER_NAME = '${steps.env.CONTAINER_NAME}'"

        if (!steps.env.APP_TYPE) {
            steps.error "❌ Pre-Run check failed: APP_TYPE still null!"
        }
    }
}
