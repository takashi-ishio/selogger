
# SELogger: (Near-)Omniscient Logging Tool for Java

SELogger is a Java Agent to record an execcution trace of a Java program.
The tool name "SE" means Software Engineering, because the tool is developed for software engineering research topics including omniscient debugging. 

The design of this tool is partly explained in the following articles.
- Kazumasa Shimari, Takashi Ishio, Tetsuya Kanda, Naoto Ishida, Katsuro Inoue: "NOD4J: Near-omniscient debugging tool for Java using size-limited execution trace", Science of Computer Programming, Volume 206, 2021, 102630, ISSN 0167-6423, https://doi.org/10.1016/j.scico.2021.102630.
- Kazumasa Shimari, Takashi Ishio, Tetsuya Kanda, Katsuro Inoue: "Near-Omniscient Debugging for Java Using Size-Limited Execution Trace", ICSME 2019 Tool Demo Track, https://ieeexplore.ieee.org/abstract/document/8919216

The developement of this tool has been supported by JSPS KAKENHI Grant No. JP18H03221.


## Usage

Execute your Java program with SELogger using `-javaagent` option as follows.

        java -javaagent:path/to/selogger-0.4.0.jar [Application Options]

SELogger accepts some options.  Each option is specified by `option=value` style with commas (","). For example:

        java -javaagent:path/to/selogger-0.4.0.jar=output=dirname,format=freq [Application Options]

By default, SELogger creates a directory named `selogger-output` for an execution trace.

The created files are described in the [DataFormat.md](DataFormat.md) file.
The file includes [the list of recordable runtime events](DataFormat.md#runtime-events).


### Specify an output directory

The `output=` option specifies a directory to store an execution trace.  
The directory is automatically created if it does not exist.
You can include `{time}` in the directory name (e.g. `output=selogger-output-{time}`).  
The part is replaced by the time in the `yyyyMMdd-HHmmssSSS` format including year, month, day, hour, minute, second, and millisecond.
You can also explicitly specify the format like this: `{time:yyyyMMdd}`.  The format string is passed to `java.text.SimpleDateFormat` class.


### Select a Trace Format

The `format=` option specifies a data format of an execution trace.
The default is `nearomni` format.  

  * `freq` mode records only a frequency table of events.
  * `nearomni` mode records the latest event data with timestamp and thread ID for each bytecode location. 
  * `latest` mode is an alias of `nearomni`.
  * `omni` mode records all the events in a stream.
  * `discard` mode discard event data, while it injects logging code into classes.

In the `nearomni` mode, three additional options are available:
  * `size=` specifies the size of buffers (the number of recorded events per source code location).  The default is 32.
    * The nearomni mode creates buffers for each event location.  Each buffer consumes SIZE*20 bytes (e.g. 640 bytes in case of the default size). A large buffer size (or a large program) may cause OutOfMemoryError.  When SELogger detected OutOfMemory, it records an error message and discards the execution trace to continue the program execution.
  * `keepobj={strong|weak|id}` specifies how to record objects in a trace.
    * (Default) `keepobj=strong` keeps all objects in recent events. 
    * `keepobj=weak` keeps objects using weak references to avoid the impact to GC.  It reduces memory consumption, while some object information may be lost.
    * `keepobj=id` assigns unique IDs to objects in the same way as `format=omni`.  This option maintains an object-to-id map on memory but may reduce memory consumption.
    * For compatibility with previous versions of SELogger, `keepobj={true|false}` is regarded as `keepobj={strong|weak}`, respectively. 
  * `json={true|false}` specifies whether the output file is written in a JSON format or not.
    * The default value is false.

SELogger records the contents of String objects and stack traces of exception objects when creating an object-to-id map (`format=omni` or `keepobj=id` is specified).
- The `string=false` option discards the strings.
- The `exception=message` option records only exception messages.
- The `exception=none` option disables the recoding of stack traces.


### Select Event Types

The default configuration records all events in [the list of recordable runtime events](DataFormat.md#runtime-events).
If you are interested in only a subset of the events, you can exclude uninteresting events from logging.
This option affects the runtime performance because SELogger does not inject logging code for the excluded events.

The `weave=` option specifies events to be recorded. 
Supported event groups are: 

  * EXEC (entry/exit)
  * CALL (call)
  * PARAM (parameters for method entries and calls)
  * FIELD (field access)
  * ARRAY (array creation and access)
  * OBJECT (constant object usage)
  * SYNC (synchronized blocks)
  * LOCAL (local variables)
  * LABEL (conditional branches)
  * LINE (line numbers)
  * ALL (All events listed above)

The event group names EXEC and CALL come from AspectJ pointcut: execution and call.

You can add multiple groups in a single option using `+` (e.g., `weave=EXEC+CALL` method execution and call events).  


### Exclude Libraries from Logging

Logging may generate a huge amount of events. 
You can manually exclude some classes from logging by specifying filtering options.

Another reason to exclude some libraries is to avoid breaking library code.
As SELogger inserts logging code into classes at runtime, the behavior may break some classes, e.g. those using a custom class loader.
For example, SELogger excludes JavaFX classes from logging to avoid crash (`NoClassDefFoundError`).

You can find a list of loaded classes in the `log.txt` file generated by SELogger.  
A message `Weaving executed: [Class Name] loaded from [URI]` shows a pair of class name and location.


#### Filtering by Package and Class Names

Using `e=` option, you can specify a prefix of class names excluded from the logging process.  
You can use multiple `e=` options to enumerate class paths.
By default, the selogger excludes the system classes from logging: `sun/`,`com/sun/`, `java/`, `javax/`, and `javafx/`.

If a class is excluded from logging by this filter, a log message `Excluded by name filter: (the class name)` is recorded in a log file.

If you would like to add logging code to some filtered classes, you can use `i=` option.  
It also specifies a prefix of class names.  A class having the prefix is included in logging even if it matches the `e=` option.


#### Filtering by File Location

Using `exlocation=` option, you can exclude classes loaded from a particular directory or JAR file.
If a specified string is included in a URI where the class is loaded, that class is excluded from logging.
For example, you can exclude classes loaded from Maven dependencies by using `exlocation=.m2` option.

You can use multiple `exlocation=` options to enumerate file paths.
By default, no location filter is configured.

If a class is excluded from logging by this filter, a log message `Excluded by class filter: (the class name) loaded from (location name)` is recorded in a log file.


#### Infinite loop risk 

The security manager mechanism of Java Virtual Machine may call a `checkPermission` method to check whether a method call is allowed in the current context or not.
When a custom security manager having a `checkPermission` method is defined in a target program, SELogger injects logging code into the method by default. 
The logging code tries to record an execution of `checkPermission`.  
The logging step triggers an additional `checkPermission` call, and results in infinite recursive calls.

To reduce the risk of infinite recursive calls, SELogger automatically detects a subclass of SecurityManager and exclude the class from weaving.
If such a class is detected, a log message `Excluded security manager subclass` is recorded.
If you would like to weave logging code into such a subclass, add `weavesecuritymanager=true` option.


### Recording a Specified Interval

SELogger records all events from the beginning to the end of a program execution by default.
A pair of `logstart=` and `logend=` options is available to specify an interval of interest (both `logstart=` and `logend=` must be specified to enable this filtering feature).

The logging is started when an event represented by `logstart` is observed.
The logging is terminated after an event represented by `logend` is observed.
The events between the `logstart` and `logend` events are included in an execution trace.

The options accept a text pattern comprising four elements: `ClassName#MethodName#MethodDesc#EventType`.
- The `ClassName`, `MethodName`, and `MethodDesc` elements are regular expressions representing class names, method names, and method descriptors, respectively.  
- The `EventType` element specifies a list of event types separated by `;`.  The event types are listed in `selogger.EventType` class.  `METHOD_EXIT` is a special keyword that matches both `METHOD_NORMAL_EXIT` and `METHOD_EXCEPTIONAL_EXIT`.
- An empty pattern matches any text.


Example patterns:
|Pattern|Interval|
|:------|:-------|
|`logstart=my/Class###METHOD_ENTRY`|Logging starts when any method entry events of `my/Class` class|
|`logend=my/Class#testX##METHOD_EXIT`|Logging ends when `testX` method of `my/Class` is finished|
|`logstart=my/Class#test.+#\(\)V#METHOD_ENTRY`|Logging starts at the beginning of any `test` method of `my/Class` class without parameters and return values.  The parentheses are escaped because they are not a part of regular expression.|


The start and end of logging are triggered irrelevant to the thread of control.  The logging started by a thread may be terminated by another thread.
The timings of logging start and end are recorded in `log.txt`.
The logging on/off is switched by every occurrence of `logstart` and `logend` events.  A `logstart` event after a `logend` event restarts the logging.



### Option for Troubleshooting

The `dump=true` option stores class files including logging code into the output directory. It may help a debugging task if invalid bytecode is generated. 


## Limitation

The logging feature for some instructions (in particular, JUMP, RET, INVOKEDYNAMIC instructions) has not been tested well due to the lack of appropriate test cases.

To record Local variable names and line numbers, the tool uses debugging information embedded in class files. 


## Package Structure

SELogger comprises three sub-packages: `logging`, `reader`, and `weaver`.

  - The `weaver` sub-package is an implementation of Java Agent.  
    - RuntimeWeaver class is the entry point of the agent.  It calls ClassTransformer to inject logging instructions into target classes.
  - The `logging` sub-package implements logging features.
    - Logging class is the entry point of the logging feature.  It records runtime events in files.
  - The `reader` sub-package implements classes to read log files.
    - LogPrinter class is an example to read `.slg` files generated by logging classes. 

 
## How to Build from Source Code

You can build a jar file with Maven.

        mvn package


### How to Build for JDK7

selogger works with JDK 7 while selogger's test cases requires JDK 8 to test the behavior of INVOKEDYNAMIC instructions.
If you would like to build a jar file for JDK7, please skip compilation of test classes as follows.
  - Prepare JDK 7 and Maven.
  - Replace the JDK version `1.8` with `1.7` for the `maven-compiler-plugin` in `pom.xml`.
  - Execute `mvn package -Dmaven.test.skip=true`.


### Dependencies

SELogger uses:
- ASM (http://asm.ow2.org/) to inject logging code.
- Jackson-Core to write an execution trace in a JSON format.

See `pom.xml` file for more details.

To avoid the conflict between the libraries for SELogger and for a target application,
the package names in our binary file are renamed by maven-shade-plugin.


## History

The first version of SELogger (in `icpc204` branch) is a static weaver for omniscient debugging .
The execution trace recorded in this version is incompatible with the branch version.
The major differences are:
 * Simplified data format
 * Simplified instrumentation implementation
 * Simplified logging implementation (easy to extend)
 * Supported load-time weaving
 * Supported tracing jumps caused by exceptions
 * Supported fixed-size buffer logging
 * Improved reliability with JUnit test cases

Please note that the documentation in the `doc` directory was written for the old version.
We still keep the files for the record.

