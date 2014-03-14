#!/bin/bash


SCRIPT_DIR=$(dirname $(readlink -f $0))


java -classpath "$SCRIPT_DIR/../conf:$SCRIPT_DIR/../var:$SCRIPT_DIR/../lib/*" \
 test1.SubtitleProject $@

