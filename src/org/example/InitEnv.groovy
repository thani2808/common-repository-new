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
            if (!repoName?.trim()) script.error("❌ 'REPO_NAME' must be provided.")

            def configText = script.libraryResource("common-repo-list.js")
            script.writeFile(file: "common-repo-list.js", text: configText)

            def parsedMap = parseAndNormalizeJson(configText)
            def appTypeKey = findAppType(repoName, parsedMap)
            if (!appTypeKey) script.error("❌ Repository '${repoName}' not found.")

            def isEureka = (appTypeKey == 'eureka')
            def hostPort = isEureka ? '8761' : findAvailablePort(9001, 9010)
            if (!hostPort) script.error("❌ No available port found between 9001–9010.")

            script.env.APP_TYPE       = appTypeKey
            script.env.PROJECT_DIR    = repoName
            script.env.IMAGE_NAME     = "${repoName.toLowerCase()}-image"
            script.env.CONTAINER_NAME = "${repoName.toLowerCase()}-container"
            script.env.DOCKER_PORT    = isEureka ? '8761' : '8080'
            script.env.IS_EUREKA      = isEureka.toString()
            script.env.HOST_PORT      = hostPort

            script.echo "✅ Environment initialized for '${repoName}'"

        } catch (Exception e) {
            script.error("❌ InitEnv failed: ${e.message ?: e.toString()}")
        }
    }

    @NonCPS
    def parseAndNormalizeJson(String configText) {
        def raw = new JsonSlurper().parseText(configText)
        def normalized = [:]
        raw.each { type, list ->
            normalized[type] = list.collect { item ->
                item.collectEntries { k, v -> [(k): v.toString()] }
            }
        }
        return normalized
    }

    @NonCPS
    def findAppType(String repoName, Map parsedMap) {
        parsedMap.find { type, repos -> repos.find { it['repo-name'] == repoName } }?.key
    }

    String findAvailablePort(int start, int end) {
        for (int port = start; port <= end; port++) {
            if (script.sh(script: "netstat -an | findstr :${port}", returnStatus: true) != 0) {
                return port.toString()
            }
        }
        return null
    }
}
