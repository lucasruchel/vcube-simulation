package br.unioeste.ppgcomp.broadcast;

import br.unioeste.ppgcomp.fd.NewHiADSD;
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
        NewHiADSD fd = new NewHiADSD(process,sender,PROTOCOL_NAME);
        fd.setId(PROTOCOL_NAME);


        LazyReliableBroadcast lazy = new LazyReliableBroadcast(process,sender,PROTOCOL_APP);
        lazy.setId(PROTOCOL_APP);


        fd.addListener(lazy);


        //Inicia execução
        fd.launch();
        lazy.launch();





    }
}
