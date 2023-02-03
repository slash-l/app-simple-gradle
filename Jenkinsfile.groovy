node {
    def server = Artifactory.server 'demo-server'
    def rtGradle = Artifactory.newGradleBuild()
    def buildInfo = Artifactory.newBuildInfo()

    stage ('Code Check out') {
        git url: 'https://gitee.com/mumu79/app-gradle-single.git'
    }

    stage ('Artifactory configuration') {
        rtGradle.tool = 'gradle' // Tool name from Jenkins configuration
//        rtGradle.useWrapper = true
        rtGradle.usesPlugin = true
        rtGradle.deployer repo: 'slash-gradle-virtual', server: server
        rtGradle.resolver repo: 'slash-gradle-virtual', server: server
    }

    stage ('Exec Gradle') {
        rtGradle.run rootDir: ".", buildFile: 'build.gradle', tasks: 'clean artifactoryPublish --stacktrace', buildInfo: buildInfo
    }

    stage ('Publish') {
        server.publishBuildInfo buildInfo
    }
}