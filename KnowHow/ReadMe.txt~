Repositório também disponivel em:
https://github.com/telmofriesen/oficinas3-bellator

----------------------------------------------------------------------------------
Estrutura de arquivos:

Ambiente de Desenvolvimento ARM
- Gravador-GUI-flpcui: gravador para o lpc2103
- Gravador-Terminal-lpc21isp_185: gravador para o lpc2103, sem interface grafica.
- Compilador: arm-2011.03-41-arm-none-linux-gnueabi.bin: Compilador usado
Documentos
- Documentos entregues na disciplina
KnowHow
- Datasheets: Datasheets de todos os componentes de hardware e User Manual do lpc210x;
- Old: Dados de trabalhos anteriores; 
- getting-started-arm-2009q-203.pdf: conhecimentos gerais sobre o compilador;
- GitHub: comandos básicos para uso do GitHub;
- ReadMe: este arquivo.
Placa: Arquivos relativos ao projeto da placa;
Software: código fonte dos softwares desenvolvidos.

----------------------------------------------------------------------------------
Ambiente de desenvolvimento para baixo nivel:

- instalar eclipse-cdt (pelo synaptic)
- instalar compilador:
	baixar aqui:
		https://sourcery.mentor.com/GNUToolchain/release1803?lite=arm
		arm-2011.03-41-arm-none-linux-gnueabi.bin
	chmod +x ./arm-2011.03-41-arm-none-linux-gnueabi.bin
	sudo dpkg-reconfigure -plow dash -> Install as /bin/sh? No
	sudo ./arm-2011.03-41-arm-none-linux-gnueabi.bin
	- mudar a instalação padrão e instalar somente o toolchain, sem documentação
	- fazer links simbolicos para /usr/bin do compilador:
	for i in `ls /opt/arm-none-linux/bin/arm-none-linux-gnueabi-*`; do name=`echo $i|awk -F"-" {'print $NF'}`; sudo ln -s $i /usr/bin/arm-none-linux-gnueabi-$name; done
- abrir o projeto (Software/Embarcado/Baixo Nivel/bellator_low_level) no eclipse e compilar.	

Gravador ISP:

- rodar: /Ambiente de Desenvolvimento ARM/Gravador-GUI-flpcui/bin/Release/flpcui
	- Configurar o clock da placa (14745 kHz);
	- Selecionar o arquivo .hex gerado pelo eclipse após a compilação do projeto;
	- Conectar placa na serial (ou dongle) e selecionar porta;
	- Clicar em Program.

----------------------------------------------------------------------------------
Ambiente para desenvolvimento do hardware

- instalar gEDA (gschem) e pcb pelo synaptic;
- copiar a biblioteca de comoponentes (/Placa/Projeto-Geda/symbols) para /usr/share/gEDA/sym e dar permições de no mínimo leitura para o usuário;
- abrir o esquematico (/Placa/Projeto-Geda/bellator2.2.sch) usando o gEDA;
- abrir o roteamento usando o PCB (/Placa/Projeto-Geda/bellator2.1.pcb)

Para gerar pdf a partir do gEDA:
- imprimir para arquivo.ps
- ps2pdf -sPAPERSIZE=a1 ./bellator2.1.ps
