#!/bin/bash

#echo "Digite a senha para entrar como root:"

#[ `whoami` != "root" ] && { sudo su; }
#[ `whoami` != "root" ] && { exit 1; }

echo '## sudo service network-manager stop'
sudo service network-manager stop
echo '## sudo service nfs-kernel-server start'
sudo service nfs-kernel-server start
echo '## sudo service apache2 start'
sudo service apache2 start
echo '## sudo ifconfig eth0 192.168.10.36 netmask 255.255.255.0 up'
sudo ifconfig eth0 192.168.10.36 netmask 255.255.255.0 up
sleep 1

echo "Digite os seguintes comandos dentro do minicom:"
echo '-------'
echo 'load -v -r -b 0x00218000 -m http -h 192.168.10.36 /zImage'
echo 'exec -c "console=ttyAM0,115200 ip=192.168.10.50:192.168.10.36:192.168.10.1:255.255.255.0::: nfsroot=192.168.10.36:/ts init=/linuxrc root=/dev/nfs rw"'
echo '-------'

sudo minicom -D /dev/ttyUSB0 #-S minicom_script.txt

echo
read -p "Pressione ENTER para iniciar o network-manager..."
echo

echo '## sudo service network-manager start'
sudo service network-manager start


