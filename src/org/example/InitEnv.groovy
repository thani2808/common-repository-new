package org.example

import groovy.json.JsonSlurper

class InitEnv implements Serializable {
    def steps

    InitEnv(steps) {
        this.steps = steps
    }

    void init(String repoName) {
        // Always set PROJECT_DIR
        steps.env.PROJECT_DIR = repoName

        // Load and parse the repo config list
        def jsonText = steps.libraryResource('common-repo-list.js')
        def parsedList = new JsonSlurper().parseText(jsonText)
        def matchedConfig = parsedList.find { it['repo-name'] == repoName }

        if (!matchedConfig) {
            steps.error "❌ No config found for repo: ${repoName}"
        }

        // Dynamically set env vars from config
        steps.env.APP_TYPE       = matchedConfig['app-type'] ?: 'unknown'
        steps.env.IMAGE_NAME     = matchedConfig['image-name'] ?: "${repoName.toLowerCase()}-image"
        steps.env.CONTAINER_NAME = matchedConfig['container-name'] ?: "${repoName.toLowerCase()}-container"
        steps.env.DOCKER_PORT    = matchedConfig['docker-port']?.toString() ?: "8080"
        steps.env.IS_EUREKA      = (matchedConfig['is-eureka']?.toString() ?: "false").toLowerCase()
    }
}
