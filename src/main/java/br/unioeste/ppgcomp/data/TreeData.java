package br.unioeste.ppgcomp.data;

public class TreeData {
    int ts;
    int source;
    Object data;

    public TreeData(int ts, int source, Object data){
        this.data = data;
        this.ts = ts;
        this.source = source;
    }

    public TreeData clone(){
        return new TreeData(ts,source,data);
    }

    @Override
    public String toString() {
        return "TreeData{" +
                "ts=" + ts +
                ", source=" + source +
                ", data=" + data +
                '}';
    }

    public int getTs() {
        return ts;
    }

    public int getSource() {
        return source;
    }

    public Object getData() {
        return data;
    }
}
