# Grupo

Sérgio Neto **201407729**
Ruben Ramalho **201407438**

# Ficheiros

## Peer.java
- Compilar:
>javac Peer.java
- Executar:
>java Peer host port
>- Exemplo:
>java Peer localhost 2222

Exemplo para 4 peers (p1,p2,p3,p4):
> p1:
> java Peer p1.host 2222
> >p2.host 2222

>p2:
>java Peer p2.host 2222
> >p3.host 2222

>p3:
>java Peer p3.host 2222
>>p4.host 2222

>p4:
>java Peer p4.host 2222
>>p1.host 2222

lock ou unlock para parar ou libertar o **token**.

## Pushpull.java
- Compilar:
>javac Pushpull.java
- Executar:
>java Pushpull host port
>- Exemplo:
>java Pushpull localhost 2222

Exemplo para 3 peers (p1,p2,p3):

- Register(ip/host) - Cria ligação **bidirecional** entre peers.
> p1:
> java Pushpull p1.host 2222
> >register p2.host 2222
> >register p3.host 2222
> >push p3.host

>p2:
>java Pushpull p2.host 2222
> >pull p1.host

>p3:
>java Pushpull p3.host 2222
>>pushpull p1.host


## LamperPeer.java
- Compilar:
>javac LamperPeer.java
- Executar:
>java LamperPeer host port
>- Exemplo:
>java LamperPeer localhost 2222

Exemplo para 4 peers (p1,p2,p3,p4):

- Register(ip/host) - Cria ligação **bidirecional** entre peers.

> p1:
> java LamperPeer p1.host 2222
>>register 192.168.0.150 2222
>>register 192.168.0.143 2222
>>register 192.168.0.141 2222


>p2:
>java LamperPeer p2.host 2222
>>register 192.168.0.143 2222
>>register 192.168.0.141 2222


>p3:
>java LamperPeer p3.host 2222
>>register 192.168.0.141 2222


>p4:
>java LamperPeer p4.host 2222

- Depois de crirar as ligações qualquer frase escrita em shell vai ser enviada para os **Peers** que tem na sua tabela de ip. 





Link from dictionary

http://200.17.137.109:8081/novobsi/Members/cicerog/disciplinas/introducao-a-programacao/arquivos-2016-1/algoritmos/Lista-de-Palavras.txt/view


