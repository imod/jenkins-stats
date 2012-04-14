

import org.codehaus.jackson.JsonToken

import java.io.File
import java.text.SimpleDateFormat;

import org.codehaus.jackson.*
import org.codehaus.jackson.map.*

/**
 * A metric instance for one instance 
 */
class InstanceMetric {
    def jenkinsVersion
    def plugins
    def jobTypes
    def nodesOnOs
    def totalExecutors
}

/**
 * This parser treats a file as an input for one month and only uses the newest stats entry of each instanceId.
 * 
 * 
 * Note: Although groovy provides first class json support, we use jackson because of the amount of data we have to deal
 */
class JenkinsMetricParser {

    /**
     * Returns a map of "instanceId -> InstanceMetric" - only the newest entry for each instance is returned (latest of the given month, each file contains only data for one month).
     * SNAPSHOT versions are ignored too.
     */
    public Map parse(File file) throws Exception {

        println "parsing $file"

        def installations = [:]

        JsonFactory f = new org.codehaus.jackson.map.MappingJsonFactory();
        JsonParser jp = f.createJsonParser(file);

        JsonToken current;

        current = jp.nextToken();
        if (current != JsonToken.START_OBJECT) {
            println("Error: root must be object!");
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

                        // we only want the latest available date for each instance
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