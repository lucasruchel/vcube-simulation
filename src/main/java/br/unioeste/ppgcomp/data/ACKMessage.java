package br.unioeste.ppgcomp.data;

import java.util.Objects;

public class ACKMessage implements Comparable<ACKMessage> {
    private Data data;
    private int root;
    private int id;
    private int source;

    public ACKMessage(int id, Data data, int source, int root) {
        this.data = data;
        this.id = id;
        this.root = root;
        this.source = source;
    }



    public int getRoot() {
        return root;
    }

    public void setRoot(int root) {
        this.root = root;
    }


    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSource() {
        return source;
    }

    public void setSource(int source) {
        this.source = source;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ACKMessage)) return false;
        ACKMessage that = (ACKMessage) o;
        return root == that.root && id == that.id && source == that.source && Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data, root, id, source);
    }

    @Override
    public String toString() {
        return "ACK{" +
                "data=" + data +
                ", root=" + root +
                ", id=" + id +
                ", source=" + source +
                '}';
    }

    @Override
    public int compareTo(ACKMessage o) {
        if (o.getRoot() != this.getRoot())
            return this.getRoot() - o.getRoot();
        else
            return this.id - o.id;
    }
}
