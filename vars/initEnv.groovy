import groovy.json.JsonSlurper
import org.example.CommonConfig

def call(Map config = [:]) {
  env.REPO_NAME    = config.REPO_NAME
  env.REPO_BRANCH  = config.COMMON_REPO_BRANCH
  env.REPO_URL     = "git@github.com:thani2808/${env.REPO_NAME}.git"

  // Default values
  env.PROJECT_DIR     = '.'
  env.CONTAINER_NAME = "${env.REPO_NAME.toLowerCase()}-container"
  env.IMAGE_NAME     = "${env.REPO_NAME.toLowerCase()}-image"
  env.IS_EUREKA       = 'false'

  // Determine APP_TYPE and other flags based on repo name or known logic
  switch (env.REPO_NAME) {
    case 'common-repository-new':
      env.APP_TYPE       = 'springboot'
      env.PROJECT_DIR    = 'eureka-discovery-server'
      env.IMAGE_NAME     = 'eureka-server-image'
      env.CONTAINER_NAME = 'eureka-server-container'
      env.IS_EUREKA      = 'true'
      break

    case ~/.*node.*/:
      env.APP_TYPE = 'nodejs'
      break

    case ~/.*nginx.*/:
      env.APP_TYPE = 'nginx'
      break

    default:
      env.APP_TYPE = 'springboot'
  }

  // Assign DOCKER_PORT dynamically based on APP_TYPE
  switch (env.APP_TYPE) {
    case 'springboot':
      env.DOCKER_PORT = env.IS_EUREKA == 'true' ? '8761' : '9004'
      break
    case 'nodejs':
      env.DOCKER_PORT = '9005'
      break
    case 'nginx':
      env.DOCKER_PORT = '9006'
      break
    default:
      env.DOCKER_PORT = '9010' // default fallback port
  }

  echo "✅ Initialized Environment:"
  echo "   - REPO_NAME      = ${env.REPO_NAME}"
  echo "   - REPO_BRANCH    = ${env.REPO_BRANCH}"
  echo "   - REPO_URL       = ${env.REPO_URL}"
  echo "   - APP_TYPE       = ${env.APP_TYPE}"
  echo "   - PROJECT_DIR    = ${env.PROJECT_DIR}"
  echo "   - IMAGE_NAME     = ${env.IMAGE_NAME}"
  echo "   - CONTAINER_NAME = ${env.CONTAINER_NAME}"
  echo "   - DOCKER_PORT    = ${env.DOCKER_PORT}"
  echo "   - IS_EUREKA      = ${env.IS_EUREKA}"
}
