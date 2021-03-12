package br.unioeste.ppgcomp.data;

import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public class AtomicData implements Comparable<AtomicData>{
   private Integer timestamp;
    private Data data;

    public AtomicData( Data data, Integer timestamp) {

        this.timestamp = timestamp;
        this.data = data;

    }





    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public Integer getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AtomicData)) return false;
        AtomicData that = (AtomicData) o;
        return Objects.equals(timestamp, that.timestamp) && Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, data);
    }

    @Override
    public String toString() {
        return "atomic-data{" +
                " ts=" + timestamp +
                ", data=" + data +
                '}';
    }

    public void setTimestamp(Integer timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public int compareTo(AtomicData o) {
        if (this.data.getSrc() != o.getData().getSrc())
            return this.data.getSrc() - o.getData().getSrc();
        else
            return this.timestamp - o.getTimestamp();
    }
}
