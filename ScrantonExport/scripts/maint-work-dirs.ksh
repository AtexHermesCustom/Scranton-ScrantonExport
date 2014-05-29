#!/bin/ksh

DEBUGDIR_MERLIN=${HERMES}/h11export/debug/merlin
DEBUGDIR_OBIT=${HERMES}/h11export/debug/obit

# delete old files older than 5 days
find ${DEBUGDIR_MERLIN} -name "*.*" -mtime +5 -exec rm -f {} \;
find ${DEBUGDIR_OBIT} -name "*.*" -mtime +5 -exec rm -f {} \;

exit
