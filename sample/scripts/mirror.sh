#!/bin/bash
# #!/usr/local/bin/bash
# comment #!/bin/bash

if [ "$#" -ne 1 ]; then
    echo "Illegal number of parameters,
     You must precise the name of path file.
     sample : ./mirror.sh version"
    exit 0
fi

version=$1

PROJECT_BASE_PATH=$PWD

# we copy the files in repository in new repository
rm -rf mirror
mkdir mirror
cd mirror

git clone git@github.com:Mappy/mappy_dataxy_android.git mappydataxy -o client


cd mappydataxy
#Keeping gradle.properties and build.gradle project files
cp build.gradle ../build.gradle
cp gradle.properties ../gradle.properties
rm -rf *
cp ../build.gradle build.gradle
cp ../gradle.properties gradle.properties

cp -R $PROJECT_BASE_PATH/samples/mappy_data_xy_sample sample
# we don't want to have the implementation of module in sample so we change it
sed -i "" "s/implementation project(path: ':mappy-dataxy')/implementation \"com.mappy:mappy-dataxy:$version\"/g" sample/build.gradle
cp  $PROJECT_BASE_PATH/mappy-dataxy/CHANGELOG CHANGELOG
cp  $PROJECT_BASE_PATH/mappy-dataxy/README.md README.md
cp  $PROJECT_BASE_PATH/.gitignore .gitignore


git add .
git commit -m "New Version $version"
git push client main

# delete the temporary mirror directory
cd ../..
rm -Rf mirror

