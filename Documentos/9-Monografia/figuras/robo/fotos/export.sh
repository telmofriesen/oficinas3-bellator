#!/bin/bash

for x in *.jpg *.png; do
	name=`echo "$x" | cut -f1 -d"."`
	echo "
\begin{figure}[H]
	\centering
	\includegraphics[width=1\textwidth]{./figuras/robo/fotos/$x}
	%\caption{$name.}
	\label{fig:robo_$name}
\end{figure}" #| sed 's/\\n/\n/'
done
