package br.unioeste.ppgcomp.data;

import java.util.TreeSet;

public class AtomicDataMessage {
    private int source;
    private TreeSet<TSDataMessage> tsaggr;
    private Data data;
    private boolean deliverable;

    public AtomicDataMessage(int source, TreeSet<TSDataMessage> tsaggr, Data data) {
        this.source = source;
        this.tsaggr = tsaggr;
        this.data = data;
        this.deliverable = false;
    }

    public boolean isDeliverable() {
        return deliverable;
    }

    public void setDeliverable(boolean deliverable) {
        this.deliverable = deliverable;
    }

    public int getSource() {
        return source;
    }

    public void setSource(int source) {
        this.source = source;
    }

    public TreeSet<TSDataMessage> getTsaggr() {
        return tsaggr;
    }

    public void setTsaggr(TreeSet<TSDataMessage> tsaggr) {
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
                 " tsaggr ={"+ tsaggr +
                "}," ;
    }
}
