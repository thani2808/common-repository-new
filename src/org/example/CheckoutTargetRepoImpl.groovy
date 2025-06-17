package org.example

class CheckoutTargetRepoImpl implements Serializable {
    def steps

    CheckoutTargetRepoImpl(steps) {
        this.steps = steps
    }

    void checkout(String repo, String branch) {
        def gitUrl = "git@github.com:thani2808/${repo}.git"

        steps.dir("target-repo") {
            steps.checkout([
                $class: 'GitSCM',
                branches: [[name: "*/${branch}"]],
                userRemoteConfigs: [[
                    url: gitUrl,
                    credentialsId: steps.env.GIT_CREDENTIALS_ID
                ]]
            ])
        }

        steps.echo "✅ Checked out ${repo}@${branch}"
    }
}
