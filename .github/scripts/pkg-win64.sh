#!/bin/sh
set -eux

FIJI_FILENAME="fiji-win64.zip"

wget https://downloads.imagej.net/fiji/latest/fiji-win64.zip -qO "$FIJI_FILENAME"

zip --delete "$FIJI_FILENAME" 'Fiji.app/macros/*'
zip --delete "$FIJI_FILENAME" 'Fiji.app/plugins/*'
zip --delete "$FIJI_FILENAME" 'Fiji.app/scripts/*'
zip --delete "$FIJI_FILENAME" 'Fiji.app/images/about/*'

zip -r macros "$FIJI_FILENAME"
zip -r plugins "$FIJI_FILENAME"
