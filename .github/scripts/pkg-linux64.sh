#!/bin/sh
set -eux

FIJI_FILENAME="fiji-linux64.zip"

wget https://downloads.imagej.net/fiji/latest/fiji-linux64.zip -qO "$FIJI_FILENAME"

zip --delete "$FIJI_FILENAME" 'Fiji.app/macros/*'
zip --delete "$FIJI_FILENAME" 'Fiji.app/plugins/*'
zip --delete "$FIJI_FILENAME" 'Fiji.app/scripts/*'
zip --delete "$FIJI_FILENAME" 'Fiji.app/images/about/*'

zip -r "$FIJI_FILENAME" Fiji.app
unzip -vl "$FIJI_FILENAME" | grep -i "shoecomp"
