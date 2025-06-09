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
                    import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials
                    import jenkins.model.Jenkins

                    try {
                        def githubUser = "thani2808"
                        def creds = CredentialsProvider.lookupCredentials(
                            StandardUsernamePasswordCredentials.class,
                            Jenkins.instance,
                            null,
                            null
                        )
                        def tokenCred = creds.find { it.id == "github-api-token" }
                        if (!tokenCred) return ["GitHub token not found"]
                        def token = tokenCred.password.getPlainText()

                        def url = "https://api.github.com/users/${githubUser}/repos"
                        def conn = new URL(url).openConnection()
                        conn.setRequestProperty("User-Agent", "jenkins")
                        conn.setRequestProperty("Authorization", "token ${token}")
                        def response = new groovy.json.JsonSlurper().parse(conn.inputStream)
                        return response.collect { it.name }.sort()
                    } catch (Exception e) {
                        return ["Error fetching repos: " + e.message]
                    }
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
                    import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials
                    import jenkins.model.Jenkins

                    try {
                        def githubUser = "thani2808"
                        def repo = REPO_NAME
                        if (!repo) return ["Select a repo first"]

                        def creds = CredentialsProvider.lookupCredentials(
                            StandardUsernamePasswordCredentials.class,
                            Jenkins.instance,
                            null,
                            null
                        )
                        def tokenCred = creds.find { it.id == "github-api-token" }
                        if (!tokenCred) return ["GitHub token not found"]
                        def token = tokenCred.password.getPlainText()

                        def url = "https://api.github.com/repos/${githubUser}/${repo}/branches"
                        def conn = new URL(url).openConnection()
                        conn.setRequestProperty("User-Agent", "jenkins")
                        conn.setRequestProperty("Authorization", "token ${token}")
                        def response = new groovy.json.JsonSlurper().parse(conn.inputStream)
                        return response.collect { it.name }.sort()
                    } catch (Exception e) {
                        return ["Error fetching branches: " + e.message]
                    }
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
                    if (!params.REPO_NAME?.trim()) {
                        error "❌ REPO_NAME is empty. Please select a repo."
                    }

                    def portMap = [springboot: '9004', nginx: '80']
                    env.REPO_NAME = params.REPO_NAME.trim()
                    env.IMAGE_NAME = "${params.APP_TYPE}-local-app"
                    env.CONTAINER_NAME = "${params.APP_TYPE}-local-container"
                    env.DOCKER_PORT = portMap[params.APP_TYPE]
                    env.DOCKERHUB_REPO = "${env.DOCKERHUB_USERNAME}/${env.IMAGE_NAME}"
                    env.REPO_URL = "git@github.com:thanigai2808/${env.REPO_NAME}.git"
                    env.REPO_BRANCH = params.COMMON_REPO_BRANCH.trim()
                }
            }
        }

        stage('Print Config') {
            steps {
                script {
                    echo "App Type         : ${params.APP_TYPE}"
                    echo "Target Repo      : ${env.REPO_NAME}"
                    echo "Target Branch    : ${env.REPO_BRANCH}"
                    echo "Docker Repo      : ${env.DOCKERHUB_REPO}"
                    echo "Container Name   : ${env.CONTAINER_NAME}"
                    echo "Port Mapping     : ${env.HOST_PORT}:${env.DOCKER_PORT}"
                }
            }
        }

        stage('Checkout Target Repo') {
            steps {
                script {
                    echo "📥 Cloning ${env.REPO_URL} @ branch ${env.REPO_BRANCH}"
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: "*/${env.REPO_BRANCH}"]],
                        userRemoteConfigs: [[
                            url: env.REPO_URL,
                            credentialsId: env.GIT_CREDENTIALS_ID
                        ]]
                    ])
                }
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
