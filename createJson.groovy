@GrabConfig(systemClassLoader=true)
@Grab('org.xerial:sqlite-jdbc:3.7.2')
import org.sqlite.*
import java.sql.*
import java.util.zip.GZIPInputStream;

import groovy.xml.MarkupBuilder

class Generator {
    
    def db

    def workingDir = new File("target")
    def svgDir = new File(workingDir, "svg")

    def dateStr2totalJenkins = [:]
    def dateStr2totalNodes = [:]
    def dateStr2totalJobs = [:]
    def dateStr2totalPluginsInstallations = [:]

    def Generator(db){
        this.db = db
    }

    def generateJson(targetDir) {

        def installations = [:]
        db.eachRow("select version, count(*) as number from jenkins group by version;") { 
            println "${it.number} ${it.version}"
            installations.put it.version, it.number 
        }
        

        createJSON("installations.json", installations)
        

    }

    
    def createJSON(def file, def o){
        def json = new groovy.json.JsonBuilder()
        json.installations(o)
        println groovy.json.JsonOutput.prettyPrint(json.toString())
        new File(svgDir, file) << groovy.json.JsonOutput.prettyPrint(json.toString()) 
    }



    def run() {
        svgDir.deleteDir()
        svgDir.mkdirs()

        generateJson(svgDir)
        
//        createBarSVG("Total Jenkins installations", new File(svgDir, "total-jenkins.svg"), dateStr2totalJenkins, 100, false, {true})
//        createBarSVG("Total Nodes", new File(svgDir, "total-nodes.svg"), dateStr2totalNodes, 100, false, {true})
//        createBarSVG("Total Jobs", new File(svgDir, "total-jobs.svg"), dateStr2totalJobs, 1000, false, {true})
//        createBarSVG("Total Plugin installations", new File(svgDir, "total-plugins.svg"), dateStr2totalPluginsInstallations, 1000, false, {true})
//        createHtml(svgDir)
    }

}


def setupDB(){
    // in memory
//    sql = groovy.sql.Sql.newInstance("jdbc:sqlite::memory:","org.sqlite.JDBC")
    // persitent
    sql = groovy.sql.Sql.newInstance("jdbc:sqlite:target/test.db","org.sqlite.JDBC")
    
    return sql;
}

def db = setupDB()
new Generator(db).run()





