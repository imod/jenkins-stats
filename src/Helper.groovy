import java.io.File
import java.util.zip.GZIPInputStream
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder


@Grapes([
    @Grab(group='org.codehaus.jackson', module='jackson-mapper-asl', version='1.9.3'),
    @Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.5.2'),
    @Grab(group='org.apache.ant', module='ant', version='1.8.1')
])

class Helper {
    def static COLORS = [
        "BurlyWood",
        "CadetBlue",
        "red",
        "blue",
        "yellow",
        "green",
        "gold",
        "brown",
        "Azure",
        "pink",
        "khaki",
        "gray",
        "Aqua",
        "Aquamarine",
        "beige",
        "blueviolet",
        "Bisque",
        "coral",
        "darkblue",
        "crimson",
        "cyan",
        "darkred",
        "ivory",
        "lime",
        "maroon",
        "navy",
        "olive",
        "plum",
        "peru",
        "silver",
        "tan",
        "teal",
        "violet"
    ]


}

