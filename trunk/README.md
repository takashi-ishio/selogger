
# Selogger

SELogger is a tool to record an execution trace of a Java program.

## Build

Build a jar file with Maven.

        mvn package


## Usage

Execute your program with the Java Agent.

        java -javaagent:path/to/selogger-0.0.1-SNAPSHOT-jar-with-dependencies.jar [Application Options]

The agent accepts options.  Each option is specified by `option=value` style with commas (","). For example:

        java -javaagent:path/to/selogger-0.0.1-SNAPSHOT-jar-with-dependencies.jar=output=dirname,format=freq [Application Options]

 * `output=` specifies a directory to store an execution trace.  The directory is automatically created if it does not exist.
 * `weave=` specifies events to be recorded.
   * Supported event groups are: EXEC (entry/exit), CALL (call), PARAM (parameters for method entries and calls), FIELD (field access), ARRAY (array creation and access), OBJECT (constant object usage), SYNC (synchronized blocks), LOCAL (local variables), and LABEL (conditional branches).
   * The default mode records EXEC, CALL, PARAM, FIELD, ARRAY, OBJECT, and SYNC. 
 * `format=` specifies an output format. 
   * `freq` mode records only a frequency table of events.
   * `latest` mode records the frequency of events and the latest event data for each bytecode location.
   * `latesttime` mode records timestamps and thread IDs in addition to `latest` mode.
   * `discard` mode discard event data, while it injects logging code into classes.

 * `dump=true` stores class files including logging code into the output directory. It may help a debugging task if invalid bytecode is generated. 
 * In `latest` and `latesttime` mode, two additional options are available:
   * `size=` specifies the size of buffers.  The default is 32.
   * `keepobj=true` keeps objects in buffers to avoid GC.  While it may consume memory, more information is recorded.

 
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
