package br.unioeste.ppgcomp.data;

public class Data {
    private int src;
    private Object data;
    private int ts;

    public Data(int src, Object data, int ts) {
        this.src = src;
        this.data = data;
        this.ts = ts;
    }

    public int getTs() {
        return ts;
    }

    public void setTs(int ts) {
        this.ts = ts;
    }

    public int getSrc() {
        return src;
    }

    public void setSrc(int src) {
        this.src = src;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "{" +
                "(" + src +
                "," + data +
                "),ts=" + ts +
                "}";

    }
}
