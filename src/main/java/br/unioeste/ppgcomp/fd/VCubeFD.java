package br.unioeste.ppgcomp.fd;


import br.unioeste.ppgcomp.fault.CrashProtocol;
import br.unioeste.ppgcomp.initializers.FailureDetectorInitializer;
import br.unioeste.ppgcomp.topologia.VCubeTopology;
import lse.neko.*;
import lse.neko.failureDetectors.FailureDetectorListener;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VCubeFD extends AbstractFailureDetector {

    private VCubeTopology topology;

    public VCubeFD(NekoProcess process, SenderInterface sender, String name, VCubeTopology topology) {
        super(process,sender,name);

        this.topology = topology;
    }

    @Override
    public void detector() {
        List<Integer> elementsI = new ArrayList<Integer>();
        List<Integer> responses = new ArrayList<Integer>();

        // Loop de repetição do relógio
        while (process.clock() < simulation_time) {
            if (isCrashed())
                return;

            responses.clear();
            //Atraso de propagação
            double delay = DELAY;

            // de  1 até o número de dimensões do hipercubo
            for (int i = 1; i <= topology.log2(np); i++) {
                elementsI.clear();
                topology.cis(elementsI, me, i);
                for (int j : elementsI) {
                    // Verifica i é o primeiro elemento livre de falha
                    if (topology.ff_neighboor(j,i) == me && states[j] == STATE.FAULT_FREE) {
                        responses.add(j);
                        NekoMessage m = new NekoMessage(new int[]{j}, FailureDetectorInitializer.PROTOCOL_NAME, null, ARE_YOU_ALIVE);

                        NekoSystem.instance().getTimer().schedule(new SenderTask(m),delay);
                        delay += DELAY;
                    }
                }
            }
            // Checa as respostas
            for (Integer p : responses) {
                checkResponse(p);
            }

            // Exibe timestamps resultantes
            //logger.info(String.format("P%s: No tempo %s, ts = %s", me,process.clock(),Arrays.toString(ts)));

            try {
                sleep(INTERVAL);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
