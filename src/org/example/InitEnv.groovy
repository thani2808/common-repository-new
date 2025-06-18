package org.example

import groovy.json.JsonSlurper

class InitEnv implements Serializable {
    def steps

    InitEnv(steps) {
        this.steps = steps
    }

    void init(String repoName) {
        steps.env.PROJECT_DIR = repoName

        def jsonText = steps.libraryResource('common-repo-list.js')
        def parsedList = new JsonSlurper().parseText(jsonText)
        def matchedConfig = parsedList.find { it['repo-name'] == repoName }

        if (!matchedConfig) {
            steps.error "❌ No config found for repo: ${repoName}"
        }

        steps.env.APP_TYPE       = matchedConfig['app-type'] ?: 'unknown'
        steps.env.IMAGE_NAME     = matchedConfig['image-name'] ?: "${repoName.toLowerCase()}-image"
        steps.env.CONTAINER_NAME = matchedConfig['container-name'] ?: "${repoName.toLowerCase()}-container"
        steps.env.DOCKER_PORT    = matchedConfig['docker-port']?.toString() ?: "8080"
        steps.env.IS_EUREKA      = (matchedConfig['is-eureka']?.toString() ?: "false").toLowerCase()

        // Assign HOST_PORT based on whether it's a Eureka server
        if (steps.env.APP_TYPE == 'eureka') {
            steps.env.HOST_PORT = "8761"
        } else {
            def freePort = findAvailablePort(9001, 9010)
            if (freePort == null) {
                steps.error "❌ No free port available between 9001 and 9010"
            }
            steps.env.HOST_PORT = freePort.toString()
        }

        steps.echo "📦 Repo: ${repoName}"
        steps.echo "🚀 App Type: ${steps.env.APP_TYPE}"
        steps.echo "🔌 Host Port: ${steps.env.HOST_PORT}"
        steps.echo "🐳 Docker Image: ${steps.env.IMAGE_NAME}"
        steps.echo "📦 Container Name: ${steps.env.CONTAINER_NAME}"
    }

    /**
     * Returns the first available port between [start, end], or null if none found.
     */
    Integer findAvailablePort(int start, int end) {
        for (int port = start; port <= end; port++) {
            def result = steps.sh(script: "netstat -an | findstr :${port}", returnStatus: true)
            if (result != 0) {
                return port
            }
        }
        return null
    }
}
