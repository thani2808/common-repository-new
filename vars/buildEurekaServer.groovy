def call(String projectDir) {
    dir(projectDir) {
        sh '''
            echo "🧪 Building Eureka JAR..."
            mvn clean package -DskipTests

            echo "📦 Building Docker image for Eureka..."
            docker build --build-arg JAR_FILE=target/eureka-discovery-server-0.0.1-SNAPSHOT.jar -t thanigai2808/eureka-server .

            echo "🛠️ Starting Eureka container..."
            docker network inspect spring-net >/dev/null 2>&1 || docker network create spring-net

            docker rm -f eureka-server || true
            echo '🧹 Skipping fuser on Windows'

            docker run -d --name eureka-server --network spring-net -p 8761:8761 thanigai2808/eureka-server

            echo "⌛ Waiting for Eureka to be ready..."
            for i in {1..20}; do
                echo "⏳ Checking Eureka status... Attempt $i"
                STATUS=$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8761/eureka/apps || echo "000")
                if [ "$STATUS" = "200" ]; then
                    echo "✅ Eureka is up!"
                    break
                fi
                sleep 5
            done

            if [ "$STATUS" != "200" ]; then
                echo "❌ Eureka did not become ready in time. Final status: $STATUS"
                exit 1
            fi
        '''
    }
}
