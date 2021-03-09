package br.unioeste.ppgcomp.data;

import java.util.Objects;

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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Data data1 = (Data) o;
        return src == data1.src && Objects.equals(data, data1.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(src, data);
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
