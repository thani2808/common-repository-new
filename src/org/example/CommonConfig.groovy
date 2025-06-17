class CommonConfig {
    String appType
    String imageName
    String containerName
    String dockerPort
    String project_dir

    CommonConfig(Map config) {
        this.appType = config['app-type']
        this.imageName = "thanigai2808/${config['repo-name']}".toLowerCase()
        this.containerName = "${config['repo-name']}-container".toLowerCase()
        this.dockerPort = config['host_port'] ?: '9004'
        this.project_dir = config['project_dir'] ?: '.'
    }
}
