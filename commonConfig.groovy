def getConfig(String repoName) {
    def jsonText = libraryResource('common-repo-list.js')
    def config = new groovy.json.JsonSlurper().parseText(jsonText)
    return config.find { it['repo-name'] == repoName }
}
