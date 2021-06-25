package br.unioeste.ppgcomp.topologia;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractTopology {


    protected List<Integer> corrects;

    AbstractTopology(int n){
        corrects = new ArrayList<>();
        for (int i=0; i < n; i++)
            corrects.add(i);
    }

    public List<Integer> getCorrects() {
        return corrects;
    }

    public void setCorrects(List<Integer> corrects) {
        this.corrects = corrects;
    }

    public abstract List<Integer> destinations(int me);
    public abstract List<Integer> destinations(int me, int source);

    public List<Integer> fathers(int p, int root){
        return null;
    }

    public int nextNeighboor(int me, int old){
        return -1;
    }
    public abstract List<Integer> neighborhood(int p, int h);


}
