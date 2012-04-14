@GrabConfig(systemClassLoader=true)
@Grab('org.xerial:sqlite-jdbc:3.7.2')
import org.sqlite.*
import java.sql.*
import java.util.zip.GZIPInputStream;

import groovy.xml.MarkupBuilder

class Generator {

    def db
    def statsDir

    def Generator(workingDir, db){
        this.db = db
        this.statsDir = new File(workingDir, "stats")
    }

    def generateInstallationsJson(targetDir) {

        def installations = [:]
        db.eachRow("select version, count(*) as number from jenkins group by version;") {
            installations.put it.version, it.number
        }

        def json = new groovy.json.JsonBuilder()
        json.installations(installations)
        new File(statsDir, "installations.json") << groovy.json.JsonOutput.prettyPrint(json.toString())
    }


    def generatePluginsJson(targetDir) {

        println "fetching plugin names..."
        def names = []
        // fetch all plugin names, excluding the private ones...
        db.eachRow("select name from plugin where name not like 'privateplugin%' group by name ;") { names << it.name }
        println "found ${names.size()} plugins"

        names.each{ name ->
            def month2number = [:]
            def file = new File(statsDir, "${name}.stats.json")
            // fetch the number of installations per plugin per month
            db.eachRow("select month, count(*) as number from plugin where name = $name group by month order by month ASC;") {
                month2number.put it.month, it.number
            }
            def json = new groovy.json.JsonBuilder()
            json.installations(month2number)
            file << groovy.json.JsonOutput.prettyPrint(json.toString())
            println "wrote: $file.absolutePath"
        }


    }


    def run() {

        // clean the stats directory
        statsDir.deleteDir()
        statsDir.mkdirs()

        generateInstallationsJson(statsDir)
        generatePluginsJson(statsDir)

    }
}


def workingDir = new File("target")
def db = DBHelper.setupDB(workingDir)
new Generator(workingDir, db).run()





