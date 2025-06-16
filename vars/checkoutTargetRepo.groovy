def call(String repoUrl, String branch, String credentialsId) {
    dir('target-repo') {
        deleteDir()
        checkout([
            $class: 'GitSCM',
            branches: [[name: branch]],
            userRemoteConfigs: [[
                url: repoUrl,
                credentialsId: credentialsId,
                refspec: "+refs/heads/${branch}:refs/remotes/origin/${branch}"
            ]]
        ])
    }
}
