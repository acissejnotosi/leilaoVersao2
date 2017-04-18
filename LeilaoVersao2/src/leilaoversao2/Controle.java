/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package leilaoversao2;

import java.util.ArrayList;
import java.util.List;

/**
 * Classe controle dos produtos lançados em leilão, controlando o tempo de duração do leilão
 * @author allan
 */
public class Controle {
    
   private String produtoId;
   private List<String> lancadorId;
   private long tempo;
   private boolean tempoFinalizado = false;
   private String ultimo;
   private String precoFinal;

    /**
     * Construtor da Classe Controle
     * @param produtoId
     * @param ultimoLance
     */
    public Controle(String produtoId, String ultimoLance) {
        this.produtoId = produtoId;
        this.ultimo = ultimoLance;
    }
   
    /**
     * Método que retorna a ID do produto
     * @return produtoId
     */
    public String getProdutoId() {
        return produtoId;
    }

    /**
     * Método que seta a ID do produto
     * @param produtoId
     */
    public void setProdutoId(String produtoId) {
        this.produtoId = produtoId;
    }

    /**
     * Método que retorna a Lista de processos envolvidos no leilão
     * @return lancadorId
     */
    public List<String> getLancadorId() {
        return lancadorId;
    }

    /**
     * Método que seta a lista de processos lançados
     * @param lancadorId
     */
    public void setLancadorId(List<String> lancadorId) {
        this.lancadorId = lancadorId;
    }

    /**
     *  Método que retorna o tempo da transação
     * @return tempo long
     */
    public long getTempo() {
        return tempo;
    }

    /**
     * Método que seta o tempo da transação
     * @param tempo
     */
    public void setTempo(long tempo) {
        this.tempo = tempo;
    }

    /**
     * Método que retorna o último lance no produto
     * @return ultimo String
     */
    public String getUltimo() {
        return ultimo;
    }

    /**
     * método que seta o último lanço do produto desejado
     * @param ultimo
     */
    public void setUltimo(String ultimo) {
        this.ultimo = ultimo;
    }

    /**
     * Método que retorna true or false se o tempo final do leilão daquele produto foi alcançado
     * @return tempoFinalizado
     */
    public boolean isTempoFinalizado() {
        return tempoFinalizado;
    }

    /**
     * Método que seta true or false para o tempo final do leilão
     * @param tempoFinalizado
     */
    public void setTempoFinalizado(boolean tempoFinalizado) {
        this.tempoFinalizado = tempoFinalizado;
    }

    public String getPrecoFinal() {
        return precoFinal;
    }

    public void setPrecoFinal(String precoFinal) {
        this.precoFinal = precoFinal;
    }
    
    
    
   
   
    
    
}
