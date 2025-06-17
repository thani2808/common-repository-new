package org.example

class CommonConfig {
    String appType
    String imageName
    String containerName
    String dockerPort

    CommonConfig(Map config) {
        if (!config || !(config instanceof Map)) {
            throw new IllegalArgumentException("❌ Invalid config map passed to CommonConfig")
        }

        this.appType = config['app-type'] ?: 'unknown'
        def repoName = config['repo-name'] ?: 'unnamed-repo'

        this.imageName = "thanigai2808/${repoName}".toLowerCase()
        this.containerName = "${repoName}-container".toLowerCase()
        this.dockerPort = config['host_port'] ?: '9004'
    }
}
