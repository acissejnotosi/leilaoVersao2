package leilaoversao2;

import java.io.Serializable;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe que contém as características de um processo como id, lista de produtos e port para comunicação Unicast
 * @author Jessica e Allan
 */
class Processo implements Serializable {

    private String id;
    private String port;
    private List<Produto> listaProduto;
    private List<Produto> listaProdutosLeiloando;
    private PublicKey chavePublica;
    private PrivateKey chavePrivada;
    public boolean ativo;

    /**
     * Construtor da classe Processo
     *
     * @param String- id
     * @param String - port,
     * @param PublicKey - chavePublica
     * @return -
     */
    public Processo(String id, String port, PublicKey chavePublica, 
            List<Produto> listaProduto, List<Produto> listaProdutosLeiloando) {
        this.id = id;
        this.port = port;
        this.chavePublica = chavePublica;
        this.listaProduto = listaProduto;
        this.listaProdutosLeiloando = listaProdutosLeiloando;
   
    }

    /**
     * Construtor alternativo da classe Processo
     *
     * @param String- id
     * @param String - port,
     * @param PublicKey - chavePublica
     * @return -
     */
    public Processo(String id, String port, PublicKey chavePublica, PrivateKey chavePrivada) {
        this.id = id;
        this.port = port;
        this.chavePublica = chavePublica;
        this.chavePrivada = chavePrivada;
    }

    /**
     * Método que retorna o ID do processo
     *
     * @param -
     * @return String - id
     */
    public String getId() {
        return id;
    }

    /**
     * Método que retorna o Port para comunicação Unicast de um Processo
     *
     * @param -
     * @return String - port
     */
    public String getPort() {
        return port;
    }

    /**
     * Método que retorna a Chave Pública do processo
     *
     * @param -
     * @return PublicKey - chavePublica
     */
    public PublicKey getChavePublica() {
        return chavePublica;
    }

    /**
     * Método que imprime imprime as informações de ID e Port do processo
     *
     * @param -
     * @return String - "Participante: " + id + ", Porta:" + port
     */
    public String imprimaProcessos() {
        return "Participante: " + id + ", Porta: " + port;
    }

    /**
     * Método que imprime os Participantes
     *
     * @param -
     * @return String - id
     */
    public String imprimaParticipantes() {
        return "Participante: " + id;
    }

    /**
     * Método que seta o Id do processo
     * @param String - id
     * @return void
     */
    public void setId(String id) {
        this.id = id;
    }

    public void setListaProduto(List<Produto> listaProduto) {
        for(Produto prod: listaProduto){
            this.listaProduto.add(prod);
        }
        
        
            System.out.println("setlistaProduto tam: " + this.listaProduto.size());
        
    }

    public void setListaProdutosLeiloando(List<Produto> listaProdutosLeiloando) {
         for(Produto prod: listaProdutosLeiloando){
            this.listaProdutosLeiloando.add(prod);
        }
        
        
            System.out.println("setlistaProduto tam: " + this.listaProdutosLeiloando.size());
    }

    public void setChavePrivada(PrivateKey chavePrivada) {
        this.chavePrivada = chavePrivada;
    }

    public List<Produto> getListaProduto() {
        return listaProduto;
    }

    public   List<Produto> getListaProdutosLeiloando() {
        return listaProdutosLeiloando;
    }

    public PrivateKey getChavePrivada() {
        return chavePrivada;
    }

    /**
     * Método que seta o Port do processo
     * @param String - port
     * @return void
     */

    public void setPort(String port) {
        this.port = port;
    }

    /**
     * Método que seta a chave publica do processo
     * @param PublicKey chavePublica
     * @return void
     */
    public void setChavePublica(PublicKey chavePublica) {
        this.chavePublica = chavePublica;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
    
    

}