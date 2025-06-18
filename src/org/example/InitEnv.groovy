package org.example

import groovy.json.JsonSlurper

class InitEnv implements Serializable {
    def steps

    InitEnv(steps) {
        this.steps = steps
    }

    void init(String repoName) {
        if (!repoName?.trim()) {
            steps.error "❌ repoName must be provided via Jenkins parameter."
        }

        steps.env.PROJECT_DIR = repoName

        // Load and parse the JSON config file from shared library
        def jsonText = steps.libraryResource('common-repo-list.js')
        def parsedMapRaw = new JsonSlurper().parseText(jsonText)

        // Convert to fully serializable map
        def parsedMap = [:]
        parsedMapRaw.each { type, list ->
            parsedMap[type] = list.collect { item ->
                def safeItem = [:]
                item.each { k, v -> safeItem[k] = v.toString() }
                return safeItem
            }
        }

        // Detect app type
        def appTypeKey = parsedMap.find { type, list ->
            list.find { it['repo-name'] == repoName }
        }?.key

        if (!appTypeKey) {
            steps.error "❌ Repo '${repoName}' not found in any app-type list"
        }

        def matchedConfig = parsedMap[appTypeKey].find { it['repo-name'] == repoName }

        // Set environment variables
        steps.env.APP_TYPE       = appTypeKey
        steps.env.IMAGE_NAME     = "${repoName.toLowerCase()}-image"
        steps.env.CONTAINER_NAME = "${repoName.toLowerCase()}-container"
        steps.env.DOCKER_PORT    = appTypeKey == 'eureka' ? '8761' : '8080'
        steps.env.IS_EUREKA      = (appTypeKey == 'eureka').toString()

        if (steps.env.IS_EUREKA == 'true') {
            steps.env.HOST_PORT = "8761"
        } else {
            def freePort = findAvailablePort(9001, 9010)
            if (!freePort) {
                steps.error "❌ No free port available between 9001 and 9010"
            }
            steps.env.HOST_PORT = freePort
        }

        // Logging setup
        steps.echo "📦 Repo: ${repoName}"
        steps.echo "🚀 App Type: ${steps.env.APP_TYPE}"
        steps.echo "🔌 Host Port: ${steps.env.HOST_PORT}"
        steps.echo "🐳 Docker Image: ${steps.env.IMAGE_NAME}"
        steps.echo "📦 Container Name: ${steps.env.CONTAINER_NAME}"
    }

    /**
     * Find the first available port in the given range.
     */
    String findAvailablePort(int start, int end) {
        for (int port = start; port <= end; port++) {
            def result = steps.sh(
                script: "netstat -an | findstr :${port}",
                returnStatus: true
            )
            if (result != 0) {
                return port.toString()
            }
        }
        return null
    }
}
