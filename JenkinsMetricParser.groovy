

import org.codehaus.jackson.JsonToken

import java.io.File
import java.text.SimpleDateFormat;

import org.codehaus.jackson.*
import org.codehaus.jackson.map.*

@Grapes([
    @Grab(group='org.codehaus.jackson', module='jackson-mapper-asl', version='1.9.3')
])

class InstanceMetric {
    def jenkinsVersion
    def plugins
    def jobTypes
    def nodesOnOs
    def totalExecutors
}

class JenkinsMetricParser {

    public Map parse(File file) throws Exception {

        println "parsing $file"

        def installations = [:]

        JsonFactory f = new org.codehaus.jackson.map.MappingJsonFactory();
        JsonParser jp = f.createJsonParser(file);

        JsonToken current;

        current = jp.nextToken();
        if (current != JsonToken.START_OBJECT) {
            System.out.println("Error: root should be object: quiting.");
            return;
        }

        while (jp.nextToken() != JsonToken.END_OBJECT) {
            String instanceId = jp.getCurrentName();
            // move from field name to field value
            current = jp.nextToken();

            if(instanceId?.size() == 64){ // installation hash is 64 chars

                def availableStatsForInstance = 0

                def latestStatsDate
                def jobs
                def plugins
                def jVersion
                def nrOfnodes
                def nodesOnOs
                def totalExecutors

                if (current == JsonToken.START_ARRAY) {
                    while (jp.nextToken() != JsonToken.END_ARRAY) {
                        // read the record into a tree model,
                        // this moves the parsing position to the end of it
                        JsonNode jsonNode = jp.readValueAsTree();
                        // And now we have random access to everything in the object
                        def timestampStr = jsonNode.get("timestamp").getTextValue()  // 11/Oct/2011:05:14:43 -0400
                        Date parsedDate = Date.parse('dd/MMM/yyyy:HH:mm:ss Z', timestampStr)

                        if(!latestStatsDate || parsedDate.after(latestStatsDate)){

                            def versionStr = jsonNode.get("version").getTextValue()
                            // ignore SNAPSHOT versions
                            if(!versionStr.contains("SNAPSHOT") &&  !versionStr.contains("***")){
                                jVersion = versionStr ? versionStr : "N/A"

                                availableStatsForInstance++

                                latestStatsDate = parsedDate

                                jobs = [:]

                                def jobsNode = jsonNode.get("jobs");
                                jobsNode.getFieldNames().each { jobType -> jobs.put(jobType, jobsNode.get(jobType).intValue) };

                                plugins = [:]
                                jsonNode.get("plugins").each { plugins.put(it.get("name").textValue, it.get("version").textValue)} // org.codehaus.jackson.node.ArrayNode

                                nodesOnOs = [:]
                                totalExecutors = 0

                                jsonNode.get("nodes").each {
                                    def os = it.get("os") == null ? "N/A" : it.get("os")
                                    def currentNodesNumber = nodesOnOs.get(os)
                                    currentNodesNumber = currentNodesNumber ? currentNodesNumber + 1 : 1
                                    nodesOnOs.put(os, currentNodesNumber)
                                    def executors = it.get("executors")
                                    totalExecutors += executors.intValue
                                }
                            }
                        }
                    }
                }

                //                println ("available stats: $availableStatsForInstance")
                if(jVersion){ // && availableStatsForInstance >= 10 // take stats only if we have at least 10 stats snapshots
                    def metric = new InstanceMetric(jenkinsVersion: jVersion, plugins: plugins, jobTypes: jobs, nodesOnOs: nodesOnOs, totalExecutors: totalExecutors)
                    installations.put(instanceId, metric)
                }

                // jp.skipChildren();
            }
        }

        return installations
    }
}