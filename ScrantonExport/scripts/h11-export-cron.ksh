#!/bin/ksh

. ~/.profile

# Export obits to Legacy
$HERMES/h11export/run-obit-export.ksh -l PR_REPUBLICAN
$HERMES/h11export/run-obit-export.ksh -l SH_NEWSITEM
$HERMES/h11export/run-obit-export.ksh -l SC_TIMES_TRIB
$HERMES/h11export/run-obit-export.ksh -l WB_VOICE
$HERMES/h11export/run-obit-export.ksh -l TW_REVIEW
$HERMES/h11export/run-obit-export.ksh -l WC_WYOMING_CTY
$HERMES/h11export/run-obit-export.ksh -l HZ_STANDSPEAK
$HERMES/h11export/run-obit-export.ksh -l VI_DAILYNEWS
$HERMES/h11export/run-obit-export.ksh -l TSWG -e NEW_AGE
$HERMES/h11export/run-obit-export.ksh -l TSWG -e WYOMING_CITY_ADV


# Export to Merlin
$HERMES/h11export/run-merlin-export.ksh -l PR_REPUBLICAN
$HERMES/h11export/run-merlin-export.ksh -l SH_NEWSITEM
$HERMES/h11export/run-merlin-export.ksh -l SC_TIMES_TRIB
$HERMES/h11export/run-merlin-export.ksh -l WB_VOICE
$HERMES/h11export/run-merlin-export.ksh -l TW_REVIEW
$HERMES/h11export/run-merlin-export.ksh -l WC_WYOMING_CTY
$HERMES/h11export/run-merlin-export.ksh -l HZ_STANDSPEAK
$HERMES/h11export/run-merlin-export.ksh -l VI_DAILYNEWS
$HERMES/h11export/run-merlin-export.ksh -l TSWG -e WGNA
$HERMES/h11export/run-merlin-export.ksh -l TSWG -e WYOMING_CITY_ADV
