#!/bin/sh

target="android-19"

for f in `find external/ -name project.properties`; do
projectdir=`dirname $f`
    echo "Updating ant setup in $projectdir:"
    android update lib-project -p $projectdir -t $target
done
android update project -p . --subprojects -t $target --name Lightning
