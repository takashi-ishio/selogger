selogger
========

SELogger is a logging tool developed for software engineering research.
It enables to record a rich execution trace of a Java application.
This software component is a part of REMViewer presented in 
ICPC 2014 Tool Demo (http://dx.doi.org/10.1145/2597008.2597803).

SELogger requires JDK 1.8 for compilation and execution,
while the tool can process applications compiled under JDK 1.6, 1.7, and 1.8.
Since our research focuses on enterprise applications in Java, 
our implementation for new JDK features (e.g. INVOKEDYNAMIC) is experimental.

SELogger is dependent on ASM (http://asm.ow2.org/) and TROVE (http://trove.starlight-systems.com/).

Note: SELogger in the master branch is still maintained, 
but new features are not added because of its complicated implementation.
SELogger in the develop branch supports load-time weaving and
additional logging features with simplified data format.
The data format is incompatible with the master branch version.
