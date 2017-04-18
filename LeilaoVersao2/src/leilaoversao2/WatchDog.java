/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package leilaoversao2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.sql.Timestamp;
import java.util.logging.Level;
import java.util.logging.Logger;
import static leilaoversao2.LeilaoVersao2.listaProcessosLeiloeros;
import static leilaoversao2.LeilaoVersao2.processList;
import static leilaoversao2.LeilaoVersao2.setStop;
import static leilaoversao2.ServidorUniCast.getVivo;
import static leilaoversao2.ServidorUniCast.vivos;

/**
 *
 * @author allan
 */
public class WatchDog extends Thread {

    DatagramSocket socket = null;
    InetAddress localHost = null;
    String myport = null;
    String myid;

    WatchDog(DatagramSocket socket, InetAddress localHost, String myport, String myid) {
        this.socket = socket;
        this.localHost = localHost;
        this.myport = myport;
        this.myid = myid;
    }

    public void run() {

        byte[] buffer;
        char type; // type of message
        DatagramPacket messageIn;
        ByteArrayInputStream bis;
        ObjectInputStream ois;

        while (true) {

            try {
                Thread.sleep(1000);

                if (processList.size() > 1) {
                    for (Processo p : processList) {
                        if (!p.getId().equals(myid)) {
                            // *********************************************
                            // Empacado mensagem vivo
                            ByteArrayOutputStream bos1 = new ByteArrayOutputStream(10);
                            ObjectOutputStream oos1 = new ObjectOutputStream(bos1);
                            oos1.writeChar('W');
                            oos1.writeUTF(myid);
                            oos1.writeUTF(myport);
                            oos1.flush();
                            byte[] output = bos1.toByteArray();
                            // *********************************************
                            // Enviando unicast
                            DatagramPacket request = new DatagramPacket(output, output.length, localHost, Integer.parseInt(p.getPort()));
                            socket.send(request);
                            vivos.put(p.getId(), "false");
                            Thread.sleep(700);
                            if (getVivo() != null) {
                                if (!vivos.get(p.getId()).equals("true")) {
                                    for (int k = 0; k < 2; k++) {
                                        socket.send(request);
                                        Thread.sleep(200);
                                    }
                                    if (!vivos.get(p.getId()).equals("true")) {
                                        System.out.println("-----SAIU" + p.getId());
                                        for (Processo leiloaroes : listaProcessosLeiloeros) {
                                            if (leiloaroes.getId().equals(p)) {
                                                if (p.isAtivo()) {
                                                    setStop(true);
                                                }
                                                listaProcessosLeiloeros.remove(p);
                                                break;
                                            }
                                        }
                                        processList.remove(p);
                                        // *********************************************
                                        // colocar remover da lista de leiloadores
                                        break;

                                    }

                                }
                            }
                        }
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(WatchDog.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InterruptedException ex) {
                Logger.getLogger(WatchDog.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

    }

}
