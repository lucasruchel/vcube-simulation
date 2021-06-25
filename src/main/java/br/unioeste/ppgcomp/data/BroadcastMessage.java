package br.unioeste.ppgcomp.data;

import br.unioeste.ppgcomp.config.Parametros;

public class BroadcastMessage<D>{
    private D data;
    private int src;
    private int seq;

    public BroadcastMessage(D data, int src, int seq) {
        this.data = data;
        this.src = src;
        this.seq = seq;
    }



    public D getData() {
        return data;
    }

    public void setData(D data) {
        this.data = data;
    }

    public int getSrc() {
        return src;
    }

    public void setSrc(int src) {
        this.src = src;
    }

    public int getSeq() {
        return seq;
    }

    public void setSeq(int seq) {
        this.seq = seq;
    }

    @Override
    public String toString() {
        if (Parametros.DEBUG)
            return "a-broad{" +
                "data=" + data +
                ", src=" + src +
                ", seq=" + seq +
                '}';
        return "a-broad";
    }
}
