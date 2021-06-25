package br.unioeste.ppgcomp.data;

import br.unioeste.ppgcomp.config.Parametros;

import java.util.Objects;

public class ACKPending<D> implements Comparable<ACKPending> {
    private TreeMessage<D> data;
    private int id;
    private int from;

    public ACKPending(TreeMessage data, int id) {
        this.data = data;
        this.id = id;
    }

    public TreeMessage<D> getData() {
        return data;
    }

    public void setData(TreeMessage<D> data) {
        this.data = data;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ACKPending<?> that = (ACKPending<?>) o;
        return id == that.id && data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data, id);
    }



    @Override
    public int compareTo(ACKPending o) {
        if (!data.equals(o.getData()))
            return data.hashCode() - o.getData().hashCode();
        else
            return this.id - o.id;
    }

    @Override
    public String toString() {
        if (Parametros.DEBUG)
            return "ack-reply{" +
                "data=" + data +
                ", id=" + id +
                '}';
        return "ack-reply";
    }
}
