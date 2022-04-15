URL="http://oidb.jmmc.fr/modules/curl.xql?perpage=25&public=yes&descending=yes&cs_radius_unit=arcmin&cs_equinox=J2000&caliblevel=2%2C3&cs_radius=2"

wget "$URL" --output-document=list.txt 2>/dev/null \
&& awk '/^url = / {print substr($0, 7)}' < list.txt > list.txt.tmp \
&& mv list.txt.tmp list.txt
