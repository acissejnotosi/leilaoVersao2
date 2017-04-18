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
import java.net.UnknownHostException;
import java.security.PublicKey;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import static leilaoversao2.LeilaoVersao2.assinatura;
import static leilaoversao2.LeilaoVersao2.listaProcessosLeiloeros;

import static leilaoversao2.LeilaoVersao2.procesosInteresados;
import static leilaoversao2.LeilaoVersao2.processList;
import static leilaoversao2.LeilaoVersao2.produtosLancados;


/**
 * Classe para o recebimento das mensagens Unicast.
 *
 * @author Jessica
 */
public class ServidorUniCast extends Thread {

    static boolean teste = false;
    DatagramSocket socket = null;
    MulticastSocket s = null;
    InetAddress group = null;
    Processo process = null;
    String MULT_IP = null;
    int MULT_PORT = 0;
    static Map<String,String> vivos = new HashMap<String,String>();
    static String  vivo = null;
    static boolean stop = false;

    /**
     * Contrutor da Classe ServidorUnicast
     *
     * @param p Processo
     * @param MULT_IP IP para o Multicast
     * @param MULT_PORT Port para o Multicast
     */
    public ServidorUniCast(Processo p, String MULT_IP, int MULT_PORT) {
        this.process = p;
        this.MULT_IP = MULT_IP;
        this.MULT_PORT = MULT_PORT;

        try {
            socket = new DatagramSocket(Integer.parseInt(process.getPort()));
        } catch (IOException ex) {
            System.out.println("Creation of socket: " + ex);
        }

        try {
            group = InetAddress.getByName(MULT_IP);
            s = new MulticastSocket(MULT_PORT);
            s.joinGroup(group);
        } catch (IOException e) {
            System.out.println("Joining Multicast:" + e.getMessage());
        }
    }

    @Override
    public void run() {
        // ********************************************
        // Instanciado thread
        WatchDog watchdog;
        try {
            watchdog = new WatchDog(socket, InetAddress.getLocalHost(), process.getPort(), process.getId());
            watchdog.start();
        } catch (UnknownHostException ex) {
            Logger.getLogger(ServidorUniCast.class.getName()).log(Level.SEVERE, null, ex);
        }


        byte[] buffer;
        DatagramPacket messageIn;
        ByteArrayInputStream bis;
        ObjectInputStream ois;
        Chaves gera_chave = new Chaves();

        while (true) {

            try {
                String pid;
                String port;
                char type;
                PublicKey pubKey;
                String nomeProduto;
                String idProduto;
                String descProduto;
                String precoProduto;

                // ********************************************
                // Recebendo mensagem UDP
                buffer = new byte[1024];
                messageIn = new DatagramPacket(buffer, buffer.length);
                socket.receive(messageIn);
                bis = new ByteArrayInputStream(buffer);
                ois = new ObjectInputStream(bis);
                type = ois.readChar();

                switch (type) {

                    case ('N'):
                        // *********************************************
                        // Descompactando messagem
                        pid = ois.readUTF();
                        port = ois.readUTF();
                        PublicKey chavePublica = (PublicKey) ois.readObject();

                        List<Produto> listaProduto = (ArrayList<Produto>) ois.readObject();

                        List<Produto> listaProdutosleiloando = (List<Produto>) ois.readObject();

                        // *********************************************
                        // Criando um novo processo
                        Processo novoProcesso = new Processo(pid, port, chavePublica, listaProduto, listaProdutosleiloando);
                        LeilaoVersao2.processList.add(novoProcesso);

                        // *********************************************
                        // Adicionando aminha Lista de Produtos produto recebido
//                        adicionaListaDeProdutos(process.getId(), listaProduto);

                        // *********************************************
                        // Gerando Hash Map Encripta
                        Autenticacao auto = new Autenticacao();
                        auto.setPublic_chave(chavePublica);
                        gera_chave = new Chaves();
                        assinatura.put(pid, auto);

                        // System.out.println("Lista size novo unicas " + listaProdutos.size());
                        // System.out.println("Lista size novo recebida " + listaProduto.size());
                        System.out.println("");
                        System.out.println("[UNICAST - Recebe]");
                        System.out.println(" ID do participante: " + pid);
                        System.out.println(", Porta: " + port);
                        System.out.println(", Chave publica: - ");
                        System.out.println("Lista size novo unicast " + listaProduto.size());

                        break;

                    case ('B'):
                        //***************************************************
                        //desenpacota mensagem de lance
                        pid = ois.readUTF();
                        port = ois.readUTF();
                        String lance = ois.readUTF();
                        idProduto = ois.readUTF(); //Id produto do processo atual(leiloero)
                        Integer tamanho = ois.read();//Id produto do processo atual(leiloero)
                         setStop(false);
                        // *********************************************
                        // Lendo byte array
                        byte[] mensagemCripto = new byte[tamanho];
                        for (int i = 0; i < tamanho; i++) {
                            mensagemCripto[i] = ois.readByte();
                        }
                        String temp = ois.readUTF();
                        int tempo = Integer.parseInt(temp);
                        PublicKey chavePublica1 = assinatura.get(pid).getPublic_chave();

                        // *********************************************
                        // Descriptografando mensagem recebido com chaave Publicado do Processo que enviou requisiço
                        String decrypedText = gera_chave.decriptografa(mensagemCripto, chavePublica1);

                        // *********************************************
                        // Comparamdo atuendicidade da mensagem de assinatura
                        if (!decrypedText.equals("kkkk")) {
                            ClienteNaoAutenticado(pid, port);
                            break;
                        }
                        System.out.println("");
                        System.out.print("[UNICAST - Recebe]");
                        System.out.println("Requisicao de lance do processo: " + pid);
                        System.out.print(", valor do lance: " + lance);

                        // verifica valor do lance maior de que valor do produto
                        Produto produto = buscaUmProdutoPorId(process.getId(), idProduto);
                        int to = Integer.parseInt(produto.getPrecoInicial());
                        int teste = Integer.parseInt(lance);

                        if (to > Integer.parseInt(lance)) {
                            System.out.println("Valor do Lance não é suficiente!");
                            break;
                        }

                        //alguem ja deu um lance nesse produto
                        boolean lancar = true;
                        if (produtosLancados.isEmpty()) {
                            produtosLancados.add(idProduto);
                            lancar = true;
                        } else {
                            for (String c : LeilaoVersao2.produtosLancados) {
                                if (c.equals(idProduto)) {
                                    lancar = false;
                                    break;
                                }
                            }
                        }

                        // Enviar multcast atualizando valor do produto 
                        if (lancar) {
                            //setar controlador de lances
                            adiconaProcessoInteresado(pid, idProduto);
                            if(procesosInteresados.size()==0){
                                Controle cont = new Controle(idProduto,pid);
                                cont.setLancadorId(new ArrayList<>());
                                cont.getLancadorId().add(pid);
                                cont.setUltimo(pid);
                                cont.setPrecoFinal(lance);
                                procesosInteresados.add(cont);
                            }
                            for(Processo proce: processList){
                                if(proce.getId().equals(process.getId())){
                                    proce.setAtivo(true);
                                }
                            }
                                
                            //atualiza valores de preco local
                            atualizaPrecoProdutoLocal(idProduto, lance);

                            Cronometro cro = new Cronometro(socket, idProduto, process.getId(), s, group, MULT_PORT,tempo);
                            cro.start();
                           
                            System.out.println("Leilao Inicializado produtoID:" + idProduto);
                            /// Enviando atualizacao de preco para todo multicast 
                            atualizaValorCliente(pid, idProduto, lance);
                        } else {
                            /// Enviando atualizacao de preco para todo multicast 
                            System.out.println("Lancar Notificaçao para outro interesado");
//                            notificacaParaCliente(pid,port,idProduto,lance);
                            for (Controle c : procesosInteresados) {
                                c.setPrecoFinal(lance);
                                if (c.getProdutoId().equals(idProduto)) {
                                    for (String ids : c.getLancadorId()) {
                                        if (!ids.equals(pid)) {
                                            Processo p = procuraUmprocesso(ids);
                                            notificacaParaCliente(pid, p.getPort(), idProduto, lance);
                                        }
                                    }
                                }
                            }
                            //setar controlador de lances
                            adiconaProcessoInteresado(pid, idProduto);
                            atualizaValorCliente(pid, idProduto, lance);
                        }

                        break;
                    case ('F'):
                        //Finaliza Leilao tempo estorado

                        String leiloeiroID = ois.readUTF();
                        pid = ois.readUTF();
                        port = ois.readUTF();
                        Produto meuProduto = (Produto) ois.readObject();
                        atualizaPrecoProdutoLocal(meuProduto.getId(),meuProduto.getPrecoInicial());


                        System.out.print("[UNICAST - Recebe]");
                        System.out.print(" ID do participante: " + pid);
                        System.out.print(", Porta: " + port);
                        System.out.println("\nProduto arrematado com sucesso: " + meuProduto.getName());

                        break;

                    case ('U'):
                        ////Notificaçao de lance maior 

                        pid = ois.readUTF();
                        idProduto = ois.readUTF();
                        String novoValor = ois.readUTF();
                        System.out.println("     Notificaço recebido com sucesso!!!!!");
                        System.out.println("[UNICAST - Recebe]");
                        System.out.println(" ID do participante deu lance maior: " + pid);
                        System.out.print(", Valor do ultimo lance:  " + novoValor);

                        break;
                   case ('K'):
                        // *********************************************
                        // Usuario nao autenticado
                        pid = ois.readUTF();
                        System.out.println("Nao foi possivel realizar seu lance assinatura errada!");
                        break;
                        
                    case ('W'):
                        // *********************************************
                        // Envia resposta para watchdoog
                        pid = ois.readUTF();
                        if (!process.getId().equals(pid)) {
                            port = ois.readUTF();
                            repostaWatch(process.getId(), port);
                        }
                        break;
                    case ('R'):
                        // *********************************************
                        // Confimar que esta vivo             
                        pid = ois.readUTF();
                        setVivo(pid);
                        vivos.put(pid, "true");
                        break;


                }

            } catch (IOException ex) {
                System.out.println("Unicast Exception");
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(ServidorUniCast.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static boolean isTeste() {
        return teste;
    }

    public static void setTeste(boolean teste) {
        ServidorUniCast.teste = teste;
    }

    public void repostaWatch(String id, String port) throws IOException {

        // *********************************************
        // Envia confirmacao recebeu  o unicast
        ByteArrayOutputStream bos = new ByteArrayOutputStream(10);
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeChar('R');
        oos.writeUTF(process.getId());
        oos.flush();

        byte[] output = bos.toByteArray();
        DatagramPacket request = new DatagramPacket(output, output.length, InetAddress.getLocalHost(), Integer.parseInt(port));
        socket.send(request);
    }


    public void atualizaValorCliente(String id, String idProduto, String novoValor) throws IOException {

        System.out.println("");
        System.out.print("[MULTICAST - Enviando]");
        System.out.print("Atualiza valores de produto");

        // *********************************************
        // Packing transaction validation.
        ByteArrayOutputStream bos = new ByteArrayOutputStream(10);
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeChar('A');
        oos.writeUTF(id);
        oos.writeUTF(idProduto);
        oos.writeUTF(novoValor);

        oos.flush();

        byte[] m1 = bos.toByteArray();
        DatagramPacket messageOut = new DatagramPacket(m1, m1.length, group, MULT_PORT);
        s.send(messageOut);

    }

    public void notificacaParaCliente(String id, String port, String idProduto, String novoValor) throws IOException {

        System.out.println("");
        System.out.print("[UNIACAST - Enviando]");
        System.out.print("Outro participante deu um lance valor:" + novoValor);
        System.out.println("Porta:" + port);
        // *********************************************
        // Packing transaction validation.
        ByteArrayOutputStream bos = new ByteArrayOutputStream(10);
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeChar('U');
        oos.writeUTF(id);
        oos.writeUTF(idProduto);
        oos.writeUTF(novoValor);
        oos.flush();

        byte[] output = bos.toByteArray();
        DatagramPacket request = new DatagramPacket(output, output.length, InetAddress.getLocalHost(), Integer.parseInt(port));
        socket.send(request);

    }

    public void ClienteNaoAutenticado(String id, String port) throws IOException {

        System.out.println("");
        System.out.print("[UNIACAST - Enviando]");
        System.out.print("Seu lance nao esta realizado, pois nao esta autenticado");
        // *********************************************
        // Packing transaction validation.
        ByteArrayOutputStream bos = new ByteArrayOutputStream(10);
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeChar('K');
        oos.flush();

        byte[] output = bos.toByteArray();
        DatagramPacket request = new DatagramPacket(output, output.length, InetAddress.getLocalHost(), Integer.parseInt(port));
        socket.send(request);

    }

    public static Processo procuraUmprocesso(String id) {

        Processo process = null;

        for (Processo p : processList) {
            if (p.getId().equals(id)) {
                process = p;
            }
        }
        return process;

    }

    public static void adiconaProcessoInteresado(String idProcesso, String idProduto) {

        for (Controle c : procesosInteresados) {
            if (c.getProdutoId().equals(idProduto)) {
                c.setUltimo(idProcesso);
                if (c.getLancadorId() != null) {
                    for (String ids : c.getLancadorId()) {
                        if (!ids.equals(idProcesso)) {
                            c.getLancadorId().add(idProcesso);
                            break;
                        }
                    }
                } else {
                    c.setLancadorId(new ArrayList<>());
                }
                c.getLancadorId().add(idProcesso);
            }
        }
       
    }

    public static Produto buscaUmProdutoPorId(String process, String idProduto) {

        for (Processo proc : processList) {
            if (proc.getId().equals(process)) {
                for (Produto p : proc.getListaProdutosLeiloando()) {
                    if (p.getId().equals(idProduto)) {
                        return p;

                    }

                }
            }
        }

        return null;
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

    public void atualizaPrecoProdutoLocal(String idProduto, String lance) {

        //atualiza valor do proiduto local
        for (Processo proc : listaProcessosLeiloeros) {
            if (proc.getId().equals(process.getId())) {
                for (Produto p : proc.getListaProduto()) {
                    if (p.getId().equals(idProduto)) {
                        p.setPrecoInicial(lance);
                        break;
                    }
                }
            }
        }

    }
    
    synchronized public static String getVivo() {
        return vivo;
    }

    synchronized public static void setVivo(String vivo) {
        ServidorUniCast.vivo = vivo;
    }

    public static Map<String, String> getVivos() {
        return vivos;
    }

    public static void setVivos(Map<String, String> vivos) {
        ServidorUniCast.vivos = vivos;
    }

    public static boolean isStop() {
        return stop;
    }

    public static void setStop(boolean stop) {
        ServidorUniCast.stop = stop;
    }
    

}
