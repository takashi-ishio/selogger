
# SELogger: (Near-)Omniscient Logging Tool for Java

SELogger is a Java Agent to record an execcution trace of a Java program.
The tool name "SE" means Software Engineering, because the tool is developed for software engineering research topics including omniscient debugging. 

The design of this tool is partly explained in the following articles.
- Kazumasa Shimari, Takashi Ishio, Tetsuya Kanda, Naoto Ishida, Katsuro Inoue: "NOD4J: Near-omniscient debugging tool for Java using size-limited execution trace", Science of Computer Programming, Volume 206, 2021, 102630, ISSN 0167-6423, https://doi.org/10.1016/j.scico.2021.102630.
- Kazumasa Shimari, Takashi Ishio, Tetsuya Kanda, Katsuro Inoue: "Near-Omniscient Debugging for Java Using Size-Limited Execution Trace", ICSME 2019 Tool Demo Track, https://ieeexplore.ieee.org/abstract/document/8919216


## Usage

Execute your Java program with SELogger using `-javaagent` option as follows.

        java -javaagent:/path/to/selogger-0.6.0.jar [Application Options]

SELogger produces a file named `trace.json` including the execution trace.
The file format is described in the [DataFormat.md](DataFormat.md) file.
The DataFormat file also includes [the list of recordable runtime events](DataFormat.md#runtime-events).

To demonstrate the behavior, [a simple program](tests/selogger/testdata/Demo.java) is included in this repository.

```
     5:	public static void main(String[] args) {
     6:		int s = 0;
     7:		for (int i=0; i<10; i++) {
     8:			s = Integer.sum(s, i);
     9:		}
    10:		System.out.println(s);
    11:	}
```

An execution of the program with SELogger produces [an execution trace file](demo-trace.json).  In the trace file, you can find actual behavior of the program. 
For example, line 8 updates a local variable `s` 10 times.
The event occurrences are recorded as a single JSON object as follows.

```
{"cname":"selogger/testdata/Demo","mname":"main",...,
 "line":8,...,
 "event":"LOCAL_STORE",
 "attr"{"type":"int","name":"s",...},
 "freq":10,...,
 "value":[0,1,3,6,10,15,21,28,36,45],...},
```

The `cname`, `mname`, and `line` fields indicate the class name, method name, and line number to identify a source code location.
The `attr` field indicates that the event is recorded when an `int` value is stored to the variable `s`.
The `freq` field shows that the event occurred 10 times.
The `value` field shows actual values assigned to the variable.

SELogger also records various events such as method call parameters, return values, and field access.
A list of runtime events is available in the [DataFormat.md file](DataFormat.md#runtime-events).


### Configure options

SELogger accepts some options.  Each option is specified by `option=value` style with commas (","). For example:

        java -javaagent:/path/to/selogger-0.6.0.jar=format=freq,weaverlog=log.txt [Application Options]

If you would like to record the behavior of test cases executed by Maven Surefire, you can use an `argLine` option.

        mvn -DargLine="-javaagent:/path/to/selogger-0.6.0.jar=format=freq,weaverlog=log.txt" test

Instead of a command line option, you can write the same option in your `pom.xml` file as follows.

        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-surefire-plugin</artifactId>
          <configuration>
           <argLine>-javaagent:/path/to/selogger-0.6.0.jar=format=freq,weaverlog=log.txt</argLine>
           </configuration>
           <executions>...</executions>
         </plugin>



### Change a trace file name

You can change a trace file name from the default `trace.json` using the `trace=` option.
You can include `{time}` in the file name (e.g. `trace=trace-{time}.json`).  
The pattern `{time}` is replaced by the time of the program execution represented by the `yyyyMMdd-HHmmssSSS` format including year, month, day, hour, minute, second, and millisecond.
You can also explicitly specify the format like this: `{time:yyyyMMdd}`.  The format string is passed to `java.text.SimpleDateFormat` class.

It should be noted that SELogger overwrites existing trace files without warnings.
Programs executed with the same option (e.g. parallel testing of Maven Surefire Plugin) may accidentally overwrite execution traces recorded by previous executions. 

### Check a weaving process log

SELogger can produce a log including its internal process (e.g. the order of class loading).
You can specify a log file name using the `weaverlog=` option.
The file name also accepts the time pattern, e.g. `weaverlog=log-{time}.txt`.


### Specify an output directory for more details

The `output=` option specifies a directory to store the entire execution trace.  
The directory is automatically created if it does not exist.

The trace file and weaving log file are created in the directory if their file paths are not specified.


### Select a Trace Format

The `format=` option specifies a data format of an execution trace.
The default is `nearomni` format.  

  * `freq` mode records only a frequency table of events.
  * `nearomni` mode records the latest event data with timestamp and thread ID for each bytecode location.   `latest` mode is an alias of `nearomni`.
  * `omni` mode records all the events in a text stream.  `omnibinary` mode records all the events in a binary stream.
    * This option requires an output directory.  If `output=` option is not specified, `selogger-output` directory is created.
  * `discard` mode discard event data, while it injects logging code into classes.

In the `nearomni` mode, three additional options are available:
  * `size=` specifies the size of buffers (the number of recorded events per source code location).  The default is 32.
    * The nearomni mode creates buffers for each event location.  Each buffer consumes SIZE*20 bytes (e.g. 640 bytes in case of the default size). A large buffer size (or a large program) may cause OutOfMemoryError.  When SELogger detected OutOfMemory, it records an error message and discards the execution trace to continue the program execution.
  * `keepobj={strong|weak|id}` specifies how to record objects in a trace.
    * (Default) `keepobj=strong` keeps all objects in recent events. 
    * `keepobj=weak` keeps objects using weak references to avoid the impact to GC.  It reduces memory consumption, while some object information may be lost.
    * `keepobj=id` assigns unique IDs to objects in the same way as `format=omni`.  This option maintains an object-to-id map on memory but may reduce memory consumption.  For convenience, string and exception messages are also recorded in the trace file.
    * For compatibility with previous versions of SELogger, `keepobj={true|false}` is regarded as `keepobj={strong|weak}`, respectively. 
  * `json={true|false}` specifies whether the output file is written in a JSON format or not.
    * The default value is true.  If this is set to false, a CSV format is used.

The `omni` mode records more details about the execution trace.  By default, it records the contents of String objects and stack traces of exception objects.
- The `string=false` option discards the strings.
- The `exception=message` option records only exception messages.
- The `exception=none` option disables the recoding of stack traces.

The `omni` mode also has an additional option to record timestamps of events.
- The `timestamp=true` option adds a timestamp for each event.  Each value is returned by `System.currentTimeMilis()`.


### Select Event Types

The default configuration records all events in [the list of recordable runtime events](DataFormat.md#runtime-events).
It may slow down an execution significantly.
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


### Exclude Utilities and Libraries from Logging

Logging may generate a huge amount of events due to a particular sequence of instructions frequently executed in a loop.
You can manually exclude such utility classes from logging by specifying filtering options.

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


### Record a Specified Interval

SELogger supports four options to control the logging process: `logstart`, `logend`, `lognested`, and `logsave`.
SELogger records all events from the beginning to the end of a program execution by default.
A pair of `logstart=` and `logend=` options is available to specify an interval of interest (both `logstart=` and `logend=` must be specified to enable this filtering feature).

The logging is started when an event represented by `logstart` is observed.
The logging is terminated after an event represented by `logend` is observed.
The events between the `logstart` and `logend` events are included in an execution trace.
- If the `logstart` and `logend` point to the same event, only the event is recorded because the logging is enabled for the event but disabled again after the event.
- The start and end of logging are triggered irrelevant to the thread of control.  The logging started by a thread may be terminated by another thread.
- The interval represented by `logstart` and `logend` events can be nested if an additional option `lognested=true` is specified.   Otherwise, logging started by multiple `logstart` events can be terminated by a single `logend` event.

The options accept a text pattern comprising four elements: `ClassName#MethodName#MethodDesc#EventType`.
- The `ClassName`, `MethodName`, and `MethodDesc` elements are regular expressions representing class names, method names, and method descriptors, respectively.  
- The `EventType` element specifies a list of event types separated by `;`.  
  - The event types are listed in `selogger.EventType` class.  
  - `METHOD_EXIT` is a special keyword equivalent to `METHOD_NORMAL_EXIT;METHOD_EXCEPTIONAL_EXIT`.
- An empty pattern matches any text.

The timings of logging start and end are recorded in `log.txt`.
The logging on/off is switched by every occurrence of `logstart` and `logend` events.  A `logstart` event after a `logend` event restarts the logging.


#### Example patterns

|Pattern|Interval|
|:------|:-------|
|`logstart=my/Class###METHOD_ENTRY`|Logging starts when any method entry events of `my/Class` class|
|`logend=my/Class#testX##METHOD_EXIT`|Logging ends when `testX` method of `my/Class` is finished|
|`logstart=#test.+#\(\)V#METHOD_ENTRY`|Logging starts at the beginning of any `test` method without parameters and return values.  The parentheses are escaped because they are not a part of regular expression.|

The following options records method entry, exit, and executed lines during executions of `testX` method defined in `my/Class` class.
```
weave=EXEC+LINE,logstart=my/Class#testX##METHOD_ENTRY,logend=my/Class#testX##METHOD_EXIT
```

#### Saving an interval as a partial trace file

You can save a partial trace file when the logging has been suspended by a `logend` event:

- `logsave=snapshot` option saves a snapshot of the trace after the event.  
- `logsave=partial` option saves a partial trace and discards the recorded trace after the save.  If the full trace included multiple intervals, each interval is saved as separated files.

The options work when the format is `nearomni` and `freq`.



### Option for Troubleshooting

The `dump=true` option stores class files including logging code into the output directory. It may help a debugging task if invalid bytecode is generated. 


## Limitation

The logging feature for some instructions (in particular, JUMP, RET, INVOKEDYNAMIC instructions) has not been tested well due to the lack of appropriate test cases.

To record local variable names and line numbers, the tool uses debugging information embedded in class files. 


## Information for Developers

### Package Structure

SELogger comprises three sub-packages: `logging`, `reader`, and `weaver`.

  - The `weaver` sub-package is an implementation of Java Agent.  
    - RuntimeWeaver class is the entry point of the agent.  It calls ClassTransformer to inject logging instructions into target classes.
  - The `logging` sub-package implements logging features.
    - Logging class is the entry point of the logging feature.  It records runtime events in files.
  - The `reader` sub-package implements classes to read log files.
    - LogPrinter class is an example to read `.slg` files generated by logging classes. 

 
### Dependencies

SELogger uses the following libraries:
- ASM (http://asm.ow2.org/) to inject logging code.
- Jackson-Core to write an execution trace in a JSON format.

See `pom.xml` file for more details.

To avoid the conflict between the libraries for SELogger and for a target application,
the package names in our binary file are renamed by maven-shade-plugin.


### How to Build from Source Code

You can build a jar file with Maven.

        mvn package

The following command line uses the generated jar file to trace a test class in SELogger.

        java -javaagent:target/selogger-{version}.jar -classpath target/test-classes selogger.testdata.SimpleTarget

The resultant file `trace.json` includes an execution trace.


#### How to Build for JDK7

SELogger works with JDK 7 while selogger's test cases requires JDK 8 to test the behavior of INVOKEDYNAMIC instructions.
If you would like to build a jar file for JDK7, please skip compilation of test classes as follows.

  - Prepare JDK 7 and Maven.
  - Replace the JDK version `1.8` with `1.7` for the `maven-compiler-plugin` in `pom.xml`.
  - Execute `mvn package -Dmaven.test.skip=true`.

Jackson Core 2.13.x works with JDK 6, while its newer versions require JDK 8.



### Static Weaving

StaticWeaver is available to see the result of bytecode weaving. 
The feature can be executed as follows.


        java -classpath /path/to/selogger.jar selogger.weaver.StaticWeaver [weaving options] [target files]

The `[weaving options]` is the same as the argument for the runtime weaver. 
The target files may include class files, jar files, and directories including classes and jars.
The resultant files are stored in `selogger-output`.

The following command writes a class file with logging code to the `output-static` directory.

        java -classpath /path/to/selogger.jar selogger.weaver.StaticWeaver output=output-static,weave=LABEL,dump=true target/test-classes/selogger/testdata/SimpleTarget.class

We cannot directly execute the woven program, because RuntimeWeaver is the agent that starts and closes a Logger at runtime.


### Debug Configuration

As SELogger is loaded to a Java VM at runtime, the behavior can be controlled by a debugger.
In case of Eclipse, the following steps enable a single step execution on SELogger source code.

 - Import the SELogger project to the workspace and build it.
 - Prepare a target program to be analyzed by SELogger.
 - Open the "Debug Configurations" dialog.
 - Create a configuration for the target program and select a Main Class.
 - In the "Arguments" tab, add a VM argument to enable SELogger, for example: `-javaagent:${project_loc:selogger}\target\selogger-0.6.0.jar`.
 - In the "Source" tab, add the SELogger project to "Source Lookup Path."
 - Launch the Debug configuration.



## History

The developement of this tool has been supported by JSPS KAKENHI Grant No. JP18H03221.

The first version of SELogger (in `icpc204` branch) is a static weaver for omniscient debugging .
The execution trace recorded in the version is incompatible with the current version.
The major differences are:
 * Simplified data format
 * Simplified instrumentation implementation
 * Simplified logging implementation (easy to extend)
 * Supported load-time weaving
 * Supported tracing jumps caused by exceptions
 * Supported fixed-size buffer logging
 * Improved reliability with JUnit test cases

