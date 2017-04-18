/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package leilaoversao2;

import leilaoversao2.Produto;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import static leilaoversao2.LeilaoVersao2.assinatura;
import static leilaoversao2.LeilaoVersao2.listaProcessosLeiloeros;

import static leilaoversao2.LeilaoVersao2.procesosInteresados;
import static leilaoversao2.LeilaoVersao2.processList;
import static leilaoversao2.LeilaoVersao2.verificaSeExisteProessoLeiloeiro;

/**
 * Classe utilizada para o recebimento das mensagens em Multicast
 *
 * @author a1562711
 */
public class ServidorMultiCast extends Thread {

    String leitura1 = " ";
    Scanner scan = new Scanner(System.in);
    MulticastSocket s;
    InetAddress group;
    Processo process = null;
    DatagramSocket socket = null;
    String ipMulticast = null;
    int portMulticast = 0;


    /**
     * construtor do servidor multicast
     *
     * @param process
     * @param ipMulticast
     * @param portMulticast
     * @throws SocketException
     * @throws UnknownHostException
     * @throws IOException
     */
    public ServidorMultiCast(Processo process, String ipMulticast, int portMulticast) throws SocketException, UnknownHostException, IOException {
        this.socket = new DatagramSocket();
        this.process = process;
        this.ipMulticast = ipMulticast;
        this.portMulticast = portMulticast;

        // ********************************************
        // Inserts this thread into the Multicast Group.
        group = InetAddress.getByName(ipMulticast);
        s = new MulticastSocket(portMulticast);
        s.joinGroup(group);

    }

    @Override
    public void run() {

        byte[] buffer;
        char type;
        DatagramPacket messageIn;
        byte[] msg;
        ByteArrayInputStream bis;
        ObjectInputStream ois;
        Chaves gera_chave = null;
        while (true) {
            try {
                buffer = new byte[1024];
                messageIn = new DatagramPacket(buffer, buffer.length);

                s.receive(messageIn);

                msg = messageIn.getData();
                bis = new ByteArrayInputStream(msg);

                ois = new ObjectInputStream(bis);
                type = ois.readChar();
                System.out.println(type);
                System.out.println("aqqui");
                switch (type) {

                    case 'N':
                        String pid = ois.readUTF();

                        if (pid.equals(process.getId())) {
                            break;
                        } else {

                            // *********************************************
                            // Desempacotando o resto da mensagem
                            String port = ois.readUTF();
                            PublicKey chavePublica = (PublicKey) ois.readObject();
                            List<Produto> listaProduto = (ArrayList<Produto>) ois.readObject();
                            List<Produto> listaProdutosleiloando = (ArrayList<Produto>) ois.readObject();

                            // *********************************************
                            // Adicionado lista de Produtos a minha lista de Produtos local
                            // adicionaListaDeProdutos(pid, listaProduto);
                            // *********************************************
                            // Criando um novo processo e adicionando na lista de processos
                            Processo novoProcesso = new Processo(pid, port, chavePublica, listaProduto,
                                    listaProdutosleiloando);
                            processList.add(novoProcesso);
                            List<Produto> produtos = retornaListadeProdutosdeProcesso(process.getId());
                            // *********************************************
                            // Enviando para o novo processo essas informações
                            // Packing the message.

                            // *********************************************
                            // Gerando Hash Map Encripta
                            // Packing the message.
                            Autenticacao auto = new Autenticacao();
                            auto.setPublic_chave(chavePublica);
                            gera_chave = new Chaves();
                            assinatura.put(pid, auto);

                            ByteArrayOutputStream bos = new ByteArrayOutputStream(10);
                            ObjectOutputStream oos = new ObjectOutputStream(bos);
                            oos.writeChar('N');
                            oos.writeUTF(process.getId());
                            oos.writeUTF(process.getPort());
                            oos.writeObject(process.getChavePublica());
                            oos.writeObject(process.getListaProduto());
                            oos.writeObject(process.getListaProdutosLeiloando());

                            oos.flush();

                            byte[] output = bos.toByteArray();
                            DatagramPacket messageOut = new DatagramPacket(output, output.length, messageIn.getAddress(), Integer.parseInt(port));
                            System.out.println("");
                            System.out.print("[MULTICAST - recebe]");
                            System.out.print(" ID do participante: " + pid);
                            System.out.print(", Porta: " + port);
                            System.out.print(", Chave publica: - ");
                            for (Produto p : listaProduto) {
                                System.out.println("Informações sobre o " + p.getName() + " produto da lista do processo " + pid);
                                System.out.println(", ID Produto " + p.getName() + ": " + p.getId());
                                System.out.println(", Descrição Produto " + p.getName() + ": " + p.getDescricao());
                                System.out.println(", Preço Produto " + p.getName() + ": " + p.getPrecoInicial());
                            }

                            System.out.println("");
                            System.out.print("[UNICAST - envia] tipo N");
                            System.out.print(" ID do participante: " + process.getId());
                            System.out.print(", Porta: " + process.getPort());
                            System.out.print(", Chave publica: - ");
                            for (Produto p : retornaListadeProdutosdeProcesso(process.getId())) {
                                System.out.println("Informações sobre o " + p.getName() + " produto da lista do processo " + pid);
                                System.out.println(", ID Produto " + p.getName() + ": " + p.getId());
                                System.out.println(", Descrição Produto " + p.getName() + ": " + p.getDescricao());
                                System.out.println(", Preço Produto " + p.getName() + ": " + p.getPrecoInicial());
                            }
                            socket.send(messageOut);

                            break;
                        }

                    case 'A':
                        // *********************************************

                        String id = ois.readUTF();
                        if (!process.getId().equals(id)) {
                            // Debug recebi  atualizaço de prooduto
                            System.out.print("[MULTICAST - Recebe]");
                            System.out.print("Atualizacao de produto leiloeiro: " + id);

                        }
                        String idProduto = ois.readUTF();
                        String novoValor = ois.readUTF();
                        atualizaValorProduto(id, idProduto, novoValor);
                        break;

                    case 'R':
                        // *********************************************
                        // Recebe o ID do processo vencedor e do produto leiloado
                        String leiloeiroID = ois.readUTF();
                        String vencedorID = ois.readUTF();
                        String produtoID = ois.readUTF();
                        // *********************************************
                        // Atualizando Novo Proprietario                        
                        System.out.print("[MULTICAST - Envia]");
                        System.out.print(" ID do participante: " + vencedorID);
                        atualizaProprientario(leiloeiroID, vencedorID, produtoID);

                        atualizaListadeLeiloeiros(leiloeiroID);
                        break;

                    case 'S':
                        System.out.println("recebi");
                        String p = ois.readUTF();
                        String port = ois.readUTF();
                        List<Produto> novaListaProduto = (ArrayList<Produto>) ois.readObject();
                        List<Produto> novaListaProdutoLeiloando = (ArrayList<Produto>) ois.readObject();

                        System.out.println("novaListaProduto.size()" + novaListaProduto.size());
                        System.out.println("novaListaProdutoLeiloando.size()" + novaListaProdutoLeiloando.size());
                        //**************************************************
                        //Atualiza lista de produto e produto leiloando desse processo
                        //Além disso atualiza a lista de processos leiloeiros para
                        //o processo corrente
                        for (Produto prod : novaListaProdutoLeiloando) {
                            System.out.println("PRoduto: " + prod.getName());

                        }

                        for (Processo pro : processList) {
                            if (pro.getId().equals(p) && (!pro.getId().equals(process.getId()))) {
                                pro.setListaProduto(novaListaProduto);
                                System.out.println("Tamanho lista proc nova produto: " + pro.getListaProduto().size());
                                pro.setListaProdutosLeiloando(novaListaProdutoLeiloando);

                                System.out.println("Tamanho lista proc nova produto leiloando: " + pro.getListaProdutosLeiloando().size());

                            }
                            if (pro.getId().equals(p)) {

                                if (!verificaSeExisteProessoLeiloeiro(pro.getId())) {
                                    LeilaoVersao2.listaProcessosLeiloeros.add(pro);
                                }
                            }
                        }

                        break;

                    case 'E':
                        String idProSaindo = ois.readUTF();
                        String portProSaindo = ois.readUTF();

                        LeilaoVersao2.processList.remove(idProSaindo);
                        LeilaoVersao2.listaProcessosLeiloeros.remove(idProSaindo);

                        break;

                }

            } catch (IOException ex) {
                Logger.getLogger(ServidorMultiCast.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(ServidorMultiCast.class.getName()).log(Level.SEVERE, null, ex);

            }

        }
    }

    /**
     * Método que retorna um processo a partir do seu ID
     *
     * @param id
     * @return
     */
    public Processo ProcuraProcesso(String id) {
        Processo processo = null;
        for (Processo p : processList) {
            if (p.getId().equals(id)) {
                processo = p;
                break;
            }
        }
        return processo;

    }

    /**
     * Método que atualiza o valor do produto
     *
     * @param idProduto
     * @param novoValor
     */
    public void atualizaValorProduto(String idProcesso, String idProduto, String novoValor) {

        for (Processo proc : processList) {
            if (proc.getId().equals(idProcesso)) {
                for (Produto p : proc.getListaProduto()) {
                    if (p.getId().equals(idProduto)) {
                        p.setPrecoInicial(novoValor);
                    }

                }
            }
        }
    }

    /**
     * Método que atualiza o proprietário do produto
     *
     * @param idProcesso
     * @param idProduto
     */
    public static void atualizaProprientario(String leiloeiroID, String vencedorID, String idProduto) {

        Produto product = null;

        for (Processo pL : processList) {
            if (pL.getId().equals(leiloeiroID)) {
                for (Produto prodL : pL.getListaProdutosLeiloando()) {
                    if (prodL.getId().equals(idProduto)) {
                        product = prodL;
                        pL.getListaProdutosLeiloando().remove(prodL);

                        break;
                    }
                }

            }
        }

        for (Processo pL : processList) {
            if (pL.getId().equals(vencedorID)) {
                pL.getListaProduto().add(product);
                break;
            }

        }
    }

    public void atualizaListadeLeiloeiros(String LeiloeiroID) {
        for (Processo p : listaProcessosLeiloeros) {
            if (p.getId().equals(LeiloeiroID)) {
                if (p.getListaProdutosLeiloando().size() == 0) {
                    listaProcessosLeiloeros.remove(p);
                    break;
                }

            }

        }

    }

    /**
     * Método que adiciona lista de produtos a lista de produtos do novo
     * processo
     *
     * @param id
     * @param listaProduto
     */
    public void adicionaListaDeProdutos(String id, List<Produto> listaProduto) {
        for (Processo proc : processList) {
            if (proc.getId().equals(id)) {

                proc.setListaProduto(listaProduto);
                for (Produto prod : proc.getListaProduto()) {
                    Controle controle = new Controle(prod.getId(), prod.getPrecoInicial());
                    procesosInteresados.add(controle);
                }

            }
        }
    }

    /**
     * Método ue retorna a lista de produtos do processo
     *
     * @param idProcesso
     * @return produtos
     */
    public List<Produto> retornaListadeProdutosdeProcesso(String idProcesso) {

        List<Produto> produtos = new ArrayList<>();

        for (Processo proc : processList) {
            if (proc.getId().equals(idProcesso)) {
                return proc.getListaProduto();
            }
        }

        return null;
    }

}
