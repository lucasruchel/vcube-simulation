package br.unioeste.ppgcomp.initializers;

import br.unioeste.ppgcomp.fd.NewHiADSD;
import lse.neko.NekoProcess;
import lse.neko.NekoProcessInitializer;
import lse.neko.SenderInterface;
import org.apache.java.util.Configurations;

public class FailureDetectorInitializer implements NekoProcessInitializer {

    public static final String PROTOCOL_NAME = "New-hiADSD";

    public void init(NekoProcess process, Configurations config) throws Exception {
        // Tipo de rede definido nos arquivos de configuração
        SenderInterface sender = process.getDefaultNetwork();
        NewHiADSD fd = new NewHiADSD(process,sender,PROTOCOL_NAME);
        fd.setId(PROTOCOL_NAME);




        //Inicia execução
        fd.launch();



    }
}
