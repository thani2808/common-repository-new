package org.example

import groovy.json.JsonSlurper

class InitEnv implements Serializable {
    def script

    InitEnv(script) {
        this.script = script
    }

    def initialize() {
        try {
            def repoName = script.params.REPO_NAME
            if (!repoName?.trim()) {
                script.error("❌ 'REPO_NAME' parameter must be provided.")
            }

            // Load config from shared library
            def configText = script.libraryResource("common-repo-list.js")
            script.writeFile(file: "common-repo-list.js", text: configText)

            def parsedMapRaw = new JsonSlurper().parseText(configText)
            def parsedMap = [:]

            // Normalize config to a serializable form
            parsedMapRaw.each { type, list ->
                parsedMap[type] = list.collect { item ->
                    def safeItem = [:]
                    item.each { k, v -> safeItem[k] = v.toString() }
                    return safeItem
                }
            }

            // Identify app type based on repo name
            def appTypeKey = parsedMap.find { type, list ->
                list.find { it['repo-name'] == repoName }
            }?.key

            if (!appTypeKey) {
                script.error("❌ Repository '${repoName}' not found in configuration.")
            }

            def isEureka = (appTypeKey == 'eureka')
            def hostPort = isEureka ? '8761' : findAvailablePort(9001, 9010)

            if (!hostPort) {
                script.error("❌ No free port available between 9001 and 9010.")
            }

            // Set environment variables
            script.env.PROJECT_DIR    = repoName
            script.env.APP_TYPE       = appTypeKey
            script.env.IMAGE_NAME     = "${repoName.toLowerCase()}-image"
            script.env.CONTAINER_NAME = "${repoName.toLowerCase()}-container"
            script.env.DOCKER_PORT    = isEureka ? '8761' : '8080'
            script.env.IS_EUREKA      = isEureka.toString()
            script.env.HOST_PORT      = hostPort

            // Log details
            script.echo "📦 Repo: ${repoName}"
            script.echo "🚀 App Type: ${script.env.APP_TYPE}"
            script.echo "🔌 Host Port: ${script.env.HOST_PORT}"
            script.echo "🐳 Docker Image: ${script.env.IMAGE_NAME}"
            script.echo "📦 Container Name: ${script.env.CONTAINER_NAME}"

        } catch (Exception e) {
            script.error("❌ Failed to initialize configuration: ${e.message}")
        }
    }

    /**
     * Find the first available port in the given range.
     */
    String findAvailablePort(int start, int end) {
        for (int port = start; port <= end; port++) {
            def result = script.sh(
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
