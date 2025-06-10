// === Dynamic Parameters Section ===
properties([
    parameters([
        [
            $class: 'CascadeChoiceParameter',
            choiceType: 'PT_SINGLE_SELECT',
            description: 'Select repository from thani2808',
            filterLength: 1,
            name: 'REPO_NAME',
            referencedParameters: '',
            script: [
                $class: 'GroovyScript',
                script: new org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript('''
                    import com.cloudbees.plugins.credentials.CredentialsProvider
                    import com.cloudbees.plugins.credentials.common.StandardCredentials
                    import com.cloudbees.plugins.credentials.common.IdCredentials
                    import com.cloudbees.plugins.credentials.CredentialsMatchers
                    import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials
                    import jenkins.model.Jenkins
                    import groovy.json.JsonSlurper

                    def githubUser = "thani2808"

                    def creds = CredentialsProvider.lookupCredentials(
                        BaseStandardCredentials.class,
                        Jenkins.instance,
                        null,
                        null
                    )
                    def tokenCred = creds.find { it.id == "github-api-token" }
                    if (!tokenCred) return ["❌ GitHub token not found"]

                    def token = tokenCred.secret.getPlainText()

                    def url = "https://api.github.com/users/${githubUser}/repos"
                    def conn = new URL(url).openConnection()
                    conn.setRequestProperty("User-Agent", "jenkins")
                    conn.setRequestProperty("Authorization", "token ${token}")
                    def response = new JsonSlurper().parse(conn.inputStream)
                    return response.collect { it.name }.sort()
                ''', false)
            ]
        ],
        [
            			$class: 'CascadeChoiceParameter',
            choiceType: 'PT_SINGLE_SELECT',
            description: 'Select branch of selected repo',
            filterLength: 1,
            name: 'COMMON_REPO_BRANCH',
            referencedParameters: 'REPO_NAME',
            script: [
                $class: 'GroovyScript',
                script: new org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript('''
                    import com.cloudbees.plugins.credentials.CredentialsProvider
                    import com.cloudbees.plugins.credentials.common.StandardCredentials
                    import com.cloudbees.plugins.credentials.common.IdCredentials
                    import com.cloudbees.plugins.credentials.CredentialsMatchers
                    import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials
                    import jenkins.model.Jenkins
                    import groovy.json.JsonSlurper

                    def githubUser = "thani2808"
					def repo = REPO_NAME
                    if (!repo) return ["Select a repo first"]

                    def creds = CredentialsProvider.lookupCredentials(
                        BaseStandardCredentials.class,
                        Jenkins.instance,
                        null,
                        null
                    )
                    def tokenCred = creds.find { it.id == "github-api-token" }
                    if (!tokenCred) return ["❌ GitHub token not found"]

                    def token = tokenCred.secret.getPlainText()

                    def url = "https://api.github.com/repos/${githubUser}/${repo}/branches"
                    def conn = new URL(url).openConnection()
                    conn.setRequestProperty("User-Agent", "jenkins")
                    conn.setRequestProperty("Authorization", "token ${token}")
                    def response = new JsonSlurper().parse(conn.inputStream)
                    return response.collect { it.name }.sort()
                ''', false)
            ]
        ],
        choice(
            name: 'APP_TYPE',
            choices: ['springboot', 'nginx'],
            description: 'Type of app to deploy'
        )
    ])
])

// === Main Pipeline ===
pipeline {
    agent any

    parameters {
        string(name: 'REPO_NAME', defaultValue: '', description: 'GitHub repository name (without .git)')
        string(name: 'COMMON_REPO_BRANCH', defaultValue: 'feature', description: 'Branch to use')
    }

    environment {
        DOCKERHUB_USERNAME = 'thanigai2808'
        HOST_PORT = '9004'
        GIT_CREDENTIALS_ID = 'private-key-jenkins'
    }

    stages {
        stage('Clean Workspace') {
            steps {
                cleanWs()
            }
        }

        stage('Initialize') {
            steps {
                script {
                    def portMap = [springboot: '9004', nginx: '80']
                    def repoName = params.REPO_NAME.trim()
                    def branchName = params.COMMON_REPO_BRANCH.trim()
                    def imageName = "${params.APP_TYPE}-local-app"
                    def containerName = "${params.APP_TYPE}-local-container"
                    def dockerPort = portMap[params.APP_TYPE]
                    def dockerRepo = "${env.DOCKERHUB_USERNAME}/${imageName}"
                    def repoUrl = "git@github.com:thani2808/${repoName}.git"

		    env.REPO_NAME = repoName
		    env.REPO_BRANCH = branchName
                    env.IMAGE_NAME = imageName
                    env.CONTAINER_NAME = containerName
                    env.DOCKER_PORT = dockerPort
                    env.DOCKERHUB_REPO = dockerRepo
                    env.REPO_URL = repoUrl
                }
            }
        }

        stage('Checkout Target Repo') {
            steps {
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: "${env.REPO_BRANCH}"]],
                        userRemoteConfigs: [[
                            url: env.REPO_URL,
                            credentialsId: env.GIT_CREDENTIALS_ID,
			    refspec: "+refs/heads/${env.REPO_BRANCH}:refs/remotes/origin/${env.REPO_BRANCH}"	
                        ]]
                    ])
                }
            }

        stage('Build App') {
            when { expression { params.APP_TYPE == 'springboot' } }
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    if (!fileExists('Dockerfile')) {
                        error "❌ Dockerfile not found!"
                    }
                    sh "docker build -t ${env.IMAGE_NAME} ."
                }
            }
        }

        stage('Run Locally') {
            steps {
                script {
                    def runCmd = params.APP_TYPE == 'springboot' ? "java -jar app.jar --server.port=${env.HOST_PORT}" : ""
                    sh """
                        docker stop ${env.CONTAINER_NAME} || true
                        docker rm ${env.CONTAINER_NAME} || true
                        docker run -d --name ${env.CONTAINER_NAME} -p ${env.HOST_PORT}:${env.DOCKER_PORT} ${env.IMAGE_NAME} ${runCmd}
                    """
                }
            }
        }

        stage('Health Check') {
            steps {
                script {
                    sh """
                        retries=10
                        for i in \$(seq 1 \$retries); do
                          CODE=\$(curl -o /dev/null -s -w "%{http_code}" http://localhost:${env.HOST_PORT})
                          if [[ "\$CODE" == "200" ]]; then
                            echo "✅ App is up!"
                            exit 0
                          else
                            echo "⏳ Waiting for app... (\$i/\$retries)"
                            sleep 5
                          fi
                        done
                        echo "❌ App failed to start"
                        exit 1
                    """
                }
            }
        }

        stage('Success') {
            steps {
                echo "🎉 Local deployment of ${params.APP_TYPE} from ${params.REPO_NAME} succeeded!"
            }
        }
    }

    post {
        failure {
            echo '🚨 Pipeline failed!'
        }
        always {
            echo '📋 Pipeline finished.'
        }
    }
}