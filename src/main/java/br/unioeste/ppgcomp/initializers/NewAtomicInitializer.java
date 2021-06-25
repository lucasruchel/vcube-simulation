package br.unioeste.ppgcomp.initializers;

import br.unioeste.ppgcomp.broadcast.AtomicBroadcast;
import br.unioeste.ppgcomp.broadcast.NewAtomicBroadcast;
import br.unioeste.ppgcomp.broadcast.core.AbstractBroadcast;
import br.unioeste.ppgcomp.data.BroadcastMessage;
import br.unioeste.ppgcomp.data.Data;
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

import java.util.logging.Logger;

public class NewAtomicInitializer implements NekoProcessInitializer {

    public static final String PROTOCOL_NAME = "New-hiADSD";
    public static final String PROTOCOL_APP = "New-Broadcast";

    public void init(NekoProcess process, Configurations config) throws Exception {
        int messages = config.getInteger("messages.number",1);
        // Cada processa envia um conjunto de mensagens
        int executions = messages / process.getN();


        // Tipo de rede definido nos arquivos de configuração
        SenderInterface sender = process.getDefaultNetwork();

        VCubeTopology topo = new VCubeTopology(process.getN());
        VCubeFD fd = new VCubeFD(process,sender,PROTOCOL_NAME,topo);

        fd.setId(PROTOCOL_NAME);

//        Topologia inicializada com número de processos no sistema
        AbstractTopology topology = new VCubeTopology(process.getN());
        Logger logger = NekoLogger.getLogger("messages");


        NewAtomicBroadcast<String> atomic = new NewAtomicBroadcast(process,sender,PROTOCOL_APP,topology);
        atomic.setId(PROTOCOL_APP);
        atomic.addDataListener(new AbstractBroadcast.DataListener<String>() {
            int exec = 1;
            @Override
            public void deliver(BroadcastMessage<String> data) {
                int id = process.getID();

                logger.info(String.format("p%d: data=%s",id,data.getData()));

//                if (exec <= 0 && id == 0 ){
//                if (exec <= 1000 && (id == 0 || id == 1))
                if (exec < executions && data.getSrc() == id){
                    String m = String.format("p%s:exec-%d",id,++exec);
                    atomic.broadcast(m);
                } else if (exec >= executions && data.getSrc() == id){
                    logger.info("Finished-experiment");
                }

            }
        });

//        if (process.getID() == 0)
        NekoSystem.instance().getTimer().schedule(new TimerTask() {
                @Override
                public void run() {
                    logger.info("starting-experiment");
                    atomic.broadcast(String.format("p%s:exec-%d",process.getID(),1));
                }
            }, 200);



        fd.addListener(atomic);


        //Inicia execução
        fd.launch();
        atomic.launch();





    }
}