package org.example

import groovy.json.JsonSlurper

class InitEnv implements Serializable {
    def steps

    InitEnv(steps) {
        this.steps = steps
    }

    Map init(String repoName) {
        if (!repoName?.trim()) {
            steps.error "❌ repoName must be provided via Jenkins parameter."
        }

        steps.env.PROJECT_DIR = repoName

        // Load and parse the JSON config file from shared library
        def jsonText = steps.libraryResource('common-repo-list.js')
        def parsedMapRaw = new JsonSlurper().parseText(jsonText)

        // Convert to serializable map
        def parsedMap = [:]
        parsedMapRaw.each { type, list ->
            parsedMap[type] = list.collect { item ->
                def safeItem = [:]
                item.each { k, v -> safeItem[k] = v.toString() }
                return safeItem
            }
        }

        // Detect app type by repoName
        def appTypeKey = parsedMap.find { type, list ->
            list.find { it['repo-name'] == repoName }
        }?.key

        if (!appTypeKey) {
            steps.error "❌ Repo '${repoName}' not found in any app-type list"
        }

        def isEureka = appTypeKey == 'eureka'
        def hostPort = isEureka ? '8761' : findAvailablePort(9001, 9010)

        if (!hostPort) {
            steps.error "❌ No free port available between 9001 and 9010"
        }

        def config = [
            APP_TYPE      : appTypeKey,
            IMAGE_NAME    : "${repoName.toLowerCase()}-image",
            CONTAINER_NAME: "${repoName.toLowerCase()}-container",
            DOCKER_PORT   : isEureka ? '8761' : '8080',
            IS_EUREKA     : isEureka.toString(),
            HOST_PORT     : hostPort
        ]

        // Log the config
        steps.echo "📦 Repo: ${repoName}"
        steps.echo "🚀 App Type: ${config.APP_TYPE}"
        steps.echo "🔌 Host Port: ${config.HOST_PORT}"
        steps.echo "🐳 Docker Image: ${config.IMAGE_NAME}"
        steps.echo "📦 Container Name: ${config.CONTAINER_NAME}"

        return config
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
