package br.unioeste.ppgcomp.data;

import java.util.Set;
import java.util.TreeSet;

public class AtomicMessage {
    private int source;
    private TreeSet<Timestamp> tsaggr;
    private Data data;

    public AtomicMessage(int source, TreeSet<Timestamp> tsaggr, Data data) {
        this.source = source;
        this.tsaggr = tsaggr;
        this.data = data;

    }



    public int getSource() {
        return source;
    }

    public void setSource(int source) {
        this.source = source;
    }

    public Set<Timestamp> getTsaggr() {
        return tsaggr;
    }

    public void setTsaggr(TreeSet<Timestamp> tsaggr) {
        this.tsaggr = tsaggr;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public int getMaxTimestamp(){
        return tsaggr.last().getTs();
    }

    @Override
    public String toString() {
        return  "{" + source +
                data +
                '}' +
                 "tsaggr ={"+ tsaggr +
                "}," ;
    }
}
