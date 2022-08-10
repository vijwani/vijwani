pipeline {
    agent any
    options {
        office365ConnectorWebhooks([
            [name: 'Office 365', url: "${URL_WEBHOOK}", notifyBackToNormal: true, notifyFailure: true, notifyRepeatedFailure: true, notifySuccess: true, notifyAborted: true]
        ])
    }
    parameters {
        string(name: 'BRANCH', defaultValue: 'main', description: 'Branch name', trim: true)
        string(name: 'REPO', defaultValue: '', description: 'Repository', trim: true)
    }

    stages {
        stage ('Clean workspace') {
            steps {
                cleanWs()
            }
        }
        stage('Checkout Script') {
            steps {
                script {
                    checkout([$class: 'GitSCM', branches: [[name: "${BRANCH}"]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'admin-github', url: "${REPO}"]]])
                }
            }
        }
        stage('Build') {
            steps {
                bat "msbuild.exe ${workspace}\\<path-to-solution>\\<solution-name>.sln /nologo /nr:false  /p:platform=\"x64\" /p:configuration=\"release\" /p:PackageCertificateKeyFile=<path-to-certificate-file>.pfx /t:clean;restore;rebuild"
            }
        }
        stage('Test: Unit Test') {
            steps {
                bat "dotnet test YourProjectPath\\msbuild.exe"
            }
        }

        stage('Test: Integration Test') {
            steps {
                bat "dotnet test ProjectPath\\msbuild.exe"
            }
        }
        stage('Push artifacts to Jfrog') {
            steps {
                def server = Artifactory.newServer url: 'artifactory-url', credentialsId: 'ccrreeddeennttiiaall'
                def uploadSpec = '''{
                                      "files": [
                                       {
                                          "pattern": "$WORKSPACE/*.exe",
                                           "target": "pathtoupload/"
                                       }
                                          ]
                                      }'''
                server.upload spec: uploadSpec
            }
        }
    }
    post {
        always {
                office365ConnectorSend webhookUrl: "${URL_WEBHOOK}",
                message: 'Code is deployed',
                status: 'Success'
        }
    }
}
