import java.io.File
import java.util.zip.GZIPInputStream
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder


@Grapes([
    @Grab(group='org.codehaus.jackson', module='jackson-mapper-asl', version='1.9.3'),
    @Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.5.2'),
    @Grab(group='org.apache.ant', module='ant', version='1.8.1')
])


class Downloader {
    def authUrl = "https://www.jenkins-ci.org/census/"
    def workingDir = new File("target")

    /**
     * gets all compressed JSON files from jenkins-ci
     */
    def getFiles(pwd) {

        def site = new HTTPBuilder( authUrl )
        site.auth.basic 'jenkins', pwd
        def doc = site.get( path:'/census/' )

        doc.depthFirst().collect { it }.findAll {
            it.name() == "A"
        }.each {
            def fileName = it.attributes()["href"]
            if(fileName.endsWith(".json.gz")){
                def fileUrl = '/census/'+fileName
                println "${it.text()}, $fileUrl"
                def targetArchive = new File(workingDir, fileName)
                targetArchive << site.get(contentType: ContentType.BINARY, path: fileUrl ) // java.io.ByteArrayInputStream
                uncompressGZIP(targetArchive)
                targetArchive.delete()
            }
        }
    }

    /**
     * uncompress the given gzip to the same location as the given archive
     * @param file the archive to uncompress
     * @return
     */
    def uncompressGZIP(File file){
        println "uncompress $file"
        GZIPInputStream gzipInputStream = null;
        FileInputStream fileInputStream = null;
        gzipInputStream = new GZIPInputStream(new FileInputStream(file));
        String outFilename = file.absolutePath.substring(0, file.absolutePath.length() - 3)
        OutputStream out = new FileOutputStream(outFilename);
        byte[] buf = new byte[1024];
        int len;
        while ((len = gzipInputStream.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        gzipInputStream.close();
        out.close();
    }

    def run(args) {
        if(args.size() != 1){
            println "no password for $authUrl given..."
        }else{
            // make sure working dir exists
            workingDir.deleteDir()
            workingDir.mkdirs()
            getFiles(args[0])
        }
    }
}

new Downloader().run(this.args)