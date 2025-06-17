package org.example

class CheckoutTargetRepo implements Serializable {
    def steps

    CheckoutTargetRepo(steps) {
        this.steps = steps
    }

    void checkout(String repoUrl, String branch = 'main', String credentialsId) {
        steps.dir('target-repo') {
            steps.deleteDir()
            steps.checkout([
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
}
