package org.example

import groovy.json.JsonSlurper

class InitEnv implements Serializable {
    def steps

    InitEnv(steps) {
        this.steps = steps
    }

    void init(String repoName) {
        steps.env.PROJECT_DIR = repoName

        // Load and parse the JSON config
        def jsonText = steps.libraryResource('common-repo-list.js')
        def parsedMapRaw = new JsonSlurper().parseText(jsonText) as Map
        // 🔴 Convert parsedMapRaw to Serializable Map<String, Object>
        def parsedMap = [:]
        parsedMapRaw.each { k, v -> parsedMap[k] = v.collect { it.clone() } }

        // Detect app type (springboot, eureka, etc.)
        def appTypeKey = parsedMap.find { type, list ->
            list.find { it['repo-name'] == repoName }
        }?.key

        if (!appTypeKey) {
            steps.error "❌ Repo '${repoName}' not found in any app-type list"
        }

        def repoList = parsedMap[appTypeKey]
        def matchedConfig = repoList.find { it['repo-name'] == repoName }

        if (!matchedConfig) {
            steps.error "❌ No config found for repo: ${repoName}"
        }

        // 🔴 Defensive copy to avoid LazyMap serialization
        def safeMatchedConfig = [:]
        matchedConfig.each { k, v -> safeMatchedConfig[k] = v.toString() }

        // Set standard environment variables
        steps.env.APP_TYPE       = appTypeKey
        steps.env.IMAGE_NAME     = "${repoName.toLowerCase()}-image"
        steps.env.CONTAINER_NAME = "${repoName.toLowerCase()}-container"
        steps.env.DOCKER_PORT    = appTypeKey == 'eureka' ? '8761' : '8080'
        steps.env.IS_EUREKA      = (appTypeKey == 'eureka').toString()

        // Use static port for Eureka, dynamic port for others
        if (steps.env.IS_EUREKA == 'true') {
            steps.env.HOST_PORT = "8761"
        } else {
            def freePort = findAvailablePort(9001, 9010)
            if (!freePort) {
                steps.error "❌ No free port available between 9001 and 9010"
            }
            steps.env.HOST_PORT = freePort
        }

        // Log the environment setup
        steps.echo "📦 Repo: ${repoName}"
        steps.echo "🚀 App Type: ${steps.env.APP_TYPE}"
        steps.echo "🔌 Host Port: ${steps.env.HOST_PORT}"
        steps.echo "🐳 Docker Image: ${steps.env.IMAGE_NAME}"
        steps.echo "📦 Container Name: ${steps.env.CONTAINER_NAME}"
    }

    /**
     * Returns the first available port between [start, end], or null if none found.
     */
    String findAvailablePort(int start, int end) {
        for (int port = start; port <= end; port++) {
            def result = steps.sh(script: "netstat -an | findstr :${port}", returnStatus: true)
            if (result != 0) {
                return port.toString()
            }
        }
        return null
    }
}
