package br.unioeste.ppgcomp.data;

import java.util.Set;
import java.util.TreeSet;

public class AtomicData implements Comparable<AtomicData>{
   private Timestamp timestamp;
    private Data data;

    public AtomicData( Data data, Timestamp timestamp) {

        this.timestamp = timestamp;
        this.data = data;

    }





    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "atomic-data{" +
                " ts=" + timestamp +
                ", data=" + data +
                '}';
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public int compareTo(AtomicData o) {
        return timestamp.compareTo(o.timestamp);
    }
}
