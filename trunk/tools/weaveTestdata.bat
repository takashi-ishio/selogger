cd /d %~dp0
cd ..
set LIB=bin;lib/asm-5.0.3.jar;lib/asm-analysis-5.0.3.jar;lib/asm-commons-5.0.3.jar;lib/asm-tree-5.0.3.jar;lib/asm-util-5.0.3.jar;lib/trove-3.0.2.jar
java -classpath %LIB% selogger.weaver.TraceWeaver -jdk17 -ignoreError -output=../experiment/selogger bin/selogger/testdata
pause
