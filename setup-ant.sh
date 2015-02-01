#!/bin/sh

target="android-21"

rm -rf external/appcompat
cp -r "${ANDROID_HOME}/extras/android/support/v7/appcompat" external/

# This library is already included by netcipher, but SHA1 of JARs differ
rm external/appcompat/libs/android-support-v4.jar

rm -rf external/palette
cp -r "${ANDROID_HOME}/extras/android/support/v7/palette" external/
mkdir external/palette/src

# Update ant setup in project and all sub-projects
for f in `find external/ -name project.properties`; do
    projectdir=`dirname $f`
    echo "Updating ant setup in $projectdir:"
    android update lib-project -p $projectdir -t $target
done
android update project -p . --subprojects -t $target --name Lightning
