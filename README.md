Jenkins Usage Statistics
========================

Theses script are meant to generate some SVG graphics from existing JSON files collected by the jenkins-ci.org infrastructure.

HOWTO
-----

1. download the scripts from jenkins-ci.org

   groovy download.groovy

2. generate the graphs

   groovy generateStats.groovy

The final SVGs will be in target/svg