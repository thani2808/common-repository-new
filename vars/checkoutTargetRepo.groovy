def call(String repoUrl, String branch = 'main', String credentialsId) {
    dir('target-repo') {
        deleteDir()
        checkout([
            $class: 'GitSCM',
            branches: [[name: "*/${branch}"]],
            doGenerateSubmoduleConfigurations: false,
            extensions: [],
            userRemoteConfigs: [[
                url: repoUrl,
                credentialsId: credentialsId,
                refspec: "+refs/heads/${branch}:refs/remotes/origin/${branch}"
            ]]
        ])
    }
}
