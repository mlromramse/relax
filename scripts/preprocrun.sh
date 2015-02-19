#!/bin/bash

if [ $# -lt 1 ];
then
	echo "usage: `basename $0` pid"
	exit 0
fi

if [ ! -d "/proc/$1" ];
then
	echo "Given process id is not active at the moment."
	exit 0
fi

uptime1=`cat /proc/uptime |cut -d " " -f1|sed 's/\.//'`
proctime1=`cat /proc/$1/stat|awk '{t=$14+$15;print t}'`

echo "$uptime1 $proctime1" > preprocrun.log

sleep 1
