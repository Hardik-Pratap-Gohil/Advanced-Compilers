#!/bin/bash

javac Main.java

echo "=== TC01 ==="
java Main < ../public-testcase/TC01.java
echo "=== TC02 ==="
java Main < ../public-testcase/TC02.java
