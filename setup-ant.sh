#!/bin/sh

target="android-21"

# Update ant setup in project and all sub-projects
for f in `find external/ -name project.properties`; do
    projectdir=`dirname $f`
    echo "Updating ant setup in $projectdir:"
    android update lib-project -p $projectdir -t $target
done
android update project -p . --subprojects -t $target --name Lightning

