
# Selogger

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
   * `freq` mode records a frequency table of events.
   * `fixed` mode records only the latest events for each data point.
