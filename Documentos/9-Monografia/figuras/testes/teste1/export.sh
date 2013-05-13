#!/bin/bash

for x in *.jpg *.png; do
	name=`echo "$x" | cut -f1 -d"."`
	echo "
\begin{figure}[H]
	\centering
	\includegraphics[width=1\textwidth]{./images/$x}
	\caption{Gráfico da questão $name.}
	\label{fig:$name}
\end{figure}" #| sed 's/\\n/\n/'
done
