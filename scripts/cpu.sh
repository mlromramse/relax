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

uptime1=`cat preprocrun.log |cut -d " " -f1`
proctime1=`cat preprocrun.log |cut -d " " -f2`

uptime2=`cat /proc/uptime |cut -d " " -f1|sed 's/\.//'`
proctime2=`cat /proc/$1/stat|awk '{t=$14+$15;print t}'`

uptime=$((uptime2-uptime1))
proctime=$((proctime2-proctime1))

memtot=`cat /proc/meminfo|head -n1|tr -s " "|cut -d " " -f2`
pagesize=`getconf PAGESIZE`
mempages=`cat /proc/$1/statm|cut -d " " -f2`
memproc=$((mempages*pagesize/1024))
pmem=`echo "$memproc $memtot"|awk '{printf "%.1f", $1*100/$2}'`

echo "{ \"cpu%\": $((proctime*100/uptime)), \"memtot\": $memtot, \"mem\": $memproc, \"mem%\": $pmem, \"proctime\": $proctime, , \"uptime\": $uptime}"