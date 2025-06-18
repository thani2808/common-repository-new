package org.example

import groovy.json.JsonSlurperClassic

class EnvLoader implements Serializable {
    def steps

    EnvLoader(steps) {
        this.steps = steps
    }

    /**
     * Dynamically loads environment variables for the current Jenkins job
     * using common-repo-list.js from the shared library.
     * 
     * @return Map of resolved environment variables
     */
    def load() {
        steps.echo "🔧 Loading environment variables from EnvLoader..."

        // Load and parse the repo config file
        def repoListText = steps.libraryResource('common-repo-list.js')
        def repoList = new JsonSlurperClassic().parseText(repoListText)

        // Determine the repo name to search for
        def currentRepo = steps.params.REPO_NAME ?: steps.env.JOB_NAME.tokenize('/').last()
        steps.echo "🔍 Looking for repo: ${currentRepo}"

        def result = [:]
        def found = false

        // Loop through the repo list to find the matching entry
        repoList.each { appType, repos ->
            repos.each { repo ->
                if (repo['repo-name'] == currentRepo) {
                    result.APP_TYPE = appType
                    result.IMAGE_NAME = "${repo['dockerhub_username']}/${repo['repo-name']}"
                    result.CONTAINER_NAME = repo['repo-name']
                    result.HOST_PORT = repo['host_port']
                    result.DOCKERHUB_USERNAME = repo['dockerhub_username']
                    result.GIT_CREDENTIALS_ID = repo['git_credentials_id']
                    result.GIT_URL = repo['git-url']
                    result.DOCKER_PORT = '8080'  // default port inside container

                    steps.echo "✅ Repo found. Setting environment variables:"
                    result.each { key, value ->
                        steps.echo "➡️ ${key}: ${value}"
                    }

                    found = true
                    return
                }
            }
        }

        if (!found) {
            steps.error "❌ Repo '${currentRepo}' not found in common-repo-list.js"
        }

        return result
    }
}
