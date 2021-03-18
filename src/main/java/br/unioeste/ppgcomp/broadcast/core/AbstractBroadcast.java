package br.unioeste.ppgcomp.broadcast.core;

import br.unioeste.ppgcomp.config.Parametros;
import br.unioeste.ppgcomp.data.AtomicData;
import br.unioeste.ppgcomp.fault.CrashProtocol;
import br.unioeste.ppgcomp.topologia.AbstractTopology;
import br.unioeste.ppgcomp.topologia.VCubeTopology;
import lse.neko.*;
import lse.neko.failureDetectors.FailureDetectorListener;
import lse.neko.util.TimerTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public abstract class AbstractBroadcast extends CrashProtocol implements FailureDetectorListener {

    protected boolean DEBUG = true;

    protected double DELAY = 0.1;

    // Identificador do process
    protected int me;
    // Numero de processos
    protected int np;
    protected List<Integer> corretos;

    protected int dim;

    // Overlay dos processos para encaminhamento em árvore
    protected AbstractTopology topo;

    public AbstractBroadcast(NekoProcess process, SenderInterface sender, String name) {
        super(process, sender, name);

        np = process.getN();
        me = process.getID();

        corretos = new ArrayList<>();
        for (int i = 0; i < np; i++) {
            corretos.add(i);
        }

        dim = (int) (Math.log10(np)/Math.log10(2));
        topo = new VCubeTopology(dim);
        topo.setCorrects(corretos);
    }





    protected class SenderTask extends TimerTask{
        private NekoMessage m;
        public SenderTask(NekoMessage m) {
            this.m = m;
        }

        @Override
        public void run() {
            if (!isCrashed()){
                if (DEBUG) {
                    System.out.println(process.clock() + " " + process.getID() + " s " + m);
                }

                send(this.m);
            }

        }
    }

    class DeliverTask extends TimerTask{
        private NekoMessage m;

        public DeliverTask(NekoMessage m){
            this.m = m;
        }

        @Override
        public void run() {
            if (!isCrashed()){
                if (DEBUG) {
                    System.out.println(process.clock() + " " + process.getID() + " s " + m);
                }

                deliverMessage(m);
            }
        }
    }

    public abstract void deliverMessage(NekoMessage m);

    @Override
    public void deliver(NekoMessage m) {
        NekoSystem.instance().getTimer().schedule(new DeliverTask(m), Parametros.RT);
    }

    public void send(NekoMessage m) {
        if (!isCrashed()){
            broadcast(m);
        }
    }

    public void broadcast(NekoMessage m){
        sender.send(m);
    }

    public class DeliverComparator implements Comparator<AtomicData> {
        @Override
        public int compare(AtomicData o1, AtomicData o2) {
            if (o2.getTimestamp() != o1.getTimestamp())
                return o1.getTimestamp() - o2.getTimestamp();
            else
                return o1.getData().getSrc() - o2.getData().getSrc();
        }
    }

    @Override
    public void statusChange(boolean suspected, int p) {
        if (suspected && topo.getCorrects().contains(p)){
            System.out.println(String.format("p%s: Foi detectado falha do %s no tempo %s", me,p,process.clock()));
            for (int i = 0; i < topo.getCorrects().size(); i++) {
                if (topo.getCorrects().get(i) == p){
                    //System.err.println(String.format("p%s: Processo %s excluído da lista de corretos", me,p));
                    topo.getCorrects().remove(i);
                    break;
                }

            }

        }
    }

}


