
# SELogger Data Format

A program execution with SELogger produces two types of data: weaving result and runtime events.

SELogger supports various types of runtime events.
The types are listed in `selogger.EventType` class. 
Each event is recorded before/after a particular bytecode instruction is executed.
Hence, SELogger assigns `data ID` to each event based on the bytecode location (class, method, and instruction position in the method).
Using the data ID, users can distinguish events generated by different bytecode instructions.
Those static attributes are stored in the weaving result files.

The dynamic information, e.g. runtime values of variables recorded with events are stored in execution trace files.
The contents and format are dependent on the `format=` option.



## Weaving Result

The weaver component of SELogger analyzes a target program and injects logging instructions into code.
The component produces the following files including the result.

 - `weaving.properties`: The configuration options recognized by the weaver.
 - `log.txt`: Recording errors encountered during bytecode manipulation.
 - 
 - `classes.txt`: A list of woven classes.  The content is defined in the `selogger.weaver.ClassInfo` class.
 - `methods .txt`: A list of methods in the woven classes.  The content is defined in the `selogger.weaver.MethodInfo` class.
 - `dataids.txt`: A list of Data IDs. 

### classes.txt

Each line of this file represents a Java class manipulated by SELogger.
The file is a CSV having the following columns without a header line.

|Column Name|Content|
|:----------|:------|
|ClassID    |A sequential number assigned to the class|
|Container  |The name of a JAR file or directory from which the class is loaded|
|Filename   |The class file name|
|Classname  |The name of the class| 
|Level      |The level of inserted logging code.  `Normal` indicates the weaving has been successfully finished.|
|Hash       |SHA-1 hash of the class bytecode|
|LoaderID   |A string representing a class loader that loaded the original class|

The data format is represented by `selogger.weaver.ClassInfo` class.
You can parse a line using its `parse(String)` method.


### methods.txt
Each line of this file represents a Java method defined in a class included in `classes.txt`.
The file is a CSV having the following columns without a header line.

|Column Name|Content|
|:----------|:------|
|ClassID    |The ClassID in `classes.txt` of the class including the method|
|MethodID   |A sequential number assigned to this method|
|ClassName  |The name of the class.  This is redundant but for ease of use.|
|MethodName |The name of the method|
|MethodDesc |The descriptor of the method containing parameter and return value types.|
|Access     |The access modifiers of the method|
|SourceFileName|The source file name embedded by a compiler|
|MethodHash|SHA-1 hash of the byte array of the method body (before weaving)|


### dataids.txt

Each line of this file represents a runtime event corresponding to a Java bytecode instruction.
The file is a CSV having the following columns without a header line.

|Column Name|Content|
|:----------|:------|
|DataID     |A sequential number assigned to an event|
|ClassID    |ClassID for `classes.txt`|
|MethodID   |MethodID for `methods.txt`|
|Line       |Line number including the bytecode instruction|
|InstructionIndex|This points to an AbstractInsnNode object in `InsnList` of the ASM library.|
|EventType  |Event type name|
|ValueDesc  |The type of a "Recorded Data" value of the event|
|Attributes |Extra columns representing additional information about the bytecode instruction|



## Execution Trace

The injected logging code calls `recordEvent` methods defined in class `selogger.logging.Logging`. 
It internally uses different logging implementations depending on the `format` option.



### Near-Omniscient Execution Trace (format=nearomni)

The default `nearomni` mode produces a file named `recentdata.txt`.
Each line of the file includes the following data items in a CSV format:
 - Data ID representing an event
 - The number of the events observed in the execution 
 - The number of events recorded in the file
 - A list of recorded values (triples of a data value, a sequential number representing the order of recording, and a thread ID) for the events


### Frequency of events (format=freq)

The `freq` mode produces a csv named `eventfreq.txt`.
The file has two columns: 
 - The first column represents `dataID` defined in `dataids.txt`.
 - The second column represents the number of occurrences of the event.

The file does not include dataIDs that never occurred at runtime.


### Omniscient Execuion Trace (format=omni)

In this mode, the logging class produces `.slg` files with a sequential number recording all runtime events observed during a program execution.

The `selogger.reader.LogPrinter` class is to translate the binary format into a text format.

> java -classpath selogger-0.2.3.jar selogger.reader.LogPrinter selogger-output

The command accepts the following options.

 - `-from=N` skips the first N events.
 - `-num=M` terminates the program after printing M events.
 - `-thread=` specifies a list of thread ID separated with commas.  The program ignores events in other threads.
 - `-processparams` links parameter events to its METHOD ENTRY/CALL events.

The output format is like this:

> EventId=475,EventType=CALL,ThreadId=0,DataId=18009,Value=35,objectType=java.lang.String,method:CallType=Regular,Instruction=INVOKEVIRTUAL,Owner=java/lang/String,Name=indexOf,Desc=(Ljava/lang/String;)I,,org/apache/tools/ant/taskdefs/condition/Os:isOs,Os.java:260:38

Each line includes the following attributes.

- Event ID
- Event Type
- Thread ID
- DataID (to refer dataids.txt to its static information)
- Value associated with the event (in case of CALL, it is an object ID)
- objectType is the class name represented by an object ID (if the value points to an object)
- attributes recorded in dataids.txt
- Class Name, 
- File Name, Line Number (recorded as debug symbols)
- Instruction Index of the bytecode in the method (it is useful when you analyze the bytecode with the ASM library)

The object types and string contents are seperately stored in the following files.

#### LOG$Types.txt

The file records object types.
It is a CSV file including 6 columns.

- Type ID
- Type Name
- Class file location
- The superclass type ID
- The component type ID (available for an array type) 
- A string representation of class loader that loaded the type and followed by the class name.  This text is linked to the weaving information (`classes.txt`) described below.

#### LOG$ObjectTypesNNNNN.txt

This CSV file includes two columns.
Each row represents an object.

- The first column shows the object ID.
- The second column shows the type ID.

#### LOG$ExceptionNNNNN.txt

This is a semi-structured CSV file records messages and stack traces of exceptions thrown during a program execution.
Each exception is recorded by the following lines.

- Message line including three columns
  - Object ID of the Throwable object
  - A literal "M"
  - Textual message returned by `Throwable.getMessage()`
- Cause Object
  - Object ID of the Throwable object
  - A literal "CS"
  - Object ID of the cause object returned by `Throwable.getCause()`
  - If the object has suppressed exceptions (returned by `getSuppressed()`), their object IDs
- Stack Trace Elements (Each line corresponds to a single line of a stack trace)
  - Object ID of the Throwable object
  - A literal "S"
  - A literal "T" or "F": "T" represents the method is a native method.
  - Class Name
  - Method Name
  - File Name
  - Line Number

#### LOG$StringNNNNN.txt

The file records the contents of string objects used in an execution.
It is a CSV file format; each line has three fields.

- The object ID of the string
- The length of the string
- The content escaped as a JSON string



