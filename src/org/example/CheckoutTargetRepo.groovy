package org.example

class CheckoutTargetRepo implements Serializable {
    def steps

    CheckoutTargetRepo(steps) {
        this.steps = steps
    }

    void checkout(String repoUrl, String branch = 'feature', String credentialsId) {
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

// 🔹 This function should either be inside a class or removed if you're using the class above.
// 🔹 To avoid confusion, use only one approach. Below is the cleaned global function version:

def checkoutRepo(String repoName, String repoBranch = 'feature') {
    checkout([
        $class: 'GitSCM',
        branches: [[name: "*/${repoBranch}"]],
        doGenerateSubmoduleConfigurations: false,
        extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'target-repo']],
        userRemoteConfigs: [[
            url: "git@github.com:thani2808/${repoName}.git",
            credentialsId: 'private-key-jenkins',
            refspec: "+refs/heads/${repoBranch}:refs/remotes/origin/${repoBranch}"
        ]]
    ])
}
