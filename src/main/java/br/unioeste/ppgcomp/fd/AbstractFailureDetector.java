package br.unioeste.ppgcomp.fd;

import br.unioeste.ppgcomp.fault.CrashProtocol;
import br.unioeste.ppgcomp.initializers.FailureDetectorInitializer;
import br.unioeste.ppgcomp.topologia.AbstractTopology;
import lse.neko.*;
import lse.neko.failureDetectors.FailureDetectorListener;
import lse.neko.util.TimerTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractFailureDetector extends CrashProtocol {
    protected final boolean DEBUG = false;

    protected final double DELAY = 0.1;

    // Identificação da mensagem do protocolo utilizado
    protected static final int I_AM_ALIVE = 1002;
    protected static final int ARE_YOU_ALIVE = 1003;


    // Registrar para facilitar no log
    static {
        MessageTypes.instance().register(I_AM_ALIVE, "I_AM_ALIVE");
        MessageTypes.instance().register(ARE_YOU_ALIVE, "ARE_YOU_ALIVE");
    }


    // Configurações
    protected static final double DEFAULT_TIMEOUT = 5 ;
    protected static final double INTERVAL = 1;

    // Número de processos
    protected int np;

    //private destinos de envio
    protected int[] destinations;

    // Timestamps dos processos
    protected int[] ts;

    // Estado dos processos
    protected VCubeFD.STATE[] states;

    // ID do processo
    protected int me;

    // Lista de Listeners do detector de falhas
    protected List<FailureDetectorListener> listeners;

//    Falho ou correto
    enum STATE {
        FAULTY,
        FAULT_FREE
    };

    //Fila de mensagens para aguardar resposta
    protected NekoMessageQueue messageQueue[];

    protected double simulation_time;

    public AbstractFailureDetector(NekoProcess process, SenderInterface sender, String name) {
        super(process, sender, name);

        simulation_time = NekoSystem.instance().getConfig().getDouble("simulation.time");

        this.np = process.getN();
        this.me = process.getID();

        // Cria e preenche vetor com valores de timestamps iniciais
        this.ts = new int[np];
        Arrays.fill(this.ts,-1);

        this.destinations = new int[np];

        messageQueue = new NekoMessageQueue[np];

        for (int i = 0; i < this.np; i++) {
            destinations[i] = i;
            messageQueue[i] = new NekoMessageQueue();

        }

        states = new STATE[np];
        Arrays.fill(states,STATE.FAULT_FREE);

        // Inicializa o objeto da lista de listeners
        listeners = new ArrayList<FailureDetectorListener>();
    }


    protected void checkNewer(int[] source, int[] received){
        int len = source.length;

        for (int i = 0; i < len; i++) {
            if (source[i] < received[i]){
                source[i] = received[i];
            }
        }
    }


    protected void suspect(int p){
        for (FailureDetectorListener listener: listeners){
            listener.statusChange(true,p);
        }
    }
    protected void unsuspect(int p){
        for (FailureDetectorListener listener: listeners){
            listener.statusChange(false,p);
        }
    }


    public void addListener(FailureDetectorListener fl){
        if (listeners != null) {
            listeners.add(fl);
        }
    }

    public void removeListener(FailureDetectorListener fl){
        if (listeners != null) {
            listeners.remove(fl);
        }
    }

    /***
     * Método utilizado para implementação do detector de falhas.
     */
    public abstract void detector();

    @Override
    public final void run() {
        detector();
    }

    protected void checkResponse(int j){
        NekoMessage m = messageQueue[j].get(DEFAULT_TIMEOUT);
        if(isCrashed()){
            return;
        }

        // Falha do process, não respondeu
        if (m == null){
            if (DEBUG)
                logger.fine(String.format("-------->>   processo %s falho !!!!!", j));
            STATE before = states[j];
            states[j] = STATE.FAULTY;

            if(ts[j] == -1)
                ts[j] = 0;

            ts[j]++;

            // Suspeita de processo e avisa listeners

            if (before == STATE.FAULT_FREE)
                suspect(j);

        } else {
            int source = m.getSource();

            if (states[source] == STATE.FAULTY){
                states[source] = STATE.FAULT_FREE;
                //    ts[source]++;
                unsuspect(source);
            }

            //Atualiza estado
            if (ts[source] == -1){
                ts[source] = 0;
            }

            // Recupera objeto enviado
            int [] data = (int[]) m.getContent();

            // Atualiza timestamps
            for (int i = 0; i < np; i++) {
                //  if (i != me && i != source){

                // Atualiza timestamps
                if (ts[i] < data[i])
                    ts[i] = data[i];

                // Atualiza estado, caso processo esteja faltoso
                if (ts[i] % 2 == 1){
                    STATE before = states[i];
                    states[i] = STATE.FAULTY;
                    if (before == STATE.FAULT_FREE)
                        suspect(i);
                } else {
                    states[i] = STATE.FAULT_FREE;
                    unsuspect(i);
                }
                // }
            }
        }
    }

    @Override
    public void deliver(NekoMessage m) {
        deliverMessage(m);

        super.deliver(m);
    }

    protected class SenderTask extends TimerTask {
        private NekoMessage m;
        public SenderTask(NekoMessage m) {
            this.m = m;
        }

        @Override
        public void run() {
            if (!isCrashed()){
                if (DEBUG) {
                    System.out.println(process.clock() + " " + process.getID() + " s " + m);
                }

               sender.send(m);
            }

        }
    }

    class DeliverTask extends TimerTask{
        private NekoMessage m;

        public DeliverTask(NekoMessage m){
            this.m = m;
        }

        @Override
        public void run() {
            if (!isCrashed()){
                if (DEBUG) {
                    System.out.println(process.clock() + " " + process.getID() + " s " + m);
                }

                deliverMessage(m);
            }
        }
    }

    public void deliverMessage(NekoMessage m){
        if (isCrashed())
            return;

        if (m.getType() == ARE_YOU_ALIVE){
            int source = m.getSource();

            if (ts[source] == -1){
                ts[source] = 0;
            }

            if (ts[me] == -1)
                ts[me] = 0;

            if (states[source] == STATE.FAULTY){
                states[source] = STATE.FAULT_FREE;
                ts[source]++;

                // Informa listeners de que processo está vivo
                unsuspect(source);
            }

            // Envia resposta para o processo que enviou a mensagem com uma cópia do vetor de timestamps
            NekoMessage m1 = new NekoMessage(me, new int[]{m.getSource()}, FailureDetectorInitializer.PROTOCOL_NAME,ts.clone(), I_AM_ALIVE);
            NekoSystem.instance().getTimer().schedule(new SenderTask(m1), DELAY);



        } else if (m.getType() == I_AM_ALIVE){
            if (states[m.getSource()] == STATE.FAULTY){
                states[m.getSource()] = STATE.FAULT_FREE;
                ts[m.getSource()]++;

                int[] data = (int[]) m.getContent();
                checkNewer(ts,data);
                unsuspect(m.getSource());
            } else {
                // Processo está correto
                messageQueue[m.getSource()].put(m);
            }
        }
    }
}


