package org.example

import groovy.json.JsonSlurperClassic

class EnvLoader implements Serializable {
    def steps

    EnvLoader(steps) {
        this.steps = steps
    }

    /**
     * Dynamically loads environment variables based on the current Jenkins job
     * and a central JSON config file (common-repo-list.js).
     */
    def load() {
        steps.echo "🔧 Loading environment variables from EnvLoader"

        // Read the repo list config from shared library's resources
        def repoListText = steps.libraryResource('common-repo-list.js')
        def repoList = new JsonSlurperClassic().parseText(repoListText)

        // Determine the current repo name from the Jenkins job context
        def currentRepo = steps.env.JOB_NAME.tokenize('/').last()
        steps.echo "🔍 Current Jenkins repo: ${currentRepo}"

        def found = false

        // Search for the matching repo config
        repoList.each { appType, repos ->
            repos.each { repo ->
                if (repo['repo-name'] == currentRepo) {
                    steps.env.APP_TYPE = appType
                    steps.env.HOST_PORT = repo['host_port']
                    steps.env.DOCKER_IMAGE = "${repo['dockerhub_username']}/${repo['repo-name']}"
                    steps.env.CONTAINER_NAME = repo['repo-name']
                    steps.env.GIT_URL = repo['git-url']
                    steps.env.GIT_CREDENTIALS_ID = repo['git_credentials_id']
                    found = true

                    steps.echo "✅ Matched repo in config. Setting environment variables:"
                    steps.echo "🚀 App Type: ${steps.env.APP_TYPE}"
                    steps.echo "🔌 Host Port: ${steps.env.HOST_PORT}"
                    steps.echo "🐳 Docker Image: ${steps.env.DOCKER_IMAGE}"
                    steps.echo "📦 Container Name: ${steps.env.CONTAINER_NAME}"
                    return
                }
            }
        }

        if (!found) {
            steps.error "❌ Repo '${currentRepo}' not found in common-repo-list.js"
        }
    }
}
