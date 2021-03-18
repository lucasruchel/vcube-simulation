package br.unioeste.ppgcomp.initializers;

import br.unioeste.ppgcomp.broadcast.LazyReliableBroadcast;
import br.unioeste.ppgcomp.fd.VCubeFD;
import br.unioeste.ppgcomp.topologia.VCubeTopology;
import lse.neko.NekoProcess;
import lse.neko.NekoProcessInitializer;
import lse.neko.SenderInterface;
import org.apache.java.util.Configurations;

public class LazyReliableInitializer implements NekoProcessInitializer {

    public static final String PROTOCOL_NAME = "New-hiADSD";
    public static final String PROTOCOL_APP = "Lazy-Broadcast";

    public void init(NekoProcess process, Configurations config) throws Exception {
        // Tipo de rede definido nos arquivos de configuração
        SenderInterface sender = process.getDefaultNetwork();

        VCubeTopology topo = new VCubeTopology(process.getN());
        VCubeFD fd = new VCubeFD(process,sender,PROTOCOL_NAME,topo);
        fd.setId(PROTOCOL_NAME);


        LazyReliableBroadcast lazy = new LazyReliableBroadcast(process,sender,PROTOCOL_APP);
        lazy.setId(PROTOCOL_APP);


        fd.addListener(lazy);


        //Inicia execução
        //fd.launch();
        lazy.launch();





    }
}
