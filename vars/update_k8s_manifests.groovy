def call(Map config = [:]) {
    def imageTag = config.imageTag ?: error("Image tag is required")
    def manifestsPath = config.manifestsPath ?: 'kubernetes'
    def gitCredentials = config.gitCredentials ?: 'github-credentials'
    def gitUserName = config.gitUserName ?: 'divs111'
    def gitUserEmail = config.gitUserEmail ?: 'sonidivya638@gmail.com'

    echo "Updating K8s manifests with image tag"

    withCredentials([usernamePassword(
        credentialsId: gitCredentials,
        usernameVariable: 'GIT_USERNAME',
        passwordVariable: 'GIT_PASSWORD'
    )]) {
        // CONFIGURE GIT
        sh """
            git config user.name "${gitUserName}"
            git config user.email "${gitUserEmail}"
            """
        // Update deployment manifests with new image tags - using linux sed syntax
        sh """
            # Update main application deployment - note the correct image name is divs11/easyshop-mp
            sed -i "s|image: divs11/easyshop-mp:.*|image: divs11/easyshop-mp:${imageTag}|g" ${manifestsPath}/08-easyshop-deployment.yaml
            
            # Update migration job if it exists
            if [ -f "${manifestsPath}/12-migration-job.yaml" ]; then
                sed -i "s|image: divs11/easyshop-migration-mp:.*|image: divs11/easyshop-migration-mp:${imageTag}|g" ${manifestsPath}/12-migration-job.yaml
            fi
            
            # Ensure ingress is using the correct domain
            if [ -f "${manifestsPath}/10-ingress.yaml" ]; then
                sed -i "s|host: .*|host: easyshop.mp.com|g" ${manifestsPath}/10-ingress.yaml
            fi
            
            # Check for changes
            if git diff --quiet; then
                echo "No changes to commit"
            else
                # Commit and push changes
                git add ${manifestsPath}/*.yaml
                git commit -m "Update image tags to ${imageTag} and ensure correct domain [ci skip]"
                
                # Set up credentials for push
                git remote set-url origin https://\${GIT_USERNAME}:\${GIT_PASSWORD}@github.com/divs111/EasyShop.git
                git push origin HEAD:\${GIT_BRANCH}
            fi
        """

        
    }
}