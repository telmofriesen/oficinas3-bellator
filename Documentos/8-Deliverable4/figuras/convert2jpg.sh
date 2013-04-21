
mkdir png; for x in *.png;do; convert -quality 70 $x `echo $x | cut -f1 -d"."`.jpg; mv $x png;done

