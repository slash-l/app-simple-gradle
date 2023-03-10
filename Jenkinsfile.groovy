node {
    def server = Artifactory.server 'JFrogChina-Server'
    def rtGradle = Artifactory.newGradleBuild()
    def buildInfo = Artifactory.newBuildInfo()

    stage ('Code Check out') {
        git url: 'git@github.com:slash-l/app-simple-gradle.git', branch : "main"
    }

    stage ('Artifactory configuration') {
        rtGradle.tool = 'gradle-6.9.1' // Tool name from Jenkins configuration
//        rtGradle.useWrapper = true
        rtGradle.usesPlugin = true
        rtGradle.deployer repo: 'slash-gradle-virtual', server: server
        rtGradle.resolver repo: 'slash-gradle-virtual', server: server
    }

    stage ('Exec Gradle') {
        rtGradle.run rootDir: ".", buildFile: 'build.gradle', tasks: 'clean artifactoryPublish --stacktrace', buildInfo: buildInfo
    }

    stage ("Collect properties") {
        rtGradle.deployer.addProperty("status", "in-qa").addProperty("compatibility", "1", "2", "3")
    }

    stage ('Publish') {
        server.publishBuildInfo buildInfo
    }

    stage ("Xray scan") {
        def xrayConfig = [
                'buildName': env.JOB_NAME,
                'buildNumber': env.BUILD_NUMBER,
                'failBuild': false
        ]
        def xrayResults = server.xrayScan xrayConfig
//          echo xrayResults as String
        xrayurl = readJSON text:xrayResults.toString()
//          echo xrayurl as String
        rtGradle.deployer.addProperty("scan", "true")
        rtGradle.deployer.addProperty("xrayresult.summary.total_alerts", xrayurl.summary.total_alerts as String)

        rtGradle.deployer.deployArtifacts buildInfo
    }

}
