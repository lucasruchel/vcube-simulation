package br.unioeste.ppgcomp.broadcast;

import br.unioeste.ppgcomp.fd.NewHiADSD;
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
        NewHiADSD fd = new NewHiADSD(process,sender,PROTOCOL_NAME);
        fd.setId(PROTOCOL_NAME);


        TreeReliableBroadcast tree = new TreeReliableBroadcast(process,sender,PROTOCOL_APP);
        tree.setId(PROTOCOL_APP);



        fd.addListener(tree);


        //Inicia execução
        fd.launch();
        tree.launch();





    }
}
