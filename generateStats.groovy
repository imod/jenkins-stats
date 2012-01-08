import java.util.zip.GZIPInputStream;

import groovy.xml.MarkupBuilder

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

        }

        def nodesOs = []
        def nodesOsNrs = []
        nodesOnOs2number.each{os, number ->
            nodesOs.add(os)
            nodesOsNrs.add(number)
        }

        def simplename = file.name.substring(0, file.name.lastIndexOf("."))

        def totalJenkinsInstallations = version2number.inject(0){input, version, number -> input + number}
        createBarSVG("Jenkins installations (total: $totalJenkinsInstallations)", new File(targetDir, "$simplename-jenkins.svg"), version2number, 10, false, {true}) // {it.value >= 5})

        def totalPluginInstallations = plugin2number.inject(0){input, version, number -> input + number}
        createBarSVG("Plugin installations (total: $totalPluginInstallations)", new File(targetDir, "$simplename-plugins.svg"), plugin2number, 100, true, {!it.key.startsWith("privateplugin")})
        createBarSVG("Top Plugin installations (installations > 500)", new File(targetDir, "$simplename-top-plugins500.svg"), plugin2number, 100, true, {!it.key.startsWith("privateplugin") && it.value > 500})
        createBarSVG("Top Plugin installations (installations > 1000)", new File(targetDir, "$simplename-top-plugins1000.svg"), plugin2number, 100, true, {!it.key.startsWith("privateplugin") && it.value > 1000})

        def totalJobs = jobtype2number.inject(0){input, version, number -> input + number}
        createBarSVG("Jobs (total: $totalJobs)", new File(targetDir, "$simplename-jobs.svg"), jobtype2number, 1000, true, {!it.key.startsWith("private")})

        def totalNodes = nodesOnOs2number.inject(0){input, version, number -> input + number}
        createBarSVG("Nodes (total: $totalNodes)", new File(targetDir, "$simplename-nodes.svg"), nodesOnOs2number, 10, true, {true})

        createPieSVG("Nodes", new File(targetDir, "$simplename-nodesPie.svg"), nodesOsNrs, 200, 300, 150, Helper.COLORS, nodesOs, 370, 20)

        def dateStr = file.name.substring(0, 6)
        dateStr2totalJenkins.put dateStr, totalJenkinsInstallations
        dateStr2totalPluginsInstallations.put dateStr, totalPluginInstallations
        dateStr2totalJobs.put dateStr, totalJobs
        dateStr2totalNodes.put dateStr, totalNodes

    }



    def createBarSVG(def title, def svgFile, def item2number, def scaleReduction, boolean sortByValue, Closure filter){

        svgFile.delete()

        def higestNr = item2number.inject(0){ input, version, number -> number > input ? number : input }

        // ignore all private types
        item2number = item2number.findAll(filter) //{!it.key.startsWith("private")}

        if(sortByValue) {
            item2number = item2number.sort{ a, b -> a.value <=> b.value }
        }else{
            item2number = item2number.sort({ k1, k2 -> k1 <=> k2} as Comparator)
        }


        def viewWidth = (item2number.size() * 15) + 50

        def pwriter = new FileWriter(svgFile)
        def pxml = new MarkupBuilder(pwriter)
        pxml.svg('xmlns': 'http://www.w3.org/2000/svg', "version": "1.1", "preserveAspectRatio":'xMidYMid meet', "viewBox": "0 0 "+ viewWidth +" "+((higestNr / scaleReduction)+350)) {
            // 350 for the text/legend

            item2number.eachWithIndex { item, number, index ->

                def barHeight = number / scaleReduction

                def x = (index + 1) * 15
                def y = ((higestNr / scaleReduction) - barHeight) + 50 // 50 to get some space for the total text at the top
                rect(fill:"blue", height: barHeight, stroke:"black", width:"12", x:x, y:y) {
                }
                def ty = y + barHeight + 5
                def tx = x
                text(x:tx, y:ty, "font-family":'Tahoma', "font-size":'12', transform:"rotate(90 $tx,$ty)", "text-rendering":'optimizeSpeed', fill:'#000000;', "$item ($number)"){}
            }

            text(x:'10', y:'40', "font-family":'Tahoma', "font-size":'20', "text-rendering":'optimizeSpeed', fill:'#000000;', "$title"){}

        }

    }


    /**
     * www.davidflanagan.com/javascript5/display.php?n=22-8&f=22/08.js
     *
     * Draw a pie chart into an <svg> element.
     * Arguments:
     *   canvas: the SVG element (or the id of that element) to draw into.
     *   data: an array of numbers to chart, one for each wedge of the pie.
     *   cx, cy, r: the center and radius of the pie
     *   colors: an array of HTML color strings, one for each wedge
     *   labels: an array of labels to appear in the legend, one for each wedge
     *   lx, ly: the upper-left corner of the chart legend
     */
    def createPieSVG(def title, def svgFile, def data,def cx,def cy,def r,def colors,def labels,def lx,def ly) {

        // Add up the data values so we know how big the pie is
        def total = 0;
        for(def i = 0; i < data.size(); i++) total += data[i];

        // Now figure out how big each slice of pie is.  Angles in radians.
        def angles = []
        for(def i = 0; i < data.size(); i++) angles[i] = data[i]/total*Math.PI*2;

        // Loop through each slice of pie.
        def startangle = 0;

        def squareHeight = 30

        def viewWidth = lx + 350 // 350 for the text of the legend
        def viewHeight = ly + (data.size() * squareHeight) + 30 // 30 to get some space at the bottom
        def pwriter = new FileWriter(svgFile)
        def pxml = new MarkupBuilder(pwriter)
        pxml.svg('xmlns': 'http://www.w3.org/2000/svg', "version": "1.1", "preserveAspectRatio":'xMidYMid meet', "viewBox": "0 0 $viewWidth $viewHeight") {


            text("x": 30, // Position the text
                    "y": 40,
                    "font-family": "sans-serif",
                    "font-size": "16",
                    "$title, total: $total"){}


            data.eachWithIndex { item, i ->
                // This is where the wedge ends
                def endangle = startangle + angles[i];

                // Compute the two points where our wedge intersects the circle
                // These formulas are chosen so that an angle of 0 is at 12 o'clock
                // and positive angles increase clockwise.
                def x1 = cx + r * Math.sin(startangle);
                def y1 = cy - r * Math.cos(startangle);
                def x2 = cx + r * Math.sin(endangle);
                def y2 = cy - r * Math.cos(endangle);

                // This is a flag for angles larger than than a half circle
                def big = 0;
                if (endangle - startangle > Math.PI) {big = 1}

                // We describe a wedge with an <svg:path> element
                // Notice that we create this with createElementNS()
                //            def path = document.createElementNS(SVG.ns, "path");

                // This string holds the path details
                def d = "M " + cx + "," + cy +      // Start at circle center
                        " L " + x1 + "," + y1 +     // Draw line to (x1,y1)
                        " A " + r + "," + r +       // Draw an arc of radius r
                        " 0 " + big + " 1 " +       // Arc details...
                        x2 + "," + y2 +             // Arc goes to to (x2,y2)
                        " Z";                       // Close path back to (cx,cy)

                path(   d: d, // Set this path
                        fill: colors[i], // Set wedge color
                        stroke: "black", // Outline wedge in black
                        "stroke-width": "1" // 1 unit thick
                        ){}

                // The next wedge begins where this one ends
                startangle = endangle;

                // Now draw a little matching square for the key
                rect(   x: lx,  // Position the square
                        y: ly + squareHeight*i,
                        "width": 20, // Size the square
                        "height": squareHeight,
                        "fill": colors[i], // Same fill color as wedge
                        "stroke": "black", // Same outline, too.
                        "stroke-width": "1"){}

                // And add a label to the right of the rectangle
                text(   "x": lx + 30, // Position the text
                        "y": ly + squareHeight*i + 18,
                        "font-family": "sans-serif",
                        "font-size": "16",
                        "${labels[i]} ($item)"){}
            }
        }
    }

    def createHtml(dir) {
        def files = []
        dir.eachFileMatch( ~/\d+.*.svg/ ) { file ->
            // all files starting with numbers are assumed to be for a specifig date
            files << file.name
        }

        def specialFiles = []
        dir.eachFileMatch(  ~/[^\d].*.svg/ ) { file ->
            // all files not starting with a date, are treated more important
            specialFiles << file.name
        }

        files.sort()

        def fileGroups = files.groupBy { file ->
            file.substring(0, 6) // the first 6 chars are the date, group by it
        }

        def html = new File(dir, "svgs.html")
        def pwriter = new FileWriter(html)
        def phtml = new MarkupBuilder(pwriter)
        phtml.html() {
            head(){
                //                <!-- Le HTML5 shim, for IE6-8 support of HTML elements -->
                //                <!--[if lt IE 9]>
                //                  <script src="http://html5shim.googlecode.com/svn/trunk/html5.js"></script>
                //                <![endif]-->
                script(src: "https://ajax.googleapis.com/ajax/libs/jquery/1.6.4/jquery.min.js", type: "text/javascript", ""){}
                script(src: "http://twitter.github.com/bootstrap/1.4.0/bootstrap-modal.js", type: "text/javascript", ""){}
                script(src: "http://twitter.github.com/bootstrap/1.4.0/bootstrap-twipsy.js", type: "text/javascript", ""){}
                script(src: "http://twitter.github.com/bootstrap/1.4.0/bootstrap-popover.js", type: "text/javascript", ""){}

                link(rel: "stylesheet", href: "http://twitter.github.com/bootstrap/1.4.0/bootstrap.min.css"){}
            }
            body(){
                div("class":"container"){
                    div(id: "special"){
                        div(){ h1('Some statistics on the usage of Jenkins'){} }

                        table() {
                            tr(){
                                specialFiles.each { fileName ->
                                    td(){
                                        a(href: fileName, fileName){
                                            object(data: fileName, width: 200, type: "image/svg+xml")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    div(id: "byMonth"){
                        div(){ h1('Statistics by months'){} }

                        table(){

                            fileGroups.reverseEach { dateStr, fileList ->
                                tr(){
                                    Date parsedDate = Date.parse('yyyyMM', dateStr)
                                    td(parsedDate.format('yyyy-MM (MMMMM)')){}
                                    fileList.each{ fileName ->
                                        td(){
                                            a("class": "info", href: fileName, fileName, alt: fileName, "data-content": "<object data='$fileName' width='200' type='image/svg+xml'/>", rel: "popover","data-original-title": fileName)
                                        }
                                    }
                                }
                            }
                        }

                        script(popUpByMonth){}
                    }
                }
            }
        }
        println "generated: $html"
    }


    def popUpByMonth = """\$(function () {
 \$("a[rel=popover]")
 .popover({
   offset: 10,
   html: true,
   placement: 'right'
 })
})
"""

    def run() {
        svgDir.deleteDir()
        svgDir.mkdirs()
        workingDir.eachFileMatch( ~".*json" ) { file -> generateStats(file, svgDir) }
        //                workingDir.eachFileMatch( ~"201109.json" ) { file -> generateStats(file, svgDir) }
        //                workingDir.eachFileMatch( ~"200812.json" ) { file -> generateStats(file, svgDir) }

        createBarSVG("Total Jenkins installations", new File(svgDir, "total-jenkins.svg"), dateStr2totalJenkins, 100, false, {true})
        createBarSVG("Total Nodes", new File(svgDir, "total-nodes.svg"), dateStr2totalNodes, 100, false, {true})
        createBarSVG("Total Jobs", new File(svgDir, "total-jobs.svg"), dateStr2totalJobs, 1000, false, {true})
        createBarSVG("Total Plugin installations", new File(svgDir, "total-plugins.svg"), dateStr2totalPluginsInstallations, 1000, false, {true})
        createHtml(svgDir)
    }

}

new Generator().run()

