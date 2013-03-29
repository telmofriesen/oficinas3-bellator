#Script do make para compilar o texto feito no Latex.

TEX = "relatorio"

all: compilar
compilar: pdflatex
limpo: pdflatex limpar
 	
pdflatex: 
	pdflatex ${TEX}
#	bibtex ${TEX}
	pdflatex ${TEX}
	pdflatex ${TEX}

ver:
	nohup evince ${TEX}.pdf >/dev/null 2>/dev/null &
	
okular:
	nohup okular ${TEX}.pdf >/dev/null 2>/dev/null &
	
limpar:
	-rm *.log *.aux *.lof *.blg *.bbl *.toc *.dvi *.out *.lot *.lsb *.lsg *.lol *~ *.snm *.nav

limpartudo: limpar
	-rm  *.pdf
	
clean: limpar
cleanall: limpartudo
