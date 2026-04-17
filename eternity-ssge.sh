#!/bin/sh

# This isn't a great script, and doesn't follow best practice for finding
# the Java home path.  It's "good enough", though.

JAVA=java
if ! which "${JAVA}" >/dev/null 2>&1 ; then
  JAVA="${JAVA_HOME}/bin/java"
  if [ ! -f "${JAVA}" ] ; then
    >&2 echo "Could not find the Java installation.  Set 'JAVA_HOME' to the directory of it."
    exit 1
  fi
fi

jar=eternity-ssge-1.0-all.jar
if [ -f $(dirname "$0")/build/libs/${jar} ] ; then
  jar=$(dirname "$0")/build/libs/${jar}
fi
if [ ! -f "${jar}" ] ; then
  >&2 echo "Could not find jar file ${jar}"
  exit 1
fi

exec "${JAVA}" -jar "${jar}" "$@"
