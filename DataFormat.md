
# SELogger Data Format

A program execution with SELogger produces two types of data: runtime events and static attributes.
The runtime events include data values observed for the bytecode instructions; 
each event is recorded before/after a particular bytecode instruction is executed.
Each runtime event is represented by a tuple: a data ID, an observed data value, a sequential number representing the chronological order, and a thread ID of the event.
The static attributes contains the information of the executed program.



## Execution Trace

The injected logging code calls `recordEvent` methods defined in class `selogger.logging.Logging`. 
It internally uses different logging implementations depending on the `format` option.


### Near-Omniscient Execution Trace (format=nearomni)

#### recentdata.json

The default `nearomni` mode produces a file in a JSON format.
The JSON object has a `events` field including an array of JSON objects.
Each object represents one or more occurrences of a particular runtime event.
Each object has the following fields.

|Field Name|Content|
|:----------|:------|
|cname      |The class name of the event location|
|mname      |The method name of the event location|
|mdesc      |The descriptor of the method of the event location|
|mhash      |A shortened method hash (the first 8 characeters of `MethodHash`) of the method|
|line       |The line number of the event location|
|inst       |The bytecode location corresponding to the event|
|event      |Event type name|
|attr       |Additional attributes of an instruction corresponding to the event|
|vtype      |The type of a recorded value for the event|
|freq       |The number of events observed in the execution|
|record     |The number of the events recorded in the file|
|value      |An array of recorded values.  This field does not exist if the vtype is `void`.|
|seqnum     |An array of sequential numbers representing the order of events|
|thread     |An array of thread IDs of events |

Each value recorded in the `value` field is a string, number, or a boolean value.
An object reference is recorded as an object ID string in the `typename@hash` format.
A string object is recorded with the content (`typename@hash:content`).
In the reference, `<GC>` indicates that the object is garbage collected.

The `attr` field is a JSON object including the following information.

|Field Name|Content|
|:---------|:------|
|owner     |A class name having the method or field used by the CALL/FIELD event|
|name      |A method/field/variable name used by the CALL/INVOKEDYNAMIC/FIELD/LOCAL event|
|desc      |A descriptor representing the parameter list and return value of the method used by the CALL event|
|type      |A type descriptor used by an event.  Object type for class instantiation, element type for array creation, parameter type for method calls, variable type for local variable access, and so on.|
|methodtype|A method type related to METHOD_ENTRY and CALL events: instance method, static method, or constructor|
|index     |An index to specify the position of an argument in a method call|
|varindex  |An index representing a local variable.  Two or more variables may use the same index if their scope are different.|
|opcode    |The actual bytecode instruction used for call and jump|
|jumpto    |Jump destination for a jump (branch) instruction|
|amount    |The amount incremented by an increment (IINC) instruction|
|location  |This attribute is used to indicate a special instruction at the end of method|
|created   |A constructor call has this attribute pointing to a bytecode location that created the object (In Java bytecode, an object is created and then its constructor is called)|	
|blocktype |A label has this attribute if this label is the beginning of a CATCH or FINALLY block|
|start     |The first instruction of a TRY block corresponding to the CATHC/FINALLY block|	
|end       |The last instruction of a TRY block corresponding to the CATHC/FINALLY block|	


An object reference is recorded as a JSON object that has `type` and `id` fields.
A `null` object is recorded if the object reference is null.
The id `<GC>` indicates that the object is garbage collected.


#### recentdata.txt

The `json=false` option produces a file named `recentdata.txt`.
It is a CSV format with a header line. 
The columns are the same as the json version.

Multiple value, seqnum, and thread columns are recorded using multiple columns (e.g. value1, value2, ...).




### Event Frequency (format=freq)

The `freq` mode produces a csv named `eventfreq.txt`.
The file has two columns: 
 - The first column represents `dataID` defined in `dataids.txt`.
 - The second column represents the number of occurrences of the event.

The file does not include dataIDs that never occurred at runtime.


### Empty Trace (format=discard)

The `discard` mode produce no files.


### Binary Omniscient Execuion Trace (format=omni)

In the binary omniscient mode, SELogger produces `.slg` files with a sequential number recording all runtime events observed during a program execution.

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

The object types and string contents are separately stored in the following files.

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


### Text-based Omniscient Execution Trace (format=textstream)

In this mode, SELogger produces a series of text files.
Other supplementary files are the same as the binary stream mode. 

|Column Index|Name      |Content|
|:-----------|:---------|:------|
|1           |Seqnum    |A sequential number representing the order of events|
|2           |Data ID   |A data ID representing the event type and source code location|
|3           |Thread ID |A thread that the event occurred|
|4           |Value     |A data value recorded for the event|

## Runtime Events

The following table is a list of events that can be recorded by SELogger.
The event name is defined in the class `selogger.EventType`.  


|Event group            |Event Type                |Timing       |Recorded Data|
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
|                           |DIVIDE|This event represents an arithmetic division instruction (IDIV).|The event itself is not directly recorded in a trace.  The dataId of this event may appear in LABEL events.|
|                           |LINE_NUMBER|This event represents an execution of a line of source code.  As a single line of code may be compiled into separated bytecode blocks, a number of LINE_NUMBER events having different data ID may point to the same line number.||



## Static Attributes

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
|LoadedFrom |The name of a JAR file or directory from which the class is loaded|
|FileName   |The class file name|
|ClassName  |The name of the class| 
|LogLevel   |The level of inserted logging code.  `Normal` indicates the weaving has been successfully finished.|
|ClassHash  |SHA-1 hash of the class bytecode|
|ClassLoaderID|A string representing a class loader that loaded the original class|

In Java, a program may load multiple instances of a class using multiple class loaders.  
For example, the DaCapo benchmark suite is a Java program that internally executes other programs.  Some of the executed programs independently load and use different versions of the same library.
Such classes can be distinguished by the `LoadedFrom`, `ClassHash`, and `ClassLoaderID` fields.

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

The data format is represented by `selogger.weaver.MethodInfo` class.
You can parse a line using its `parse(String)` method.


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
|Attributes |Extra attributes representing additional information about the bytecode instruction|

The data format is represented by `selogger.weaver.DataInfo` class.
You can parse a line using its `parse(String)` method.


