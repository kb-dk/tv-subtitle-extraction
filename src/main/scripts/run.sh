#!/bin/bash


SCRIPT_DIR=$(dirname $(readlink -f $0))


java -classpath "$SCRIPT_DIR/../conf:$SCRIPT_DIR/../lib/*" \
 dk.statsbiblioteket.subtitleProject.SubtitleProject $@

