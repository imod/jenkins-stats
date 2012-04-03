@GrabConfig(systemClassLoader=true)
@Grab('org.xerial:sqlite-jdbc:3.7.2')
import org.sqlite.*
import java.sql.*
import java.util.zip.GZIPInputStream;

import groovy.xml.MarkupBuilder

class Generator {

    def db
    def workingDir

    def Generator(workingDir, db){
        this.db = db
        this.workingDir = workingDir
    }

    def generateStats(file) {

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
        })


        println "commited data for $monthDate"
    }


    def run() {
        workingDir.eachFileMatch( ~".*json" ) { file -> generateStats(file) }
        //        workingDir.eachFileMatch( ~"201109.json" ) { file -> generateStats(file) }
        //workingDir.eachFileMatch( ~"200812.json" ) { file -> generateStats(file) }
    }
}


def setupDB(workingDir){

    def dbFile = new File(workingDir,"test.db")
    if(dbFile.exists()){
        dbFile.delete()
    }

    // in memory
    //    sql = groovy.sql.Sql.newInstance("jdbc:sqlite::memory:","org.sqlite.JDBC")
    // persitent
    sql = groovy.sql.Sql.newInstance("jdbc:sqlite:"+dbFile.absolutePath,"org.sqlite.JDBC")

    sql.execute("create table jenkins(instanceid, month, version)")
    sql.execute("create table plugin(instanceid, month, name, version)")
    sql.execute("create table job(instanceid, month, type, jobnumber)")
    sql.execute("create table node(instanceid, month, osname, nodenumber)")
    sql.execute("create table executor(instanceid, month, numberofexecutors)")

    return sql;
}

def workingDir = new File("target")
def db = setupDB(workingDir)
new Generator(workingDir, db).run()

db.eachRow("select rowid,instanceid, version from jenkins;") { println "${it.rowid}: ${it.instanceid} ${it.version}"}

db.eachRow("SELECT name, month, count(*) as count from plugin group by name;") { println "${it.name}, ${it.month}: ${it.count}"}





