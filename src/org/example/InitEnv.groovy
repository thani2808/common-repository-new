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
        def repoConfigMap = new JsonSlurper().parseText(jsonText)

        def appTypeKey = detectAppType(repoName, repoConfigMap)
        def repoList = repoConfigMap[appTypeKey]
        def repoEntry = repoList.find { it["repo-name"] == repoName }

        if (!repoEntry) {
            steps.error("❌ No matching repo '${repoName}' under app-type '${appTypeKey}'")
        }

        // Set environment variables (with normalization)
        steps.env.APP_TYPE        = appTypeKey
        steps.env.IMAGE_NAME      = (repoEntry["image-name"] ?: "${repoName}-image").toLowerCase()
        steps.env.CONTAINER_NAME  = (repoEntry["container-name"] ?: "${repoName}-container").toLowerCase()
        steps.env.HOST_PORT       = repoEntry["docker-port"]?.toString() ?: "8080"
        steps.env.IS_EUREKA       = (repoEntry["is-eureka"]?.toString() ?: "false").toLowerCase()

        // Output details for confirmation
        steps.echo "📦 Repo: ${repoName}"
        steps.echo "🚀 App Type: ${steps.env.APP_TYPE}"
        steps.echo "🔌 Host Port: ${steps.env.HOST_PORT}"
        steps.echo "🐳 Docker Image: ${steps.env.IMAGE_NAME}"
        steps.echo "📦 Container Name: ${steps.env.CONTAINER_NAME}"
    }

    private String detectAppType(String repoName, def configMap) {
        for (def entry in configMap) {
            def appType = entry.key
            def repos = entry.value
            if (repos.find { it["repo-name"] == repoName }) {
                return appType
            }
        }
        steps.error("❌ Unable to determine app type for repo: ${repoName}")
    }
}
