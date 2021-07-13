package br.unioeste.ppgcomp.broadcast.core;

import br.unioeste.ppgcomp.config.Parametros;
import br.unioeste.ppgcomp.data.AtomicData;
import br.unioeste.ppgcomp.data.BroadcastMessage;
import br.unioeste.ppgcomp.fault.CrashProtocol;
import br.unioeste.ppgcomp.topologia.AbstractTopology;
import br.unioeste.ppgcomp.topologia.VCubeTopology;
import lse.neko.*;
import lse.neko.failureDetectors.FailureDetectorListener;
import lse.neko.util.Timer;
import lse.neko.util.TimerTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public abstract class AbstractBroadcast<D> extends CrashProtocol implements FailureDetectorListener {

    protected boolean DEBUG = false;

    protected Timer timer;

    protected double DELAY = 0.1;

    // Identificador do process
    protected int me;
    // Numero de processos
    protected int np;
    protected List<Integer> corretos;

    protected int dim;

    // Overlay dos processos para encaminhamento em árvore
    protected AbstractTopology topo;
    private List<DataListener<D>> listeners;

    public AbstractBroadcast(NekoProcess process, SenderInterface sender, String name) {
        super(process, sender, name);

        np = process.getN();
        me = process.getID();

        corretos = new ArrayList<Integer>();
        for (int i = 0; i < np; i++) {
            corretos.add(i);
        }

        dim = (int) (Math.log10(np)/Math.log10(2));
        topo = new VCubeTopology(dim);
        topo.setCorrects(corretos);

        this.listeners = new ArrayList<>();

        this.timer = NekoSystem.instance().getTimer();
    }









    @Override
    public void deliver(NekoMessage m) {
        NekoSystem.instance().getTimer().schedule(new DeliverTask(m), Parametros.TR);
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
    protected void publish(BroadcastMessage<D> data){
        for (DataListener l : listeners)
            l.deliver(data);
    }

    public void addDataListener(DataListener listener){
        listeners.add(listener);
    }

    public interface DataListener<D>{
        void deliver(BroadcastMessage<D> data);
    }
}


