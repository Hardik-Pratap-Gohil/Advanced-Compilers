#!/bin/bash

rm *.class
rm visitor/*.class
rm syntaxtree/*.class

java -jar ./jtb.jar A3JavaOut.jj

# Generate Parser
javacc jtb.out.jj

# Compile Classes
javac Main.java

echo "=== TC01 ==="
java Main < ../testcase-output/TC01.java
echo "=== TC02 ==="
java Main < ../testcase-output/TC02.java
