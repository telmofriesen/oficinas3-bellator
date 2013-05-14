#!/bin/bash

for x in 13*; do
	echo -n $x
	echo -n " - "
	name=`cat $x/descricao 2> /dev/null`
	if [ $? -eq 0 ]; then
		echo $name
	else
		echo ""
	fi
done
