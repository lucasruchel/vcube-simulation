package br.unioeste.ppgcomp.data;

public class Timestamp implements Comparable<Timestamp> {
    private int id;
    private int ts;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTs() {
        return ts;
    }

    public void setTs(int ts) {
        this.ts = ts;
    }

    public Timestamp(int id, int ts) {
        this.id = id;
        this.ts = ts;
    }

    @Override
    public String toString() {
        return "{" +
                "id=" + id +
                ", ts=" + ts +
                '}';
    }


    @Override
    public int compareTo(Timestamp o) {
        if (this.ts != o.ts)
            return this.ts - o.ts;
        else
            return this.id - o.id;
    }
}
