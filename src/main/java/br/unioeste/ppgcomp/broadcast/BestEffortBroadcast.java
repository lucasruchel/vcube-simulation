package br.unioeste.ppgcomp.broadcast;

import lse.neko.*;
import lse.neko.util.logging.NekoLogger;

import java.util.logging.Logger;

public class BestEffortBroadcast extends ActiveReceiver {

    private SenderInterface sender;

    public final static int MESSAGE_BROADCAST = 1023;
    public final static int MESSAGE_APP = 1024;

    private Logger logger = NekoLogger.getLogger("BROADCAST");

    private int me;

    public BestEffortBroadcast(NekoProcess process, String name) {
        super(process, name);

        me = process.getID();
    }

    static {
        MessageTypes.instance().register(MESSAGE_BROADCAST,"BROADCAST");
        MessageTypes.instance().register(MESSAGE_APP,"APP");
    }

    public void setSender(SenderInterface sender){
        this.sender = sender;
    }

    public void broadcast(NekoMessage m){
        // Envia mensagem à todos os processos
        for (int i = 0; i < process.getN(); i++) {
            int dest[] = {i};
            sender.send(new NekoMessage(dest,LazyReliableInitializer.PROTOCOL_APP,m,MESSAGE_BROADCAST));
        }
    }

    @Override
    public void deliver(NekoMessage m) {
        super.deliver(m);

        logger.info(String.format("Mensagem do processo %s bebDelivered!", m.getSource()));
    }
}
