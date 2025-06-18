package org.example

import groovy.json.JsonSlurper
import groovy.json.JsonBuilder
import groovy.json.JsonSlurperClassic

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

            // Load and write shared config
            def configText = script.libraryResource("common-repo-list.js")
            script.writeFile(file: "common-repo-list.js", text: configText)

            // Safely normalize parsed JSON
            def parsedMapRaw = new JsonSlurper().parseText(configText)
            def parsedMap = new JsonSlurperClassic().parseText(
                new JsonBuilder(parsedMapRaw).toPrettyString()
            )

            // Identify app type by repo name
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

            // Log environment setup
            script.echo "📦 Repo: ${repoName}"
            script.echo "🚀 App Type: ${script.env.APP_TYPE}"
            script.echo "🔌 Host Port: ${script.env.HOST_PORT}"
            script.echo "🐳 Docker Image: ${script.env.IMAGE_NAME}"
            script.echo "📦 Container Name: ${script.env.CONTAINER_NAME}"

        } catch (Exception e) {
            def msg = (e.message instanceof String) ? e.message : e.toString()
            script.echo("❌ Exception type: ${e.getClass().getName()}")
            script.error("❌ Failed to initialize configuration: ${msg}")
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
