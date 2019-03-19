# jitsi-utils
This project contains a set of basic Java utilities used in Jitsi
projects.

The aim is to reduce the interdependency between the different Jitsi
projects. For example we used to place code which needed to be shared
between ice4j, libjitsi and jitsi-media-transform in ice4j. This was
not great, because neither libjitsi not jitsi-media-transform need
to depend on ice4j.

This project's external dependencies should be kept to a minimum, and 
it should NOT depend on any other Jitsi project.
