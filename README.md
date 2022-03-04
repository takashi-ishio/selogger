
# SELogger

SELogger is a Java Agent to record an [execution trace](#execution-trace) of a Java program.
The tool name "SE" means Software Engineering, because the tool is developed for software engineering research topics including omniscient debugging. 

The design of this tool is partly explained in the following articles.
- Kazumasa Shimari, Takashi Ishio, Tetsuya Kanda, Naoto Ishida, Katsuro Inoue: "NOD4J: Near-omniscient debugging tool for Java using size-limited execution trace", Science of Computer Programming, Volume 206, 2021, 102630, ISSN 0167-6423, https://doi.org/10.1016/j.scico.2021.102630.
- Kazumasa Shimari, Takashi Ishio, Tetsuya Kanda, Katsuro Inoue: "Near-Omniscient Debugging for Java Using Size-Limited Execution Trace", ICSME 2019 Tool Demo Track, https://ieeexplore.ieee.org/abstract/document/8919216

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

        java -javaagent:path/to/selogger-0.3.2.jar [Application Options]

The agent accepts options.  Each option is specified by `option=value` style with commas (","). For example:

        java -javaagent:path/to/selogger-0.3.2.jar=output=dirname,format=freq [Application Options]


### Output Options

The `output=` option specifies a directory to store an execution trace.  
The directory is automatically created if it does not exist.
The default output directory is `selogger-output`.
You can include `{time}` in the directory name (e.g. `output=selogger-output-{time}`).  
The part is replaced by the time in the `yyyyMMdd-HHmmssSSS` format including year, month, day, hour, minute, second, and millisecond.
You can also explicitly specify the format like this: `{time:yyyyMMdd}`.  The format string is passed to `java.text.SimpleDateFormat` class.

The `format=` option specifies an output format.  The default is `latest` format.  The details of each option is described in the [DataFormat.md](DataFormat.md) file.

  * `freq` mode records only a frequency table of events.
  * `latest` mode records the latest event data with timestamp and thread ID for each bytecode location. 
  * `nearomni` mode is an alias of `latest`.
  * `omni` mode records all the events in a stream.
  * `discard` mode discard event data, while it injects logging code into classes.

In the `latest`/`nearomni` mode, three additional options are available:
  * `size=` specifies the size of buffers (the number of recorded events per source code location).  The default is 32.
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




### Logging Target Event Options

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
  * LINE (line numbers)
  * ALL (All events listed above)

You can add multiple groups in a single option using `+`.  
The dafault configuration is `EXEC+CALL+FIELD+ARRAY+SYNC+OBJECT+PARAM`.

The default configuration records all events. 

Note: The event category names EXEC and CALL come from AspectJ pointcut: execution and call.


### Excluding Libraries from Logging

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


### Option for Troubleshooting

The `dump=true` option stores class files including logging code into the output directory. It may help a debugging task if invalid bytecode is generated. 


## Execution Trace


The following table is a list of events that can be recorded by SELogger.
The event name is defined in the class `selogger.EventType`.  


|Event Category         |Event Name                |Timing       |Recorded Data|
|:----------------------|:-------------------------|:------------|:--------------------|
|Method Execution (EXEC)|METHOD_ENTRY              |The method is entered, before any instructions in the method is executed|Receiver object if the method is an instance method|
|                       |METHOD_PARAM              |Immediately after METHOD_ENTRY, before any instructions in the method is executed.  The number of the events is the same as the number of formal parameters.|Parameter given to the method|
|                       |METHOD_NORMAL_EXIT        |Before a return instruction (one of RETURN, IRETURN, ARETURN, LRETURN, FRETURN, or DRETURN) is executed|Returned value from the method (= The value passed to the return instruction)|
|                       |METHOD_EXCEPTIONAL_EXIT   |When the method is exiting by an exception|Exception thrown from the method to the caller| 
|                       |METHOD_OBJECT_INITIALIZED |Immediately after execution of `this()` or `super()` in a constructor, before any other instructions in the constructor is executed|Object initialized by the constructor|
|                       |METHOD_THROW              |Before a throw instruction (ATHROW) is executed|Exception thrown by the throw statement|
|Method Call (CALL)     |CALL                      |Before a method is called by a method call instruction (one of INVOKEVIRTUAL, INVOKESTATIC, and INVOKESPECIAL)|Receiver object|
|                       |CALL_PARAM                |Immediately after a CALL event, before executing the method invocation.  The number of the events is the same as the number of actual parameters.|Parameter passed to the callee|
|                       |CALL_RETURN               |After a method invocation instruction is executed|Returned value from the callee|
||NEW_OBJECT|When a `new` statement created an instance of some class.|This event does NOT record the object because the created object is not initialized (i.e. not accessible) at this point of execution.|
||NEW_OBJECT_CREATED|After a constructor call is finished.|Object initialized by the constructor call|
||INVOKE_DYNAMIC|Before INVOKEDYNAMIC instruction creates a function object.|(None)|
||INVOKE_DYNAMIC_PARAM|Immediately after INVOKE_DYNAMIC event.  |Parameter passed to INVOKEDYNAMIC instruction.|
||INVOKE_DYNAMIC_RESULT|After the INVOKEDYNAMIC instruction.|A function object created by the INVOKEDYNAMIC instruction.|
|Field access (FIELD)|GET_INSTANCE_FIELD|Before the instance field is read by a GETFIELD instruction|Object whose field is read|
|                    |GET_INSTANCE_FIELD_RESULT|After the field is read by a GETFIELD instruction|Value read from the field|
|                    |GET_STATIC_FIELD|After the static field is read by a GETSTATIC instruction|Value read from the field|
|                    |PUT_INSTANCE_FIELD|Before the instance field is written by a PUTFIELD instruction|Object whose field is written|
|                    |PUT_INSTANCE_FIELD_VALUE|Immediately after PUT_INSTANCE_FIELD, before the instance field is written|Value written to the field|
|                    |PUT_INSTANCE_FIELD_BEFORE_INITIALIZATION|Before the instance field is written by a PUTFIELD instruction.  This event is used when the object has not been initialized by a constructor but whose field is assigned; for example, when an anonymous inner class object stores the external context into its filed.|Value written to the field|
|                    |PUT_STATIC_FIELD|Before the static field is written by a PUTSTATIC instruction|Value written to the field|
|Array access (ARRAY)|ARRAY_LOAD|Before a value is read from the array by an array load instruction (one of AALOAD, BALOAD, CALOAD, DALOAD, FALOAD, IALOAD, LALOAD, and SALOAD)|Accessed array to read|
|                    |ARRAY_LOAD_INDEX|Immediately after ARRAY_LOAD event.|Accessed index to read|
|                    |ARRAY_LOAD_RESULT|After a value is loaded from the array.|Value read from the array|
|                    |ARRAY_STORE|Before a value is written to the array by an array store instruction (one of AASTORE, BASTORE, CASTORE, DASTORE, FASTORE, IASTORE, LASTORE, and SASTORE)|Accessed array to write|
|                    |ARRAY_STORE_INDEX|Immediately after ARRAY_STORE event|Accessed index to write|
|                    |ARRAY_STORE_VALUE|Immediately after ARRAY_STORE_INDEX event|Value written to the array|
|                    |NEW_ARRAY|Before an array is created.|Length of the new array to be created|
|                    |NEW_ARRAY_RESULT|After an array is created.|Created array object|
|                    |MULTI_NEW_ARRAY|After a multi-dimendioanl array is created.|Created array object|
|                    |MULTI_NEW_ARRAY_OWNER|A sequence of MULTI_NEW_ARRAY_OWNER followed by MULTI_NEW_ARRAY_ELEMENT events are recorded immediately after a MULTI_NEW_ARRAY event.  The events represent a recursive structure of multi-dimensional arrays (An OWNER array has a number of ELEMENT arrays).|An array object which contains array objects|
|                    |MULTI_NEW_ARRAY_ELEMENT (`new` instruction for arrays)|This event is generated to record an array object contained in an owner array.|Array object contained in the owner array.|
|                    |ARRAY_LENGTH|Before the length of the array is read by an ARRAYLENGTH instruction|Array whose length is referred.   This may be null.|
|                    |ARRAY_LENGTH_RESULT|After the execution of the ARRAYLENGTH instruction. |The length of the array|
|Synchronized block (SYNC)|MONITOR_ENTER|When a thread of control reached the synchronized block, before entering the block.|Object to be locked by the synchronized block.|
|                         |MONITOR_ENTER_RESULT|When a thread of control entered the synchronized block, before any instructions in the block is executed|Object locked by the synchronized block.|
|                         |MONTIOR_EXIT|When a thread of control is exiting the synchronized block.  |Object to be unlocked by the block|
|Object manipulation (OBJECT)|OBJECT_CONSTANT_LOAD|When a constant object (usually String) is loaded by the instruction.|Object loaded by the instruction.  This may be null.|
|                            |OBJECT_INSTANCEOF|Before the INSTANCEOF instruction is executed.|Object whose class is checked by the instruction|
|                            |OBJECT_INSTANCEOF_RESULT|After the INSTANCEOF instruction is executed.|Result (true or false) of the instruction|
|Local variables (LOCAL)|LOCAL_LOAD|Before the value of the local variable is read by a local variable instruction (one of ALOD, DLOAD, FLOAD, ILOAD, and LLOAD)|Value read from the variable|
|                       |LOCAL_STORE|Before the value is written to the local variable by an instruction (one of ASTORE, DSTORE, FSTORE, ISTORE, and LSTORE) |Value written to the variable|
|                       |LOCAL_INCREMENT|After the local variable is updated by an IINC instruction.  An IINC instruction is not only for `i++`; it is also used for `i+=k` and `i-=k` if `k` is constant (depending on a compiler).|Value written to the variable by an increment instruction|
|                       |RET|This event corresponds to a RET instruction for subroutine call.  The current version does not generate this event.||
|Control-flow events (LABEL)|LABEL|This event is recorded when an execution passed a particular code location. LABEL itself is not a Java bytecode, a pseudo instruction inserted by ASM bytecode manipulation library used by SELogger.|A dataId corresponding to the previous program location is recorded so that a user can trace a control-flow path.|
|                           |CATCH_LABEL|This event is recorded when an execution entered a catch/finally block.|A dataId corresponding to the previous program location (that is likely where an exception was thrown) is recorded.|
|                           |CATCH|When an execution entered a catch/finally block, immediately after a CATCH_LABEL event, before any instructions in the catch/finally block is executed.|Exception object caught by the block|
|                           |JUMP|This event represents a jump instruction in bytecode. |The event itself is not directly recorded in a trace.  The dataId of this event may appear in LABEL events.|
|                           |DEVIDE|This event represents an arithmetic division instruction (IDIV).|The event itself is not directly recorded in a trace.  The dataId of this event may appear in LABEL events.|
|                           |LINE_NUMBER|This event represents an execution of a line of source code.  As a single line of code may be compiled into separated bytecode blocks, a number of LINE_NUMBER events having different data ID may point to the same line number.||





## Package Structure

SELogger comprises three sub-packages: `logging`, `reader`, and `weaver`.

  - The `weaver` sub-package is an implementation of Java Agent.  
    - RuntimeWeaver class is the entry point of the agent.  It calls ClassTransformer to inject logging instructions into target classes.
  - The `logging` sub-package implements logging features.
    - Logging class is the entry point of the logging feature.  It records runtime events in files.
  - The `reader` sub-package implements classes to read log files.
    - LogPrinter class is an example to read `.slg` files generated by logging classes. 



 
 
## Limitation

The logging feature for some instructions (in particular, JUMP, RET, INVOKEDYNAMIC instructions) has not been tested well due to the lack of appropriate test cases.

To record Local variable names and line numbers, the tool uses debugging information embedded in class files. 


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


