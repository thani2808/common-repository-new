def call(String imageName, String appType = 'springboot') {
    dir('target-repo') {
        def buildDir = '.'
        if (appType == 'springboot') {
            def pomPath = sh(script: "find . -name pom.xml | head -1", returnStdout: true).trim()
            buildDir = pomPath.replaceFirst('/pom.xml$', '')
        }
        dir(buildDir) {
            sh "docker build -t ${imageName} ."
        }
    }
}
