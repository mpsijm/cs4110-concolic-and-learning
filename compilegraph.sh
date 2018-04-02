#!/usr/bin/env bash

regex="^s([0-9]+) -> s\1 \[label=\"[@A-Z] / ERR:([0-9]+)\"\];"
duplicate="INVALID"
grep -v "/ -" < $1 | sed 's/\[\]//' | while read line
do
  if [[ ${line} =~ ${regex} ]]
  then
    state="${BASH_REMATCH[1]}"
    err="${BASH_REMATCH[2]}"
    oldstate=state
    olderr=err
  else
    echo ${line}
  fi
done | dot -Tpdf -o ${1%.dot}.pdf
