package br.unioeste.ppgcomp.initializers;

import br.unioeste.ppgcomp.broadcast.AtomicBroadcast;
import br.unioeste.ppgcomp.fd.NewHiADSD;
import lse.neko.NekoProcess;
import lse.neko.NekoProcessInitializer;
import lse.neko.SenderInterface;
import lse.neko.util.logging.NekoLogger;
import org.apache.java.util.Configurations;

public class AtomicInitializer implements NekoProcessInitializer {

    public static final String PROTOCOL_NAME = "New-hiADSD";
    public static final String PROTOCOL_APP = "Atomic-Broadcast";

    public void init(NekoProcess process, Configurations config) throws Exception {
        // Tipo de rede definido nos arquivos de configuração
        SenderInterface sender = process.getDefaultNetwork();
        NewHiADSD fd = new NewHiADSD(process,sender,PROTOCOL_NAME);

        fd.setId(PROTOCOL_NAME);




        AtomicBroadcast atomic = new AtomicBroadcast(process,sender,PROTOCOL_APP);
        atomic.setId(PROTOCOL_APP);



        fd.addListener(atomic);


        //Inicia execução
        fd.launch();
        atomic.launch();





    }
}
