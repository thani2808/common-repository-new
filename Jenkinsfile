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
                    import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials
                    import com.cloudbees.plugins.credentials.CredentialsProvider
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
                    import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials
                    import com.cloudbees.plugins.credentials.CredentialsProvider
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
        ]
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
                    def repoName = params.REPO_NAME?.trim()
                    def branchName = params.COMMON_REPO_BRANCH?.trim()
                    if (!repoName || !branchName) {
                        error "❌ REPO_NAME or COMMON_REPO_BRANCH is empty."
                    }

                    env.REPO_NAME = repoName
                    env.REPO_BRANCH = branchName
                    env.REPO_URL = "git@github.com:thani2808/${repoName}.git"
                }
            }
        }

        stage('Checkout Target Repo') {
            steps {
                dir('target-repo') {
                    deleteDir()
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: "${env.REPO_BRANCH}"]],
                        userRemoteConfigs: [[
                            url: "${env.REPO_URL}",
                            credentialsId: "${env.GIT_CREDENTIALS_ID}",
                            refspec: "+refs/heads/${env.REPO_BRANCH}:refs/remotes/origin/${env.REPO_BRANCH}"
                        ]],
                        doGenerateSubmoduleConfigurations: false,
                        submoduleCfg: [],
                        extensions: [[
                            $class: 'CloneOption', noTags: false, shallow: false, depth: 0, reference: '', timeout: 10
                        ]]
                    ])
                }
            }
        }

        stage('Checkout Common JSON Repo') {
            steps {
                dir('common-repository-new') {
                    git branch: params.COMMON_REPO_BRANCH,
                        credentialsId: env.GIT_CREDENTIALS_ID,
                        url: 'git@github.com:thani2808/common-repository-new.git'
                }
            }
        }

        stage('Validate JSON Config') {
            steps {
                dir('common-repository-new') {
                    script {
                        def jsonFile = 'common-repo-list.js'
                        if (!fileExists(jsonFile)) {
                            error "❌ File not found: ${jsonFile}"
                        }

                        def jsonContent = readFile(jsonFile)
                        def parsedJson = new groovy.json.JsonSlurper().parseText(jsonContent)

                        if (!(parsedJson instanceof List)) {
                            error "❌ JSON root must be an array"
                        }

                        def requiredKeys = ['repo-name', 'app-type', 'git-url', 'dockerhub_username', 'host_port', 'git_credentials_id']
                        parsedJson.eachWithIndex { entry, i ->
                            requiredKeys.each { key ->
                                if (!entry.containsKey(key)) {
                                    error "❌ Entry at index ${i} is missing required key: ${key}"
                                }
                            }
                        }

                        echo "✅ JSON file is valid and well-structured"
                    }
                }
            }
        }

        stage('Detect App Type') {
            steps {
                dir('common-repository-new') {
                    script {
                        def jsonText = readFile('common-repo-list.js')
                        def repoList = new groovy.json.JsonSlurper().parseText(jsonText)
                        def targetRepo = repoList.find { it['repo-name'] == env.REPO_NAME }

                        if (!targetRepo) {
                            error "❌ No config found for repo ${env.REPO_NAME}"
                        }

                        env.APP_TYPE = targetRepo['app-type'] ?: 'unknown'
                        env.IMAGE_NAME = "${DOCKERHUB_USERNAME}/${env.REPO_NAME}".toLowerCase()
                        env.DOCKER_PORT = targetRepo['host_port'] ?: '9004'
                        env.CONTAINER_NAME = "${env.REPO_NAME}-container".toLowerCase()

                        echo "✅ APP_TYPE set to: ${env.APP_TYPE}"
                    }
                }
            }
        }

        stage('Debug Repo Structure') {
            steps {
                dir('target-repo') {
                    sh 'echo "🔍 Repo structure:"'
                    sh 'find .'
                }
            }
        }

        stage('Build App') {
            when {
                expression { env.APP_TYPE == 'springboot' }
            }
            steps {
                script {
                    dir('target-repo') {
                        def foundPomPath = sh(script: "find . -name pom.xml | head -1", returnStdout: true).trim()
                        if (!foundPomPath) {
                            error "❌ No pom.xml found in target-repo."
                        }
                        def pomDir = foundPomPath.replaceFirst('/pom.xml$', '')
                        dir(pomDir) {
                            sh 'mvn clean package -DskipTests -X'
                        }
                    }
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    dir('target-repo') {
                        def buildDir = '.'
                        if (env.APP_TYPE == 'springboot') {
                            def foundPom = sh(script: "find . -name pom.xml | head -1", returnStdout: true).trim()
                            if (!foundPom) {
                                error "❌ No pom.xml found in target-repo."
                            }
                            buildDir = foundPom.replaceFirst('/pom.xml$', '')
                        }

                        dir(buildDir) {
                            echo "📦 Building Docker image: ${env.IMAGE_NAME}"
                            sh "docker build -t ${env.IMAGE_NAME} ."
                        }
                    }
                }
            }
        }

        stage('Run Locally') {
    steps {
        script {
            def isSpringBoot = (env.APP_TYPE == 'springboot')
            def portFlag = isSpringBoot ? "-p ${env.HOST_PORT}:${env.HOST_PORT}" : "-p ${env.HOST_PORT}:80"
            def runCmd = isSpringBoot ? "--server.port=${env.HOST_PORT} --server.address=0.0.0.0" : ""

            sh """
                echo "🛑 Stopping existing container if running..."
                docker stop ${env.CONTAINER_NAME} || true
                docker rm ${env.CONTAINER_NAME} || true

                echo "🚀 Running new container with image: ${env.IMAGE_NAME}"
                docker run -d --name ${env.CONTAINER_NAME} ${portFlag} ${env.IMAGE_NAME} ${runCmd} || echo "❌ Failed to start container"

                echo "📦 Active containers:"
                docker ps -a

                echo "📝 Container logs:"
                docker logs ${env.CONTAINER_NAME} || echo "⚠️ No logs found"
            """
        }
    }
}

stage('Health Check') {
    steps {
        script {
            def hostIp = "localhost"
            def healthEndpoint = "/"  // or use "/actuator/health" if available
            sh """
                #!/bin/bash
                set -ex

                echo "🕒 Waiting for app to initialize..."
                sleep 10

                echo "📱 Starting health check on http://$hostIp:$HOST_PORT$healthEndpoint"

                for i in \$(seq 1 20); do
                    CODE=\$(curl -o /dev/null -s -w '%{http_code}' http://$hostIp:$HOST_PORT$healthEndpoint)
                    echo "Attempt \$i : HTTP \$CODE"
                    if [[ "\$CODE" == "200" ]]; then
                        echo '✅ Spring Boot health check passed'
                        exit 0
                    fi
                    sleep 3
                done

                echo '❌ Spring Boot health check failed after retries'
                echo "Response:"
                curl -v http://$hostIp:$HOST_PORT$healthEndpoint || true
                exit 1
            """
        }
    }
}
        
        stage('Success') {
            steps {
                echo "🎉 Local deployment of ${env.APP_TYPE} from ${env.REPO_NAME} succeeded!"
            }
        }
    }

    post {
        failure {
            echo '🚨 Pipeline failed!'
        }
        always {
            echo '🗓️ Pipeline finished.'
        }
    }
}
