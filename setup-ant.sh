#!/bin/sh

target="android-20"

# Travis doesn't support android-20 yet
if [ -n "$TRAVIS" ]; then
    target="android-19";
fi

# Update ant setup in project and all sub-projects
for f in `find external/ -name project.properties`; do
    projectdir=`dirname $f`
    echo "Updating ant setup in $projectdir:"
    android update lib-project -p $projectdir -t $target
done
android update project -p . --subprojects -t $target --name Lightning

cp libs/android-support-v4.jar external/netcipher/libnetcipher/libs/android-support-v4.jar
