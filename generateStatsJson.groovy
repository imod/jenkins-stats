import java.util.zip.GZIPInputStream;

import groovy.xml.MarkupBuilder

@Grapes([
    @Grab(group='org.codehaus.jackson', module='jackson-mapper-asl', version='1.9.3')
])

class Generator {

    def workingDir = new File("target")
    def svgDir = new File(workingDir, "svg")

    def dateStr2totalJenkins = [:]
    def dateStr2totalNodes = [:]
    def dateStr2totalJobs = [:]
    def dateStr2totalPluginsInstallations = [:]


    def generateStats(file, targetDir) {

        JenkinsMetricParser p = new JenkinsMetricParser()
        def installations = p.parse(file)

        def version2number = [:]
        def plugin2number = [:]
        def jobtype2number = [:]
        def nodesOnOs2number = [:]
        def executorCount2number = [:]

        installations.each { instId, metric ->

            //        println instId +"="+metric.jenkinsVersion
            def currentNumber = version2number.get(metric.jenkinsVersion)
            def number = currentNumber ? currentNumber + 1 : 1
            version2number.put(metric.jenkinsVersion, number)

            metric.plugins.each { pluginName, pluginVersion ->
                def currentPluginNumber = plugin2number.get(pluginName)
                currentPluginNumber = currentPluginNumber ? currentPluginNumber + 1 : 1
                plugin2number.put(pluginName, currentPluginNumber)
            }

            metric.jobTypes.each { jobtype, jobNumber ->
                def currentJobNumber = jobtype2number.get(jobtype)
                currentJobNumber = currentJobNumber ? currentJobNumber + jobNumber : jobNumber
                jobtype2number.put(jobtype, currentJobNumber)
            }

            metric.nodesOnOs.each { os, nodesNumber ->
                def currentNodeNumber = nodesOnOs2number.get(os)
                currentNodeNumber = currentNodeNumber ? currentNodeNumber + nodesNumber : nodesNumber
                nodesOnOs2number.put(os, currentNodeNumber)
            }

            currentNumber = executorCount2number.get(metric.totalExecutors)
            number = currentNumber ? currentNumber + 1 : 1
            executorCount2number.put(metric.totalExecutors, number)

        }

        def nodesOs = []
        def nodesOsNrs = []
        nodesOnOs2number.each{os, number ->
            nodesOs.add(os)
            nodesOsNrs.add(number)
        }

        def simplename = file.name.substring(0, file.name.lastIndexOf("."))

        def totalJenkinsInstallations = version2number.inject(0){input, version, number -> input + number}
        createJson("Jenkins installations (total: $totalJenkinsInstallations)", new File(targetDir, "$simplename-jenkins.json"), version2number, false, {true}) 

        def totalPluginInstallations = plugin2number.inject(0){input, version, number -> input + number}
        createJson("Plugin installations (total: $totalPluginInstallations)", new File(targetDir, "$simplename-plugins.json"), plugin2number, true, {!it.key.startsWith("privateplugin")})
        createJson("Top Plugin installations (installations > 500)", new File(targetDir, "$simplename-top-plugins500.json"), plugin2number, true, {!it.key.startsWith("privateplugin") && it.value > 500})
        createJson("Top Plugin installations (installations > 1000)", new File(targetDir, "$simplename-top-plugins1000.json"), plugin2number, true, {!it.key.startsWith("privateplugin") && it.value > 1000})

        def totalJobs = jobtype2number.inject(0){input, version, number -> input + number}
        createJson("Jobs (total: $totalJobs)", new File(targetDir, "$simplename-jobs.json"), jobtype2number, true, {!it.key.startsWith("private")})

        def totalNodes = nodesOnOs2number.inject(0){input, version, number -> input + number}
        createJson("Nodes (total: $totalNodes)", new File(targetDir, "$simplename-nodes.json"), nodesOnOs2number, true, {true})
//
//        createPieSVG("Nodes", new File(targetDir, "$simplename-nodesPie.svg"), nodesOsNrs, 200, 300, 150, Helper.COLORS, nodesOs, 370, 20)
//
        def totalExecutors = executorCount2number.inject(0){ result, executors, number -> result + (executors * number)  }
        createJson("Executors per install (total: $totalExecutors)", new File(targetDir, "$simplename-total-executors.json"), executorCount2number, false, {true})
//
        def dateStr = file.name.substring(0, 6)
        dateStr2totalJenkins.put dateStr, totalJenkinsInstallations
        dateStr2totalPluginsInstallations.put dateStr, totalPluginInstallations
        dateStr2totalJobs.put dateStr, totalJobs
        dateStr2totalNodes.put dateStr, totalNodes

    }


    def createJson(def title, def jsonFile, def item2count, boolean sortByValue, Closure filter){
        
        jsonFile.delete()

        // filter unwanted items
        def item2number = item2count.findAll(filter)

//        def higestNr = item2number.inject(0){ input, version, number -> number > input ? number : input }

        if(sortByValue) {
            item2number = item2number.sort{ a, b -> a.value <=> b.value }
        }else{
            item2number = item2number.sort({ k1, k2 -> k1 <=> k2} as Comparator)
        }

        def content = [:]
        content.put 
        
        def json = new groovy.json.JsonBuilder()
        json.content {
            header title
            data item2number
        }
        
        jsonFile << groovy.json.JsonOutput.prettyPrint(json.toString())
        println "wrote: $jsonFile.absolutePath"

        
    }
    
    def createDataIndex() {
        def key2date = [:]
        def p = ~/^[0-9]{6}.*\.json/
        svgDir.eachFileMatch(p) { file->
            println file.getName()
            def dateStr = file.name.substring(0, 6)
            Date parsedDate = Date.parse('yyyyMM', dateStr)
            key2date.put (dateStr, parsedDate.format('yyyy-MM (MMMMM)'))
        }
        return  key2date
    }
        

    def run() {
        svgDir.deleteDir()
        svgDir.mkdirs()
        workingDir.eachFileMatch( ~"2010.*json" ) { file -> generateStats(file, svgDir) }
//        //        workingDir.eachFileMatch( ~"201109.json" ) { file -> generateStats(file, svgDir) }
//        //        workingDir.eachFileMatch( ~"200812.json" ) { file -> generateStats(file, svgDir) }
//
        createJson("Total Jenkins installations", new File(svgDir, "total-jenkins.json"), dateStr2totalJenkins, false, {true})
        createJson("Total Nodes", new File(svgDir, "total-nodes.json"), dateStr2totalNodes, false, {true})
        createJson("Total Jobs", new File(svgDir, "total-jobs.json"), dateStr2totalJobs, false, {true})
        createJson("Total Plugin installations", new File(svgDir, "total-plugins.json"), dateStr2totalPluginsInstallations, false, {true})
        
        createJson("data index", new File(svgDir, "data-index.json"), createDataIndex(), false, {true})
    }

}

new Generator().run()

