package br.unioeste.ppgcomp.data;

public class AckData {
    private int source;
    private int dest;
    private TreeData m;

    public int getSource() {
        return source;
    }

    public TreeData getM() {
        return m;
    }

    public int getDest() {
        return dest;
    }

    public AckData(int source, int dest, TreeData m) {
        this.source = source;
        this.dest = dest;

        this.m = m;
    }

    @Override
    public String toString() {
        return "AckData{" +
                "source=" + source +
                "dest=" + dest +
                ", m=" + m +
                '}';
    }
}
