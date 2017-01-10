#!/bin/bash
trap ctrl_c INT

function ctrl_c() {
	bash ./stop.sh; exit
}

function open_browser() {
	#tries to send request to launching server. If doesn't succeed, waits 10s and tries again
	while true 
	do	
		RESPONSE=$(curl --write-out %{http_code} --silent --output /dev/null 'http://localhost:8080/catgenome')
		if [ $RESPONSE == "200" -o $RESPONSE == "302" ]; then
			xdg-open 'http://localhost:8080/catgenome' &
			break
		else
			sleep 1s
		fi
	done
}

export -f open_browser
bash -c open_browser &
bash ./start.sh
