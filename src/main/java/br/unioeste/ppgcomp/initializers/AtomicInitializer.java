package br.unioeste.ppgcomp.initializers;

import br.unioeste.ppgcomp.broadcast.AtomicBroadcast;
import br.unioeste.ppgcomp.broadcast.core.AbstractBroadcast;
import br.unioeste.ppgcomp.data.BroadcastMessage;
import br.unioeste.ppgcomp.fd.VCubeFD;
import br.unioeste.ppgcomp.topologia.AbstractTopology;
import br.unioeste.ppgcomp.topologia.VCubeTopology;
import lse.neko.NekoProcess;
import lse.neko.NekoProcessInitializer;
import lse.neko.NekoSystem;
import lse.neko.SenderInterface;
import lse.neko.util.TimerTask;
import lse.neko.util.logging.NekoLogger;
import org.apache.java.util.Configurations;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public class AtomicInitializer implements NekoProcessInitializer {

    public static final String PROTOCOL_NAME = "New-hiADSD";
    public static final String PROTOCOL_APP = "New-Broadcast";

    private int counter = 0;
    private int clients = 256;

    private Set<String> messages;


    public void init(NekoProcess process, Configurations config) throws Exception {
        // Cada processa envia um conjunto de mensagens
        int executions = config.getInteger("messages.number",1);

        messages = new HashSet<>();

        // Tipo de rede definido nos arquivos de configuração
        SenderInterface sender = process.getDefaultNetwork();

        VCubeTopology topo = new VCubeTopology(process.getN());
        VCubeFD fd = new VCubeFD(process,sender,PROTOCOL_NAME,topo);

        fd.setId(PROTOCOL_NAME);

//        Topologia inicializada com número de processos no sistema
        AbstractTopology topology = new VCubeTopology(process.getN());
        Logger logger = NekoLogger.getLogger("messages");


        AtomicBroadcast<String> atomic = new AtomicBroadcast(process,sender,PROTOCOL_APP,topology);
        atomic.setId(PROTOCOL_APP);

        int id = process.getID();

        atomic.addDataListener(new AbstractBroadcast.DataListener<String>() {
            int exec = 1;
            @Override
            public void deliver(BroadcastMessage<String> data) {

                logger.info(String.format("p%s:entregue:%s at %s",id, data.getData(),process.clock()) );


                if (messages.contains(data.getData())){
                    messages.remove(data.getData());
                }
                //                Ainda existem mensagens para serem enviadas por cada processo
//                E as mensagens anteriores enviadas já foram entregues.
//                if (process.getID() == 6)
                if (counter < executions && messages.size() == 0) {
                    for (int i = 0; i < clients; i++) {
                        counter++;
                        String raw = String.format("p%d:exec-%d-cli-%s", id, counter, i);
                        messages.add(raw);
                        atomic.broadcast(raw);
                    }

                }
            }
        });

//        if (process.getID() == 1)
        NekoSystem.instance().getTimer().schedule(new TimerTask() {
            @Override
            public void run() {
//                logger.info("starting-experiment");
                counter++;
                for (int i=0; i < clients; i++){
                    String data = String.format("p%d:exec-%d-cli-%s",id,counter,i);
                    messages.add(data);
                    atomic.broadcast(data);
                }


            }
        }, 200);

        fd.addListener(atomic);
        fd.launch();
        atomic.launch();





    }
}
