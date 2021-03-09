package br.unioeste.ppgcomp.data;

public class TSDataMessage implements Comparable<TSDataMessage>{

    private int p;
    private Data data;
    private int ts;




    public TSDataMessage(int p, Data data, int ts){
        this.data = data;
        this.p = p;
        this.ts = ts;

    }

    public TSDataMessage clone(){
        return new TSDataMessage(p,data,ts);
    }

    @Override
    public String toString() {
        return p + "{" +
                "v=" + data +
                ",ts=" + ts +
                '}';
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + this.p;
        hash = 97 * hash + this.getData().hashCode();
        hash = 97 * hash + this.ts;
        return hash;
    }



    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        final TSDataMessage other = (TSDataMessage) obj;
        if (this.p != other.p) {
            return false;
        }
        if (this.ts != other.ts) {
            return false;
        }
        if (this.data != other.data) {
            return false;
        }

        System.out.println("Recebido igual!");
        return true;
    }


    public Data getData() {
        return data;
    }

    public int getP() {
        return p;
    }

    public void setP(int p) {
        this.p = p;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public int getTs() {
        return ts;
    }

    public void setTs(int ts) {
        this.ts = ts;
    }

    @Override
    public int compareTo(TSDataMessage o) {
        if (this.ts != o.ts)
            return this.ts - o.ts;
        else if (this.p != o.p)
            return this.p - o.p;
        else
            return this.data.getSrc() - o.data.getSrc();
    }
}
