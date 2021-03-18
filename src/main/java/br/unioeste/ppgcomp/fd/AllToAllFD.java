package br.unioeste.ppgcomp.fd;

import br.unioeste.ppgcomp.initializers.FailureDetectorInitializer;
import br.unioeste.ppgcomp.topologia.AbstractTopology;
import lse.neko.NekoMessage;
import lse.neko.NekoProcess;
import lse.neko.NekoSystem;
import lse.neko.SenderInterface;

import java.util.ArrayList;
import java.util.List;

public class AllToAllFD extends AbstractFailureDetector{
    private AbstractTopology topo;

    public AllToAllFD(NekoProcess process, SenderInterface sender, String name,AbstractTopology topo) {
        super(process, sender, name);

        this.topo = topo;
    }

    @Override
    public void detector() {
        List<Integer> responses = new ArrayList<>();

        while (process.clock() < simulation_time){
            // Para em caso de falha
            if (isCrashed())
                return;

//            Atraso de propagação
            double delay = DELAY;

            responses.clear();
            for (Integer i : topo.destinations(me)){
                if (states[i] == STATE.FAULT_FREE){
                    responses.add(i);
                    NekoMessage m = new NekoMessage(new int[]{i}, FailureDetectorInitializer.PROTOCOL_NAME, null, ARE_YOU_ALIVE);

                    NekoSystem.instance().getTimer().schedule(new SenderTask(m),delay);
                    delay += DELAY;
                }
            }
            // Checa as respostas
            for (Integer p : responses) {
                checkResponse(p);
            }

//            Aguarda intervalo de testes
            try {
                sleep(INTERVAL);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


}
