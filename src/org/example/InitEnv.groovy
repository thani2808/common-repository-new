package org.example

import groovy.json.JsonSlurper

class InitEnv implements Serializable {
    def steps

    InitEnv(steps) {
        this.steps = steps
    }

    void init(String repoName) {
        def jsonText = steps.libraryResource('common-repo-list.js')
        def parsedList = new JsonSlurper().parseText(jsonText)
        def matchedConfig = parsedList.find { it['repo-name'] == repoName }

        if (!matchedConfig) {
            steps.error "❌ No config found for repo: ${repoName}"
        }

        def config = new CommonConfig(matchedConfig)
        steps.env.APP_TYPE = config.appType
        steps.env.IMAGE_NAME = config.imageName
        steps.env.CONTAINER_NAME = config.containerName
        steps.env.DOCKER_PORT = config.dockerPort
    }
}
