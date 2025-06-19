package org.example

class CommonConfig implements Serializable {
    def steps

    String appType
    String imageName
    String containerName
    String dockerPort
    String projectDir

    CommonConfig(steps) {
        this.steps = steps
    }

    // Overloaded constructor for config initialization
    CommonConfig(Map config) {
        this.appType = config['app-type']
        this.imageName = "thanigai2808/${config['repo-name']}".toLowerCase()
        this.containerName = "${config['repo-name']}-container".toLowerCase()
        this.dockerPort = config['host_port'] ?: '9004'
        this.projectDir = config['project_dir'] ?: '.'
    }

    // Example method for returning repository list
    def getRepoList() {
        // Replace with actual logic or external file reading
        return [
            [
                'repo-name'        : 'Dev-role-Springboot-proj',
                'project_dir'      : '.',
                'git-url'          : 'git@github.com:thani2808/Dev-role-Springboot-proj.git',
                'dockerhub_username': 'thanigai2808',
                'host_port'        : '9004',
                'git_credentials_id': 'private-key-jenkins'
            ]
        ]
    }
}
