#!/bin/sh
# ----------------------------------------------------------------------------
# Apache Maven Wrapper Startup Batch script, version 3.2.0
# ----------------------------------------------------------------------------

set -e

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false; darwin=false; mingw=false
case "`uname`" in
  CYGWIN*) cygwin=true ;;
  Darwin*) darwin=true ;;
  MINGW*) mingw=true ;;
esac

## Resolve links: $0 may be a link
PRG="$0"

# Need this for relative symlinks.
while [ -h "$PRG" ] ; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`"/$link"
  fi
done

saveddir=`pwd`

MAVEN_PROJECTBASEDIR=`dirname "$PRG"`
export MAVEN_PROJECTBASEDIR

# Need this for relative symlinks.
cd "$MAVEN_PROJECTBASEDIR"

exec mvn "$@"
