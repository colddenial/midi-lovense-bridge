#!/bin/bash
if [ "$1" != "" ];
then
    echo Running Test: $1
else
    echo compilation test only!!!
fi
mvn clean
mvn package
if [ "$1" != "" ];
then
    java -jar target/midi-lovense-bridge-1.0-SNAPSHOT.jar $1
fi
