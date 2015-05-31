/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dojoarvorebmais;

import java.io.File;
import java.io.RandomAccessFile;

/**
 *
 * @author Carlos Daniel Ogg, Fernando da Rós, João Manoel, Jonatha Nunes,
 * Monique Moledo
 */
public class ArvoreBMais {

    /**
     * Executa busca em Arquivos utilizando Arvore B+ como indice Assumir que
     * ponteiro para próximo nó é igual a -1 quando não houver próximo nó
     *
     * @param codCli: chave do cliente que está sendo buscado
     * @param nomeArquivoMetadados nome do arquivo de metadados
     * @param nomeArquivoIndice nome do arquivo de indice (que contém os nós
     * internos da arvore B+)
     * @param nomeArquivoDados nome do arquivo de dados (que contém as folhas da
     * arvore B+)
     * @return uma instancia de ResultBusca, preenchida da seguinte forma: Caso
     * a chave codCli seja encontrada: encontrou = true pontFolha aponta para a
     * página folha que contém a chave pos aponta para a posição em que a chave
     * se encontra dentro da página
     *
     * Caso a chave codCli não seja encontrada: encontrou = false pontFolha
     * aponta para a última página folha examinada pos informa a posição, nessa
     * página, onde a chave deveria estar inserida
     */
    public ResultBusca busca(int codCli, String nomeArquivoMetadados, String nomeArquivoIndice, String nomeArquivoDados) throws Exception {
        //TODO: Inserir aqui o código do algoritmo       
        RandomAccessFile met = new RandomAccessFile(nomeArquivoMetadados, "rw");

        Metadados meta = Metadados.le(met);
        int pont = meta.pontRaiz;
        if (!meta.raizFolha) {
            boolean controle = false;
            RandomAccessFile arqInd = new RandomAccessFile(nomeArquivoIndice, "rw");
            while (!controle) {
                arqInd.seek(pont);
                NoInterno noInt = NoInterno.le(arqInd);
                for (int i = 0; i < noInt.m; i++) {
                    if (codCli < noInt.chaves.get(i)) {
                        pont = noInt.p.get(i);
                        controle = noInt.apontaFolha;
                        break;
                    }
                    if (i == noInt.m - 1) {
                        pont = noInt.p.get(i + 1);
                        controle = noInt.apontaFolha;
                    }
                }

            }
        }
        //TRATANDO A FOLHA
        RandomAccessFile arqDados = new RandomAccessFile(nomeArquivoDados, "rw");
        arqDados.seek(pont);

        NoFolha n = NoFolha.le(arqDados);
        int onde = 0;
        for (int i = 0; i < n.m; i++) {
            if (n.clientes.get(i).codCliente == codCli) {
                ResultBusca result = new ResultBusca(pont, i, true);
                return result;
            }
            if (n.clientes.get(i).codCliente < codCli) {
                onde++;
                if(i==n.m-1){
                    onde++;
                }
            }

        }
        ResultBusca result = new ResultBusca(pont, onde, false);
        return result;

    }

    /**
     * Executa inserção em Arquivos Indexados por Arvore B+
     *
     * @param codCli: código do cliente a ser inserido
     * @param nomeCli: nome do Cliente a ser inserido
     * @param nomeArquivoMetadados nome do arquivo de metadados
     * @param nomeArquivoIndice nome do arquivo de indice (que contém os nós
     * internos da arvore B+)
     * @param nomeArquivoDados nome do arquivo de dados (que contém as folhas da
     * arvore B+)* @return endereço da folha onde o cliente foi inserido, -1 se
     * não conseguiu inserir retorna ponteiro para a folha onde o registro foi
     * inserido
     */
    
    
    private void particionaNoInterno(String nomeArquivoMetadados, NoInterno no, int pontFilhoMenor, RandomAccessFile ArqNos, boolean pontFolha) throws Exception{
                RandomAccessFile ArqMeta = new RandomAccessFile(new File(nomeArquivoMetadados), "rw");
                Metadados meta = Metadados.le(ArqMeta);
                
                ArqNos.seek(meta.pontProxNoInternoLivre);
                NoInterno novoNo = new NoInterno();
                int pontFilhoMaior = meta.pontProxNoInternoLivre;
                meta.pontProxNoInternoLivre += NoInterno.TAMANHO;
                
                for (int i = 0; i < NoInterno.d; i++) {
                    novoNo.chaves.add(no.chaves.get(NoInterno.d+1));
                    no.chaves.remove(NoInterno.d+1);
                    novoNo.p.add(no.p.get(NoInterno.d+1+i));                   
                }
                novoNo.p.add(no.p.get(no.p.size()-1));
                for (int i = 0; i < NoInterno.d; i++) {
                  no.p.remove(NoInterno.d+1);
                }
                
                novoNo.m = novoNo.chaves.size();
                no.m = no.chaves.size();
                
                NoInterno pai = null;
                
                if(no.pontPai<0){ //então o nó é raiz
                    
                   int novoPontRaiz = meta.pontProxNoInternoLivre;
                   meta.pontProxNoInternoLivre += NoInterno.TAMANHO;
                   meta.pontRaiz = novoPontRaiz;
                   
                   ArqMeta.seek(0);
                   meta.salva(ArqMeta);
                   ArqMeta.close();
                   
                   
                   pai = new NoInterno();
                   no.pontPai=novoPontRaiz;
                   novoNo.pontPai = no.pontPai;
                   pai.p.add(pontFilhoMenor);
                }
                
                else {
                    ArqNos.seek(no.pontPai);
                    pai = NoInterno.le(ArqNos);
                }
                
                int posCerta = 0; //achar posição ordenada para o cliente q vai subir
                for (int i = 0; i < pai.m; i++) {
                  if(no.chaves.get(NoInterno.d) > pai.chaves.get(i)){
                    posCerta++;
                  }
            
                }
                pai.chaves.add(posCerta, no.chaves.get(NoInterno.d)); 
                no.chaves.remove(NoInterno.d);
                //no.p.remove(NoInterno.d);
                no.m = no.chaves.size();
                novoNo.pontPai = no.pontPai;
                pai.p.add(posCerta+1, pontFilhoMaior);
                pai.m = pai.chaves.size();
                
                ArqNos.seek(pontFilhoMaior);
                novoNo.apontaFolha = pontFolha;
                novoNo.m = novoNo.chaves.size();
                novoNo.salva(ArqNos);
                ArqNos.seek(pontFilhoMenor);
                no.salva(ArqNos);
                pontFilhoMenor = no.pontPai;
                ArqNos.seek(pontFilhoMenor);
                pai.salva(ArqNos);
                
                if(pai.chaves.size()> 2*NoInterno.d){//verificar se a página/nó ficou cheio
                    particionaNoInterno(nomeArquivoMetadados, pai, pontFilhoMenor, ArqNos, false);
            }
                
    };
    
    public int insere(int codCli, String nomeCli, String nomeArquivoMetadados, String nomeArquivoIndice, String nomeArquivoDados) throws Exception {
        //TODO: Inserir aqui o código do algoritmo de inserção
        ResultBusca resultado;
        resultado = busca(codCli, nomeArquivoMetadados, nomeArquivoIndice, nomeArquivoDados);
        if(resultado.encontrou){
            return -1;
        } else {
            RandomAccessFile folha = new RandomAccessFile(nomeArquivoDados, "rw");
            folha.seek(resultado.pontFolha);
            NoFolha no = NoFolha.le(folha);
            Cliente monique = new Cliente(codCli,nomeCli);
            no.clientes.add(resultado.pos, monique);
            no.m = no.clientes.size();
            int result = resultado.pontFolha;
            if(no.clientes.size() > 2*NoFolha.d){
               RandomAccessFile meta = new RandomAccessFile(nomeArquivoMetadados, "rw");
               meta.seek(0);
               Metadados metadados = Metadados.le(meta);
               folha.seek(metadados.pontProxNoFolhaLivre);
               int pontProNovo = metadados.pontProxNoFolhaLivre;
               metadados.pontProxNoFolhaLivre += NoFolha.TAMANHO;
               meta.seek(0);
               metadados.salva(meta);
               NoFolha novo = new NoFolha();
                for (int i = 0; i < NoFolha.d+1; i++) {
                    novo.clientes.add(no.clientes.get(NoFolha.d));
                    no.clientes.remove(NoFolha.d);
                }
                if(resultado.pos >= NoFolha.d){
                    result = pontProNovo;
                }
                
                //encadeando a lista de folhas
                int temp = no.pontProx;
                no.pontProx = pontProNovo;
                novo.pontProx = temp;
                novo.m = novo.clientes.size();
                no.m = no.clientes.size();
                //TEMOS QUE SUBIR O ELEMENTO CLIENTES(D)
                RandomAccessFile ArqIndice = new RandomAccessFile(nomeArquivoIndice, "rw");
                if(no.pontPai<0){//o nó eh raiz, tem que aumentar altura da arvore.
                    int pontNovaRaiz = metadados.pontProxNoInternoLivre;
                    metadados.pontProxNoInternoLivre += NoInterno.TAMANHO;
                    metadados.pontRaiz = pontNovaRaiz;
                    metadados.raizFolha = false;
                    meta.seek(0);
                    metadados.salva(meta);
                    no.pontPai = pontNovaRaiz;
                    
                    ArqIndice.seek(no.pontPai);
                    NoInterno pai = new NoInterno();
                    pai.apontaFolha = true;
                    pai.pontPai = -1;
                    pai.chaves.add(novo.clientes.get(0).codCliente);
                    pai.p.add(resultado.pontFolha);
                    pai.p.add(pontProNovo);
                    pai.m = pai.chaves.size();
                    ArqIndice.seek(no.pontPai);
                    pai.salva(ArqIndice);
                }else{
                    
                
                
                ArqIndice.seek(no.pontPai);
                NoInterno pai = NoInterno.le(ArqIndice);
                                
                int posCerta = 0; //achar posição ordenada para o cliente q vai subir
                for (int i = 0; i < pai.m; i++) {
                    if(novo.clientes.get(0).codCliente > pai.chaves.get(i)){
                        posCerta++;
                    }
                }
                pai.chaves.add(posCerta, novo.clientes.get(0).codCliente);
                pai.p.add(posCerta+1, pontProNovo);
                pai.m = pai.chaves.size();
                if(pai.m > 2* NoInterno.d){
                    int pontIrmao = novo.pontProx;
                    while(pontIrmao > 0){
                        folha.seek(pontIrmao);
                        NoFolha irmao = NoFolha.le(folha);
                        irmao.pontPai = metadados.pontProxNoInternoLivre;
                        folha.seek(pontIrmao);
                        irmao.salva(folha);
                        pontIrmao = irmao.pontProx;
                    }
                    particionaNoInterno(nomeArquivoMetadados, pai, no.pontPai, ArqIndice, true);
                    
                }
                
                ArqIndice.seek(no.pontPai);
                pai.salva(ArqIndice);
                }
                
                novo.pontPai = no.pontPai;
                folha.seek(pontProNovo);
                novo.salva(folha);
                folha.seek(resultado.pontFolha);
                no.salva(folha);
                
                ArqIndice.close();
                meta.close();       
            }
            
            
            folha.seek(resultado.pontFolha);
            no.salva(folha);
            folha.close();
            return result;
        }
        
    }

    /**
     * Executa exclusão em Arquivos Indexados por Arvores B+
     *
     * @param codCli: chave do cliente a ser excluído
     * @param nomeArquivoMetadados nome do arquivo de metadados
     * @param nomeArquivoIndice nome do arquivo de indice (que contém os nós
     * internos da arvore B+)
     * @param nomeArquivoDados nome do arquivo de dados (que contém as folhas da
     * arvore B+) * @return endereço do cliente que foi excluído, -1 se cliente
     * não existe retorna ponteiro para a folha onde o registro foi excluido
     */
    public int exclui(int CodCli, String nomeArquivoMetadados, String nomeArquivoIndice, String nomeArquivoDados) {
        //TODO: Inserir aqui o código do algoritmo de remoção
        return Integer.MAX_VALUE;
    }
}
