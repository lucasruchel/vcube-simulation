package br.unioeste.ppgcomp.topologia;

import java.util.ArrayList;
import java.util.List;

public class VCube {

    private int dim;

    public VCube(int dimensoes){
        this.dim = dimensoes;
    }

    private List<Integer> corrects;

    public List<Integer> getCorrects() {
        return corrects;
    }

    public void setCorrects(List<Integer> corrects) {
        this.corrects = corrects;
    }


    /***
     *
     * @param src origem da mensagem
     * @param p processo atual
     * @return vizinhos de p que estão na subarvore de src
     */
    public List<Integer> subtree(int src, int p){
        // processo de origem está mandando mensagem
        if (src == p){
            return neighborhood(src,dim);
        }else {
            return neighborhood(src,(cluster(src,p) - 1));
        }
    }



    /***
     *
     * @param p nó do hipercube em que será obtida a vizinhança
     * @param h quantidade de clusters do hipercubo que será obtido os processos diretamente conectados
     * @return Lista de inteiros com o vizinhos
     */
    public List<Integer> neighborhood(int p, int h){
        List<Integer> elements = new ArrayList<Integer>();

        // Verifica todos os cluster de 1 até h
        for (int i = 1; i <= h; i++) {
            int e = ff_neighboor(p,i);
            if (e != -1)
                elements.add(e);
        }

        return elements;
    }

    /***
     *
     * @param i nó a ser verificado
     * @param s cluster que será utilizado
     * @return -1 se não encontrar nenhum nó livre de falhas; o número do processo vizinho do nó i
     */

    public int ff_neighboor(int i, int s){
        List<Integer> elements = new ArrayList<Integer>();

        cis(elements, i, s);

        // procura primeiro elemento no cluster J que seja livre de falhas
        int n = 0;
        do {
            if (corrects.contains(elements.get(n)))
                return elements.get(n);
            n++;
        } while (n < elements.size());

        // Nenhum vizinho sem falha encontrado
        return -1;
    }

    public void cis(List<Integer> elements, int id, int cluster){
        // Primeiro elemento do cluster
        int xor = id ^ (int)(Math.pow(2, cluster - 1));

        // Adiciona elemento ao cluster
        elements.add(xor);

        for (int i = 1; i <= cluster - 1; i++) {
            cis(elements,xor,i);
        }

    }

    public int cluster(int i, int j){
        return (MSB(i,j) + 1);
    }

    public int MSB(int i, int j) {
        int s = 0;
        for (int k = i ^ j; k > 0; k = k >> 1) {
            s++;
        }
        return --s;
    }

    public int father(int i, int src) {
        int s = cluster(i, src);
        System.out.println("s = " + s);
        int f = ff_neighboor(src, s);
        System.out.println("f = " + f);
        if (f == i)
            return src;
        else return father(i, f);
    }

}
