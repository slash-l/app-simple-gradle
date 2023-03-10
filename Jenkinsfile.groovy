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
    }

    stage ("Promotion") {
        def promotionConfig = [
                // Mandatory parameters
                'targetRepo'         : 'slash-gradle-test-local',

                // Optional parameters
                // The build name and build number to promote. If not specified, the Jenkins job's build name and build number are used
                'buildName'          : buildInfo.name,
                'buildNumber'        : buildInfo.number,

                // Comment and Status to be displayed in the Build History tab in Artifactory
                'comment'            : 'this is the promotion comment',

                // Specifies the source repository for build artifacts.
                'sourceRepo'         : 'slash-gradle-dev-local',
                // Indicates whether to promote the build dependencies, in addition to the artifacts. False by default
                'includeDependencies': true,
                // Indicates whether to copy the files. Move is the default
                'copy'               : false
                // Indicates whether to fail the promotion process in case of failing to move or copy one of the files. False by default.
//                'failFast'           : true
        ]

        // Promote build
        server.promote promotionConfig
    }

}
