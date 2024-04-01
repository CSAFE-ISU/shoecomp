#!/bin/sh
set -eu

mkdir -p plugins macros
cd plugins

echo "copying shoecomp jar..."
cp ../target/shoecomp-1.0-SNAPSHOT-shaded.jar ./shoecomp-1.0.jar

# https://figshare.com/articles/dataset/Custom_toolbars_and_mini_applications_with_Action_Bar/3397603
echo "downloading action_bar.jar..."
JAR_URL="https://figshare.com/ndownloader/files/42230733"
wget -qO action_bar.jar $JAR_URL
