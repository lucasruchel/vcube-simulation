package br.unioeste.ppgcomp.broadcast;

import br.unioeste.ppgcomp.broadcast.core.AbstractBroadcast;
import br.unioeste.ppgcomp.fault.CrashProtocol;
import lse.neko.NekoMessage;
import lse.neko.NekoProcess;
import lse.neko.SenderInterface;
import lse.neko.failureDetectors.FailureDetectorListener;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class LazyReliableBroadcast extends AbstractBroadcast {

    // Lista de processos corretos
    private List<Integer> correct;

    // Lista de mensagens entregues.
    private List<NekoMessage> delivered;

    // Lista de mensagens entregues por processo
    private List<NekoMessage> from[];

    private BestEffortBroadcast bestEffortBroadcast;

    public LazyReliableBroadcast(NekoProcess process, SenderInterface sender, String name) {
        super(process, sender, name);

        // Todos os processos inicialmente estão corretos
        correct = new ArrayList<Integer>(process.getN());
        for (int i = 0; i < process.getN(); i++) {
            correct.add(i,i);
        }

        // Nenhuma mensagem entregue
        delivered = new LinkedList<NekoMessage>();

        // Todos processos começam com nenhuma mensagem entregue
        from = new List[process.getN()];
        for (int i = 0; i < from.length; i++) {
            from[i] = new LinkedList<NekoMessage>();
        }

        // Utiliza BestEffort para envio padrão
        bestEffortBroadcast = new BestEffortBroadcast(process,name);
        bestEffortBroadcast.setSender(sender);
    }

    public void broadcast(NekoMessage m){
        bestEffortBroadcast.broadcast(m);
    }


    @Override
    public void doDeliver(NekoMessage brcast) {
        NekoMessage m = (NekoMessage) brcast.getContent();

        if (!delivered.contains(m)){
            delivered.add(m);
            from[m.getSource()].add(m);
            logger.info(String.format("p%s: Delivered %s", process.getID(),m));
            if (!correct.contains(m.getSource())){
                bestEffortBroadcast.broadcast(m);
            }
        }
    }

    @Override
    public void run() {
        if (process.getID() == 0){
            broadcast(new NekoMessage(process.getID(),new int[]{-1},0,
                    "Mensagem",BestEffortBroadcast.MESSAGE_APP));
        }
    }

    public void statusChange(boolean suspected, int p) {
        if (suspected && correct.contains(p)){
            logger.info(String.format(" -->> ## Detectou falha e fará broadcast se houverem mensagens no tempo %s"));

            correct.remove(p);
            for (NekoMessage m: from[p]) {
                bestEffortBroadcast.broadcast(m);
            }
        } else if (!suspected && !correct.contains(p)){
            correct.add(p);
        }
    }
}
