#!/bin/bash

if type -p pandoc
then
   
   LIST_FILES=$(find . -name "*.md")
   
   for file in $LIST_FILES; do 
   
       NOEXT=$(echo "$file" | sed 's/.md$//')
       NAME=`basename "$NOEXT"` 
       
       if [ "${NOEXT}.md" -nt "${NOEXT}.html" ]; then
           echo '######################'
           echo "Processing file : $file "

           pandoc --standalone --metadata pagetitle="${NAME}" ${NOEXT}.md > ${NOEXT}.html
       fi
   done
else
    echo "pandoc command was not found. Did not parse any markdown documentation file."
fi

