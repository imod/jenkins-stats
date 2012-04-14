import org.sqlite.*
import java.sql.*
import java.util.zip.GZIPInputStream;

import groovy.xml.MarkupBuilder

@Grapes([
    @Grab(group='org.codehaus.jackson', module='jackson-mapper-asl', version='1.9.3'),
    @Grab('org.xerial:sqlite-jdbc:3.7.2'),
    @GrabConfig(systemClassLoader=true)
])


class Generator {

    def db
    def workingDir

    def Generator(workingDir, db){
        this.db = db
        this.workingDir = workingDir
    }

    def generateStats(file) {

        if(!DBHelper.doImport(db, file.name)){
            println "skip $file - already imported..."
            return
        }

        def dateStr = file.name.substring(0, 6)
        java.util.Date monthDate = java.util.Date.parse('yyyyMM', dateStr)

        JenkinsMetricParser p = new JenkinsMetricParser()
        def installations = p.parse(file)

        db.withTransaction({

            installations.each { instId, metric ->

                db.execute("insert into jenkins(instanceid, month, version) values( $instId, $monthDate, ${metric.jenkinsVersion})")

                metric.plugins.each { pluginName, pluginVersion ->
                    db.execute("insert into plugin(instanceid, month, name, version) values( $instId, $monthDate, $pluginName, $pluginVersion)")
                }

                metric.jobTypes.each { jobtype, jobNumber ->
                    db.execute("insert into job(instanceid, month, type, jobnumber) values( $instId, $monthDate, $jobtype, $jobNumber)")
                }

                metric.nodesOnOs.each { os, nodesNumber ->
                    db.execute("insert into node(instanceid, month, osname, nodenumber) values( $instId, $monthDate, $os, $nodesNumber)")
                }

                db.execute("insert into executor(instanceid, month, numberofexecutors) values( $instId, $monthDate, $metric.totalExecutors)")
            }

            db.execute("insert into importedfile(name) values($file.name)")
        })

        println "commited data for ${monthDate.format('yyyy-MM')}"
    }

    def run(filePattern) {
        if(filePattern){
            workingDir.eachFileMatch( ~"$filePattern" ) { file -> generateStats(file) }
        }else{
            workingDir.eachFileMatch( ~".*json" ) { file -> generateStats(file) }
            //workingDir.eachFileMatch( ~"201109.json" ) { file -> generateStats(file) }
            //workingDir.eachFileMatch( ~"200812.json" ) { file -> generateStats(file) }
        }
    }
}

def workingDir = new File("target")
def db = DBHelper.setupDB(workingDir)
new Generator(workingDir, db).run( args ? args[0] : null )




