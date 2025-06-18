package org.example

import groovy.json.JsonSlurper
import java.net.ServerSocket

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

        // Dynamically find free port between 9001 and 9010
        def freePort = findAvailablePort(9001, 9010)
        if (!freePort) {
            steps.error "❌ No free port available between 9001 and 9010"
        }
        steps.env.HOST_PORT = freePort.toString()
    }

    int findAvailablePort(int start, int end) {
        for (int port = start; port <= end; port++) {
            try {
                new ServerSocket(port).withCloseable { return port }
            } catch (IOException ignored) {
                // Port is in use, try next
            }
        }
        return -1
    }
}
