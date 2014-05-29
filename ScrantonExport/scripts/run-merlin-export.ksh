#!/bin/ksh
#############################################################################
#
# Starter script for Merlin Export
#
#############################################################################

INSTALLDIR=`dirname $0`
LIBDIR=$INSTALLDIR/lib
CONFDIR=$INSTALLDIR/conf

test -z "$INSTALLDIR" && INSTALLDIR=.

# Defaults
PUB="SC_TIMES_TRIB"
PUBDATE="`date +%Y%m%d`"

# Input arguments
while getopts f:l:d:e:r:t argswitch
do
	case $argswitch in
		f)	INPUTFILE=$OPTARG;;
		l)	PUB=$OPTARG;;
		d)	PUBDATE=$OPTARG;;
		e)	EDITION=$OPTARG;;
		r)	PAGERANGE=$OPTARG;;
		t)	TESTFLAG=1;;
		\?)	printf "Usage: %s: [-f inputFile | [-l publication -d pubDate [-e edition] [-r pageFrom:pageTo]]]\n" `basename $0`
			exit 2;;
	esac
done

# Export arguments
if [ ! -z "$INPUTFILE" ]; then
	XARGS="-f $INPUTFILE"
elif [ ! -z "$PUB" -a ! -z "$PUBDATE" ]; then
	XARGS="-l $PUB -d $PUBDATE -c $BATCH_USR:$BATCH_PWD"
	if [ ! -z "$EDITION" ]; then
		XARGS="$XARGS -e $EDITION"
	fi
	if [ ! -z "$PAGERANGE" ]; then
		XARGS="$XARGS -r $PAGERANGE"
	fi	
else
	printf "Usage: %s: [-f inputFile | [-l publication -p pubDate [-e edition] [-r pageFrom:pageTo]]]\n" `basename $0`
	exit 2
fi

# set config files
PROPS=merlin-export.properties
if [ "$TESTFLAG" = "1" ]; then
    PROPS=merlin-export-test.properties
fi
LOGPROPS=merlin-log.properties

# set class path
CLASSPATH=$INSTALLDIR
for j in `find $HERMES/classes -type f -name '*'.jar -print`
do
	CLASSPATH="$CLASSPATH:$j"
done
for j in `find $LIBDIR -type f -name '*'.jar -print`
do
	CLASSPATH="$CLASSPATH:$j"
done
export CLASSPATH

# initiate export
COMMAND="$JAVA_HOME/bin/java 
	-Djava.security.policy=$CONFDIR/app.policy -Djava.security.manager -Djava.security.auth.login.config=$CONFDIR/auth.conf 
	-Djndi.properties=$CONFDIR/jndi.properties -Djavax.xml.transform.TransformerFactory=net.sf.saxon.TransformerFactoryImpl
	-Djava.util.logging.config.file=$CONFDIR/$LOGPROPS
	com.atex.h11.custom.scranton.export.common.Main -p $CONFDIR/$PROPS $XARGS"
echo $COMMAND
exec $COMMAND
