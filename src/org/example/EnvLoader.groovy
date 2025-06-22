package org.example

import groovy.json.JsonSlurperClassic

class EnvLoader implements Serializable {
    def steps

    EnvLoader(steps) {
        this.steps = steps
    }

    /**
     * Loads environment variables based on REPO_NAME using common-repo-list.js
     *
     * @return Map of resolved environment variables
     */
    def load() {
        steps.echo "🔧 [EnvLoader] Starting to load environment variables..."

        def repoListText = steps.libraryResource('common-repo-list.js')
        def repoList = new JsonSlurperClassic().parseText(repoListText)

        def currentRepo = steps.params.REPO_NAME ?: steps.env.REPO_NAME ?: steps.env.JOB_NAME.tokenize('/').last()
        steps.echo "🔍 [EnvLoader] Resolved REPO_NAME: '${currentRepo}'"

        def result = [:]
        def found = false

        repoList.each { appType, repos ->
            repos.each { repo ->
                if (repo['repo-name'] == currentRepo) {
                    result.APP_TYPE = appType
                    result.IMAGE_NAME = "${repo['dockerhub_username']}/${repo['repo-name']}"
                    result.CONTAINER_NAME = repo['repo-name']
                    result.HOST_PORT = repo['host_port']
                    result.DOCKER_PORT = '8080'
                    result.DOCKERHUB_USERNAME = repo['dockerhub_username']
                    result.GIT_CREDENTIALS_ID = repo['git_credentials_id']
                    result.GIT_URL = repo['git-url']

                    steps.echo "✅ [EnvLoader] Match found for repo '${currentRepo}'. Environment variables loaded:"
                    result.each { k, v -> steps.echo "➡️ ${k} = ${v}" }

                    found = true
                    return
                }
            }
        }

        if (!found) {
            steps.echo "⚠️ [EnvLoader] Repo '${currentRepo}' not found. Showing known repos:"
            repoList.each { type, repos ->
                repos.each { r -> steps.echo "- ${r['repo-name']} (type: ${type})" }
            }
            steps.error "❌ [EnvLoader] Repo '${currentRepo}' not found in common-repo-list.js"
        }

        // ✅ Fail-fast debug check
        if (result == null || result.isEmpty()) {
            steps.error "❌ [EnvLoader] Loaded env map is null or empty! This should never happen."
        }

        // ✅ Final DEBUG dump
        steps.echo "🔎 [EnvLoader] Final envVars map: ${result.inspect()}"

        return result
    }
}
