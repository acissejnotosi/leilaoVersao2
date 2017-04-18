/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package leilaoversao2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.management.ManagementFactory;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

/**
 *
 * @author Jessica
 */
public class LeilaoVersao2 {

    static ArrayList<Processo> processList = new ArrayList<>();
    static List<Controle> procesosInteresados = new ArrayList<>();
    static List<String> produtosLancados = new ArrayList<>();
    static List<Processo> listaProcessosLeiloeros = new ArrayList<>();
    static Map<String, Autenticacao> assinatura = new HashMap<String, Autenticacao>();
    static PublicKey mychavePublica = null;
    static PrivateKey myChavePrivada = null;
    static boolean lance = false;
    static char tipo;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws UnknownHostException, IOException {

        //******************************************
        // Declarações e inicializações
        int PORT_MULTICAST = 6789;
        String IP_MULTICAST = "228.5.6.7";
        MulticastSocket s = null;
        DatagramSocket socket = null;
        Processo process = null;
        Produto product = null;
        Chaves gera_chave = null;
        PrivateKey chave_privada = null;
        String nomeProcesso = " ";
        String port = " ";
        String nomeProduto = " ";
        String idProduto = " ";
        String descProduto = " ";
        String precoProduto = " ";
        String tempoLeilao = " ";
        Scanner in = new Scanner(System.in);
        gera_chave = new Chaves();
        List<Produto> listaProdutos = new ArrayList<>();
        List<Produto> listaProdutosLeiloando = new ArrayList<>();
        
        //********************************************
        // Insere o processo no grupo Multicast
        InetAddress group = InetAddress.getByName(IP_MULTICAST);
        s = new MulticastSocket(PORT_MULTICAST);
        s.joinGroup(group);
        socket = new DatagramSocket();

        //**********************************************
        //Informa o nome e o port do processo
        System.out.println("Informe o NOME do participante:");
        nomeProcesso = in.nextLine();
        clearConsole();

        System.out.println("Informe a PORTA para comunicação UNICAST:");
        port = in.nextLine();
        clearConsole();

        // ********************************************
        // Gera as chaves pra este processo.
        gera_chave.geraChave();
        myChavePrivada = gera_chave.getChavePrivada();                          //Chave Privada
        mychavePublica = gera_chave.getChavePublica();                          //Chave Pública

        //*********************************************
        //Cria um novo processo.
        process = new Processo(nomeProcesso, port, mychavePublica, listaProdutos, listaProdutosLeiloando);

        //*********************************************
        //Adiciona o processo a lista de processos.
        LeilaoVersao2.processList.add(process);

        //  Controle controle = new Controle(idProduto, precoProduto);
        //procesosInteresados.add(controle);
        ByteArrayOutputStream bos = new ByteArrayOutputStream(40);
        ObjectOutputStream oos = new ObjectOutputStream(bos);

        //*********************************************
        //Tipo N refere-se a dados enviados sobre este processo.
        oos.writeChar('N');
        oos.writeUTF(nomeProcesso);
        oos.writeUTF(port);
        oos.writeObject(mychavePublica);
        oos.writeObject(process.getListaProduto());
        oos.writeObject(process.getListaProdutosLeiloando());
        oos.flush();

        // *********************************************
        // Inicialização da comunicação Multicast
        ServidorMultiCast multCastComm = new ServidorMultiCast(process, IP_MULTICAST, PORT_MULTICAST);
        multCastComm.start();

      
        //**********************************************
        //Inicialização da comunicação Unicast
        ServidorUniCast uniCastComm = new ServidorUniCast(process, IP_MULTICAST, PORT_MULTICAST);
        uniCastComm.start();

        // *********************************************
        // Enviando através do multicast as informações sobre o processo
        byte[] m = bos.toByteArray();
        DatagramPacket messageOut = new DatagramPacket(m, m.length, group, PORT_MULTICAST);

        //***********************************************
        //Verificação se a mensagem está sendo enviada.
        System.out.println("\n[MULTICAST enviando] Enviando a informação sobre este novo processo tipo N:");
        System.out.print(" ID do participante: " + nomeProcesso);
        System.out.print(", Porta: " + port);
        System.out.println(", Chave publica: - ");
        s.send(messageOut);

        // *********************************************
        // Fase de interação do processo sobre o sistema.
        while (true) {
            String cmd;

            System.out.println("MENU");
            System.out.println("Pressione a tecla desejada:");
            System.out.println("[1] Dar um lance em um produto ");
            System.out.println("[2] Lista os processos ");
            System.out.println("[3] Cadastrar um novo produto na sua lista de produtos");
            System.out.println("[4] Leiloar um produto ");
            System.out.println("[5] Lista dos leilões que estão ocorrendo ");
            System.out.println("[6] Listar seus produtos");
            System.out.println("[7] Sair");
            cmd = in.nextLine().trim().toUpperCase();
            System.out.println("");

            switch (cmd) {

                case "1": ///B

                    String nomeProRecebeLance;
                    String nomeProdRecebeLance;
                    String lance;
                    Processo paux;                           //processo que estou dando lance
                    Produto produtoaux;                     //produto que eu quero dar um lance
                    byte[] encryptedText;

                    // *******************************************
                    //Verifica quantidade de processos ativos
                    if (processList.size() < 2) {
                        System.out.println("É necessário pelo menos 3 processos ativos para continuar!");
                        break;
                    }

                    if (listaProcessosLeiloeros.isEmpty()) {
                        System.out.println("Não há leiloes disponíveis no momento!");
                    }
                    System.out.println("NOME DOS PROCESSOS LEILOEIROS:");
                    nomeProcessosLeiloeiros();

                    System.out.println("Digite o NOME de um dos processos leiloeiros para dar lance em 1 produto:");
                    nomeProRecebeLance = in.nextLine();

                    if (verificaSeExisteProessoLeiloeiro(nomeProRecebeLance)) {
                        System.out.println("Processo para dar lance selecionado com sucesso!");
                        paux = verificaProcessoNaLista(nomeProRecebeLance);
                        System.out.println("Agora selecione o PRODUTO desejado:");
                        mostraProdutosLeiloandoDesseProcesso(nomeProRecebeLance);
                        nomeProdRecebeLance = in.nextLine();
                        if (verificaProdutoListaLeiloando(nomeProRecebeLance, nomeProdRecebeLance)) {

                            produtoaux = verificaoProdutoNaLista(nomeProdRecebeLance, paux);

                            System.out.println("O valor atual do produto é:" + produtoaux.getPrecoInicial());
                            System.out.println("Seu lance precisa ser maior que o valor atual do produto!");
                            System.out.println("Digite o valor de lance desejado:");
                            lance = in.nextLine();
                            while (Integer.parseInt(lance) < Integer.parseInt(produtoaux.getPrecoInicial())) {
                                System.out.println("Seu lance é inferior ao valor atual do produto!Tente Novamente.");
                            }

                            System.out.println("Seu lance foi registrado!");

                            // *********************************************
                            // encriptografando meu nome com minha privda
                            encryptedText = gera_chave.criptografa("kkkk", myChavePrivada);

                            // *********************************************
                            //empacotando mensagem apra mandar em unicast
                            ByteArrayOutputStream bos1 = new ByteArrayOutputStream(10);
                            ObjectOutputStream oos1 = new ObjectOutputStream(bos1);
                            oos1.writeChar('B');
                            oos1.writeUTF(process.getId());
                            oos1.writeUTF(process.getPort());
                            oos1.writeUTF(lance);
                            oos1.writeUTF(produtoaux.getId());
                            oos1.write(encryptedText.length);
                            oos1.write(encryptedText);
                            oos1.writeUTF(produtoaux.getTempoFinal());
                            oos1.flush();

                            //*****************************************************
                            // Enviando a mensagem Unicast para o vendedor
                            byte[] output = bos1.toByteArray();
                            DatagramPacket messageOut1 = new DatagramPacket(output, output.length, InetAddress.getLocalHost(), Integer.parseInt(paux.getPort()));
                            System.out.println("");
                            System.out.println("[UNICAST - Envia]");
                            System.out.print(" Enviando Lance " + paux.getId());
                            System.out.print(" para o comprador  " + process.getId());

                            socket.send(messageOut1);

                        } else {
                            System.out.println("Esse produto não está em leilão!");
                            break;
                        }

                    } else {
                        System.out.println("Esse processo não está leiloando");
                        break;
                    }

                    break;
                case "2":
                    System.out.println("LISTA DE PROCESSOS ATIVOS:");
                    iterarSobreListaProcessos();
                    break;
                case "3":

                    System.out.println("CADASTRO DE UM NOVO PRODUTO");
                    System.out.println("Informe o NOME do produto:");
                    nomeProduto = in.nextLine();
                    clearConsole();

                    System.out.println("CADASTRO DE UM NOVO PRODUTO");
                    System.out.println("Informe o ID do produto:");
                    idProduto = in.nextLine();
                    clearConsole();

                    System.out.println("CADASTRO DE UM NOVO PRODUTO");
                    System.out.println("Informe DESCRIÇÃO do produto:");
                    descProduto = in.nextLine();
                    clearConsole();

                    System.out.println("CADASTRO DE UM NOVO PRODUTO");
                    System.out.println("Informe o PREÇO do produto:");
                    precoProduto = in.nextLine();
                    clearConsole();

                    System.out.println("CADASTRO DE UM NOVO PRODUTO");
                    System.out.println("Informe o TEMPO de LEILAO do produto:");
                    tempoLeilao = in.nextLine();
                    clearConsole();

                    //*********************************************
                    //Cria um novo produto.
                    product = new Produto(idProduto, nomeProduto, descProduto, precoProduto, tempoLeilao, nomeProcesso);

                    //*********************************************
                    //Adiciona o produto a lista de produtos.
                    process.getListaProduto().add(product);

                    break;
                case "4":
                    if (process.getListaProduto().isEmpty()) {
                        System.out.println("SUA LISTA DE PRODUTOS ESTÁ VAZIA!");
                        System.out.println("CADASTRO DE UM NOVO PRODUTO");
                        System.out.println("Informe o NOME do produto:");
                        nomeProduto = in.nextLine();
                        clearConsole();

                        System.out.println("CADASTRO DE UM NOVO PRODUTO");
                        System.out.println("Informe o ID do produto:");
                        idProduto = in.nextLine();
                        clearConsole();

                        System.out.println("CADASTRO DE UM NOVO PRODUTO");
                        System.out.println("Informe DESCRIÇÃO do produto:");
                        descProduto = in.nextLine();
                        clearConsole();

                        System.out.println("CADASTRO DE UM NOVO PRODUTO");
                        System.out.println("Informe o PREÇO do produto:");
                        precoProduto = in.nextLine();
                        clearConsole();

                        System.out.println("CADASTRO DE UM NOVO PRODUTO");
                        System.out.println("Informe o TEMPO de LEILAO do produto:");
                        tempoLeilao = in.nextLine();
                        clearConsole();

                        //*********************************************
                        //Cria um novo produto.
                        product = new Produto(idProduto, nomeProduto, descProduto, precoProduto, tempoLeilao, nomeProcesso);

                        //*********************************************
                        //Adiciona o produto a lista de produtos.
                        process.getListaProduto().add(product);

                        System.out.println("Selecione 4 novamente caso queria leiloar o produto que acabou de cadastrar!");
                    } else {
                        String produtoDesejado;

                        System.out.println("SUA LISTA DE PRODUTOS:");
                        iterarSobreListaProdutos(process);
                        System.out.println("Digite um nome de produto da sua lista para leiloar:");
                        produtoDesejado = in.nextLine();

                        if (verificaSeoProdutoExisteNaLista(produtoDesejado, process)) {
                            System.out.println("Produto" + produtoDesejado + "selecionado com sucesso!");
                            //*******************************************************
                            //Envia dados do processo e do produto que deseja leiloar
                            //Realiza uma transmissão multicast para todos os processos
                            //Atualizarem suas listas de produtos e processos leiloeiros
                        } else {
                            System.out.println("Esse produto não existe na lista, para cadastrá-lo utilize a opção 3 do menu.");
                            break;
                        }

                        for (Produto p : process.getListaProduto()) {
                            if (p.getName().equals(produtoDesejado)) {
                                process.getListaProdutosLeiloando().add(p);                 //adiciona o produto na lista de produtos
                               //System.out.println("produto" + p.getName());
                               //System.out.println("Tamanho lista produto leiloando" + process.getListaProdutosLeiloando().size());
                                //leiloando.
                                process.getListaProduto().remove(p);                        //remove o produto da lista de produtos.
                                //System.out.println("Tamanho lista produto leiloando" + process.getListaProduto().size());
                               
                                break;
                            }
                        }
                        
                        System.out.println("Tamanho list aprocessos leiloeiros: " + listaProcessosLeiloeros.size());
                      
                        
                        ByteArrayOutputStream bos1 = new ByteArrayOutputStream(1024);
                        ObjectOutputStream oos1 = new ObjectOutputStream(bos1);
                        oos1.writeChar('S');
                        oos1.writeUTF(process.getId());
                        oos1.writeUTF(process.getPort());
                        oos1.writeObject(process.getListaProduto());
                        oos1.writeObject(process.getListaProdutosLeiloando());

                        oos1.flush();
                        
                        //*****************************************************
                        //Envia a mensagem para todos os processos
                        byte[] m1 = bos1.toByteArray();
                        DatagramPacket messageOut2 = new DatagramPacket(m1, m1.length, group, PORT_MULTICAST);
                        s.send(messageOut2);
                        System.out.println("enviei");
                    }
                    break;
                case "5":

                    mostraLeiloesOcorrendo();

                    break;

                case "6":

                    System.out.println("- seus PRODUTOS:");
                    iterarSobreListaProdutos(process);

                    System.out.println("");
                    System.out.println("- seus PRODUTOS em LEILÃO:");

                    mostraProdutosLeiloandoDesseProcesso(process.getId());

                    break;

                case "7":
                    //**********************************************
                    //Sai do programa

                    System.out.println("Estou saindo!");
                    ByteArrayOutputStream bos1 = new ByteArrayOutputStream(10);
                    ObjectOutputStream oos1 = new ObjectOutputStream(bos1);
                    oos1.writeChar('E');
                    oos1.writeUTF(process.getId());
                    oos1.writeUTF(process.getPort());
                    oos1.flush();
                    //*****************************************************
                    //Envia a mensagem para todos os processos
                    byte[] m2 = bos.toByteArray();
                    DatagramPacket messageOut3 = new DatagramPacket(m2, m2.length, group, PORT_MULTICAST);

                    s.leaveGroup(group);
                    s.close();
                    System.exit(0);

                    break;
                    
                case"8":
                    System.out.println("Lista de processos leiloeiros");
                    for(Processo proc: listaProcessosLeiloeros){
                        System.out.println("Processo leiloeiro: " + proc.getId());
                    }
            }

        }
    }

    public static void iterarSobreListaProcessos() {

        for (Processo p : processList) {
            System.out.println(" - processo: " + p.getId());
        }
    }

    public static Processo verificaProcessoNaLista(String processo) {

        for (Processo p : processList) {
            if (p.getId().equals(processo)) {
                return p;
            }

        }

        return null;
    }

    public static void iterarSobreListaProdutos(Processo process) {

        if (process.getListaProduto().isEmpty()) {
            System.out.println("...está vazia...");
        } else {
            for (Produto p : process.getListaProduto()) {
                System.out.println(" - produto: " + p.getName());
            }
        }
    }

    public static boolean verificaSeoProdutoExisteNaLista(String produtoDesejado, Processo process) {

        for (Produto p : process.getListaProduto()) {
            if (produtoDesejado.equals(p.getName())) {
                return true;
            }
        }
        return false;
    }

    public static Produto verificaoProdutoNaLista(String produtoDesejado, Processo process) {

        Produto prod;
        for (Produto p : process.getListaProdutosLeiloando()) {
            if (produtoDesejado.equals(p.getName())) {
                return prod = p;
            }
        }
        return null;
    }

    public final static void clearConsole() {

        try {
            final String os = System.getProperty("os.name");

            if (os.contains("Windows")) {

                Runtime.getRuntime().exec("cls");

            } else {
                Runtime.getRuntime().exec("clear");
            }
        } catch (final Exception e) {
            //  Tratar Exceptions
        }
    }

    public static void mostraLeiloesOcorrendo() {

        if (listaProcessosLeiloeros.isEmpty()) {
            System.out.println("Não há processos leiloando produtos!");
        } else {
            for (Processo pro : listaProcessosLeiloeros) {

                System.out.println("PROCESSO " + pro.getId() + " está vendendo o(s) seguintes produto(s): ");

                for (Produto n: pro.getListaProdutosLeiloando()) {

                    System.out.print(n.getName() + ", ");
                }
                System.out.print(".");
            }
        }
    }

    public static void nomeProcessosLeiloeiros() {
        for (Processo pro : listaProcessosLeiloeros) {
            System.out.print(pro.getId() + ", ");
        }
    }

    public static boolean verificaSeExisteProessoLeiloeiro(String processo) {
        for (Processo pro : listaProcessosLeiloeros) {
            if (pro.getId().equals(processo)) {

                return true;
            }
        }
        return false;
    }

    public static void mostraProdutosLeiloandoDesseProcesso(String processo) {
        
       
        if (listaProcessosLeiloeros.isEmpty()) {
            System.out.println("...está vazia...");
        } else {

            for (Processo pro : listaProcessosLeiloeros) {
                if (pro.getId().equals(processo)) {
                    for (Produto prod : pro.getListaProdutosLeiloando()) {
                        System.out.print(" " + prod.getName() + ", ");

                    }
                    System.out.print(".");
                }
            }
        }
    }

    public static boolean verificaProdutoListaLeiloando(String processo, String produto) {
        for (Processo pro : listaProcessosLeiloeros) {
            if (pro.getId().equals(processo)) {
                for (Produto prod : pro.getListaProdutosLeiloando()) {
                    if (prod.getName().equals(produto)) {
                        return true;
                    }

                }
            }
        }
        return false;
    }

    

}