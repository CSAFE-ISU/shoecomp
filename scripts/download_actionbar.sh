#!/usr/bin/env bash
set -eu

# https://figshare.com/articles/dataset/Custom_toolbars_and_mini_applications_with_Action_Bar/3397603

echo "downloading action_bar.jar..."
JAR_URL="https://figshare.com/ndownloader/files/42230733"
wget -qO action_bar.jar $JAR_URL
