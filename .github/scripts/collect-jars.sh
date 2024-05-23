#!/bin/sh
set -eu

mkdir -p Fiji.app
cd Fiji.app

# copy macros
cp -r ../macros ./

# copy plugins
mkdir -p plugins
cd plugins

echo "copying shoecomp jar..."
cp ../../target/shoecomp-1.0-SNAPSHOT-shaded.jar ./shoecomp-1.0.jar

# https://figshare.com/articles/dataset/Custom_toolbars_and_mini_applications_with_Action_Bar/3397603
echo "downloading action_bar.jar..."
JAR_URL="https://figshare.com/ndownloader/files/42230733"
# ActionBar needs this specific folder location in order to load icons
mkdir -p ActionBar/icons
cd ActionBar/icons
wget -qO action_bar.jar $JAR_URL
