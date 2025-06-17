package org.example

class CheckoutTargetRepoImpl implements Serializable {
    def steps

    CheckoutTargetRepoImpl(steps) {
        this.steps = steps
    }

    void checkout(String repoName, String branch = 'feature') {
        steps.checkout([
            $class: 'GitSCM',
            branches: [[name: "*/${branch}"]],
            doGenerateSubmoduleConfigurations: false,
            extensions: [[
                $class: 'RelativeTargetDirectory',
                relativeTargetDir: "target-repo/${repoName}"  // 🔧 <- updated line
            ]],
            userRemoteConfigs: [[
                url: "git@github.com:thani2808/${repoName}.git",
                credentialsId: 'private-key-jenkins',
                refspec: "+refs/heads/${branch}:refs/remotes/origin/${branch}"
            ]]
        ])
    }
}
