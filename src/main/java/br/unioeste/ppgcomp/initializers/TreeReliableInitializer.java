package br.unioeste.ppgcomp.initializers;

import br.unioeste.ppgcomp.broadcast.TreeReliableBroadcast;
import br.unioeste.ppgcomp.fd.VCubeFD;
import br.unioeste.ppgcomp.topologia.VCubeTopology;
import lse.neko.NekoProcess;
import lse.neko.NekoProcessInitializer;
import lse.neko.SenderInterface;
import org.apache.java.util.Configurations;

public class TreeReliableInitializer implements NekoProcessInitializer {

    public static final String PROTOCOL_NAME = "New-hiADSD";
    public static final String PROTOCOL_APP = "Tree-Broadcast";

    public void init(NekoProcess process, Configurations config) throws Exception {
        // Tipo de rede definido nos arquivos de configuração
        SenderInterface sender = process.getDefaultNetwork();

        VCubeTopology topo = new VCubeTopology(process.getN());
        VCubeFD fd = new VCubeFD(process,sender,PROTOCOL_NAME,topo);
        fd.setId(PROTOCOL_NAME);


        TreeReliableBroadcast tree = new TreeReliableBroadcast(process,sender,PROTOCOL_APP);
        tree.setId(PROTOCOL_APP);



        fd.addListener(tree);


        //Inicia execução
        fd.launch();
        tree.launch();





    }
}
