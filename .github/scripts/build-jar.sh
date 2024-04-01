#!/bin/sh

echo "current directory is $PWD"
ls -alh

echo "cleaning via mvn..."
mvn clean

echo "building JAR..."
mvn package

ls -alh target/
