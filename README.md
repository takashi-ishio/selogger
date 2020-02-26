
# Selogger

SELogger is a tool to record an execution trace of a Java program.

## Build

Build a jar file with Maven.

        mvn package

SELogger uses ASM (http://asm.ow2.org/) for injecting logging code.
The class names are shaded by maven-shade-plugin so that 
SELogger can manipulate a program using ASM. 

### How to Build for JDK7

selogger works with JDK 7 while selogger's test cases requires JDK 8 to test the behavior of INVOKEDYNAMIC instructions.
If you would like to build a jar file for JDK7, please skip compilation of test classes as follows.
  - Prepare JDK 7 and Maven.
  - Replace the JDK version `1.8` with `1.7` for the `maven-compiler-plugin` in `pom.xml`.
  - Execute `mvn package -Dmaven.test.skip=true`.

## Usage

Execute your program with the Java Agent.

        java -javaagent:path/to/selogger-0.2.jar [Application Options]

The agent accepts options.  Each option is specified by `option=value` style with commas (","). For example:

        java -javaagent:path/to/selogger-0.2.jar=output=dirname,format=freq [Application Options]


### Output Options

The `output=` option specifies a directory to store an execution trace.  
The directory is automatically created if it does not exist.

The `format=` option specifies an output format. 
  * `freq` mode records only a frequency table of events.
  * `latest` mode records the latest event data for each bytecode location.
  * `latesttime` mode records timestamps and thread IDs in addition to `latest` mode. 
  * `nearomni` mode is an alias of `latesttime`.
  * `discard` mode discard event data, while it injects logging code into classes.

In `latest` and `latesttime` mode, two additional options are available:
  * `size=` specifies the size of buffers.  The default is 32.
  * `keepobj=true` keeps objects in buffers to avoid GC.  While it may consume memory, more information is recorded.


### Logging Target Options

The `weave=` option specifies events to be recorded. Supported event groups are: 

  * EXEC (entry/exit)
  * CALL (call)
  * PARAM (parameters for method entries and calls)
  * FIELD (field access)
  * ARRAY (array creation and access)
  * OBJECT (constant object usage)
  * SYNC (synchronized blocks)
  * LOCAL (local variables)
  * LABEL (conditional branches)

The default configuration records EXEC, CALL, PARAM, FIELD, ARRAY, OBJECT, and SYNC. 

The selogger inserts logging code into all the classes except for system classes: `sun/`,`com/sun/`, `java/`, and `javax/`.
You can add a prefix of class names whose behavior is excluded from the logging process using `e=` option.  Use multiple `e=` options to enumerate class paths.



### Option for Troubleshooting

The `dump=true` option stores class files including logging code into the output directory. It may help a debugging task if invalid bytecode is generated. 

 
## Differences from master branch version

The execution trace recorded in this version is incompatible with the master branch version.
The major differences are:
 * Simplified data format
 * Simplified instrumentation implementation
 * Simplified logging implementation (easy to extend)
 * Supported load-time weaving
 * Supported tracing jumps caused by exceptions
 * Supported fixed-size buffer logging
 * Improved reliability with JUnit test cases
