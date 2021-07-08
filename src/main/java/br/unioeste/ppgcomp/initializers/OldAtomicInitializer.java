package br.unioeste.ppgcomp.initializers;

import br.unioeste.ppgcomp.broadcast.OldAtomicBroadcast;
import br.unioeste.ppgcomp.broadcast.core.AbstractBroadcast;
import br.unioeste.ppgcomp.data.BroadcastMessage;
import br.unioeste.ppgcomp.data.Data;
import br.unioeste.ppgcomp.fd.VCubeFD;
import br.unioeste.ppgcomp.topologia.AbstractTopology;
import br.unioeste.ppgcomp.topologia.VCubeTopology;
import lse.neko.NekoProcess;
import lse.neko.NekoProcessInitializer;
import lse.neko.SenderInterface;
import org.apache.java.util.Configurations;

public class OldAtomicInitializer implements NekoProcessInitializer {

    public static final String PROTOCOL_NAME = "New-hiADSD";
    public static final String PROTOCOL_APP = "Atomic-Broadcast";

    public void init(NekoProcess process, Configurations config) throws Exception {
        // Tipo de rede definido nos arquivos de configuração
        SenderInterface sender = process.getDefaultNetwork();

        VCubeTopology topo = new VCubeTopology(process.getN());
        VCubeFD fd = new VCubeFD(process,sender,PROTOCOL_NAME,topo);

        fd.setId(PROTOCOL_NAME);

//        Topologia inicializada com número de processos no sistema
        AbstractTopology topology = new VCubeTopology(process.getN());


        OldAtomicBroadcast atomic = new OldAtomicBroadcast(process,sender,PROTOCOL_APP,topology);
        atomic.setId(PROTOCOL_APP);
        atomic.addDataListener(new AbstractBroadcast.DataListener<String>() {
            int exec = 1;
            @Override
            public void deliver(BroadcastMessage<String> data) {
                int id = process.getID();

//                if (exec <= 1000 && (id == 0 || id == 1))
                if (exec < 100000 && data.getSrc() == id){
                    String m = String.format("p%s:exec-%d",id,++exec);
                    atomic.broadcast_tree(new Data(id,m));
                }

            }
        });


        fd.addListener(atomic);


        //Inicia execução
//        fd.launch();
        atomic.launch();





    }
}
