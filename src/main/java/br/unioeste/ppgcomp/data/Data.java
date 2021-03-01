package br.unioeste.ppgcomp.data;

public class Data {
    private int src;
    private Object data;

    public Data(int src, Object data) {
        this.src = src;
        this.data = data;
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
                ")"+
                "}";

    }
}
