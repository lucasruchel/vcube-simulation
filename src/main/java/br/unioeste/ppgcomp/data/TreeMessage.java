package br.unioeste.ppgcomp.data;

import br.unioeste.ppgcomp.config.Parametros;

import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public class TreeMessage<D> implements Cloneable{
    private TreeSet<Timestamp> tsaggr;
    private BroadcastMessage<D> data;
    private int from;

    public TreeMessage( BroadcastMessage<D> data, int from, TreeSet<Timestamp> tsaggr) {

        this.tsaggr = tsaggr;
        this.data = data;
        this.from = from;

    }


    public BroadcastMessage<D> getData() {
        return data;
    }

    public void setData(BroadcastMessage<D> data) {
        this.data = data;
    }

    public Set<Timestamp> getTsaggr() {
        return tsaggr;
    }

    public void setTsaggr(TreeSet<Timestamp> tsaggr) {
        this.tsaggr = tsaggr;
    }

    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public int getMaxTimestamp(){
        return tsaggr.last().getTs();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TreeMessage)) return false;
        TreeMessage<?> that = (TreeMessage<?>) o;
        return this.from == that.from && tsaggr.equals(that.tsaggr) && data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tsaggr, data, from);
    }

    @Override
    public String toString() {
        if (Parametros.DEBUG)
            return "TREE{" +
                "tsaggr=" + tsaggr +
                ", data=" + data +
                ", from=" + from +
                '}';
        return "TREE";
    }

    @Override
    public TreeMessage<D> clone() {
        return new TreeMessage<>(data,from,new TreeSet<>(tsaggr));
    }
}
