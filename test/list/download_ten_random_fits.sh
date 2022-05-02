LIST="list.txt"

if [ -f "$LIST" ]
then

   ATTEMPTS=0
   MAX_ATTEMPTS=100
   
   NB_SUCCESS=0
   MAX_NB_SUCCESS=10

   # array of 100 random number between 1 and number of lines in $LIST 
   NUMS=$(awk 'END{ srand(); for(i=1;i<=100;i++) printf("%d\n",int( 1 + rand() * NR))}' "$LIST")
	   
   while [ "$NB_SUCCESS" -lt "$MAX_NB_SUCCESS" ] && [ "$ATTEMPTS" -lt "$MAX_ATTEMPTS" ]
   do
	   ATTEMPTS=$(("$ATTEMPTS" + 1))
	   
	   NUM=$(echo "$NUMS" | sed "${ATTEMPTS}q;d")
	   URL=$(sed "${NUM}q;d" "$LIST")
	   NAME=$(echo "$URL" | sed 's#.*/##')
	   
	   echo "Info: attempt n°$ATTEMPTS: trying $URL"
	   
	   if [ -f "$NAME" ]
	   then
	      # fail
		   echo "Warning: file $NAME is already here."
      else
	   
	      echo "" > tmp_download_header
	      wget "$URL" -S --output-document=tmp_download_file 2> tmp_download_header
	      
	      FOUND=$(awk 'BEGIN {x=0} /200 OK/ {x=1} END {print x}' tmp_download_header)
	      
	      if [ "$FOUND" -eq 1 ]
	      then
	         # success
		      echo "Success: downloaded file $NAME"
		      NB_SUCCESS=$(("$NB_SUCCESS" + 1))
		      mv tmp_download_file "$NAME"
	      else
		      # fail
		      echo "Error: failed to download $URL correctly"
	      fi
	   fi
   done

   # excuse message 
   if [ "$NB_SUCCESS" -lt "$MAX_NB_SUCCESS" ]
   then
	   echo "Warning: sorry, we stop early because we already made $ATTEMPTS attempts."
   fi
	   
   # cleaning
   if [ -f tmp_download_file ]; then rm tmp_download_file; fi
   if [ -f tmp_download_header ]; then rm tmp_download_header; fi 

else 
   echo "Error: cannot find list.txt, please run download_list.sh to get it."
fi
