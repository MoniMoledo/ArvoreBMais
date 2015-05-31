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
            no.clientes.add(resultado.pos,monique);
            if(no.clientes.size() > 2*NoFolha.d){
               RandomAccessFile meta = new RandomAccessFile(nomeArquivoMetadados, "rw");
               Metadados metadados = Metadados.le(meta);
               folha.seek(metadados.pontProxNoFolhaLivre);
               int pontProNovo = metadados.pontProxNoFolhaLivre;
               metadados.pontProxNoFolhaLivre += Metadados.TAMANHO;
               metadados.salva(meta);
               NoFolha novo = NoFolha.le(folha);
                for (int i = 0; i < NoFolha.d+1; i++) {
                    novo.clientes.add(no.clientes.get(i+NoFolha.d));
                    no.clientes.remove(NoFolha.d);
                }
                int temp = no.pontProx;
                no.pontProx = pontProNovo;
                novo.pontProx = temp;
                novo.pontPai = no.pontPai;
                novo.m = novo.clientes.size();
                no.m = no.clientes.size();
                novo.salva(folha);
                no.salva(folha);
               folha.seek(no.pontPai);//TEMOS QUE SUBIS O ELEMENTO CLIENTES(D)
            }
        }
        
        return Integer.MAX_VALUE;
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
