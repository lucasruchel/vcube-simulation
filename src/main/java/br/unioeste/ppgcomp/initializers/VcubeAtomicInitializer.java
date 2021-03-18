package br.unioeste.ppgcomp.initializers;

import br.unioeste.ppgcomp.broadcast.AtomicBroadcast;
import br.unioeste.ppgcomp.fd.VCubeFD;
import br.unioeste.ppgcomp.topologia.AbstractTopology;
import br.unioeste.ppgcomp.topologia.VCubeTopology;
import lse.neko.NekoProcess;
import lse.neko.NekoProcessInitializer;
import lse.neko.SenderInterface;
import org.apache.java.util.Configurations;

public class VcubeAtomicInitializer implements NekoProcessInitializer {

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


        AtomicBroadcast atomic = new AtomicBroadcast(process,sender,PROTOCOL_APP,topology);
        atomic.setId(PROTOCOL_APP);



        fd.addListener(atomic);


        //Inicia execução
//        fd.launch();
        atomic.launch();





    }
}
