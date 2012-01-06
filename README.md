Jenkins Usage Statistics
========================

Theses script are meant to generate some SVG graphics from existing JSON files collected by the jenkins-ci.org infrastructure.

see http://imod.github.com/jenkins-stats/

HOWTO
-----

1. download the scripts from jenkins-ci.org

   $> groovy download.groovy pwd

2. generate the graphs
   ... you might have to increase the memory: JAVA_OPTS="-Xmx4000M"

   $> groovy generateStats.groovy

The final SVGs will be in target/svg