package br.unioeste.ppgcomp.fd;


import br.unioeste.ppgcomp.fault.CrashProtocol;
import lse.neko.*;
import lse.neko.failureDetectors.FailureDetectorListener;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NewHiADSD  extends CrashProtocol {
    // Configurações
    protected static final double DEFAULT_TIMEOUT = 4 ;
    protected static final double INTERVAL = 1;

    // Número de processos
    private int np;

    //private destinos de envio
    private int[] destinations;

    // Timestamps dos processos
    private int[] ts;

    // Estado dos processos
    private STATE[] states;

    // ID do processo
    private int me;

    // Identificação da mensagem do protocolo utilizado
    private static final int I_AM_ALIVE = 1002;
    private static final int ARE_YOU_ALIVE = 1003;

    // Lista de Listeners do detector de falhas
    private List<FailureDetectorListener> listeners;

    enum STATE {
        FAULTY,
        FAULT_FREE
    };
    // Registrar para facilitar no log
    static {
        MessageTypes.instance().register(I_AM_ALIVE, "I_AM_ALIVE");
        MessageTypes.instance().register(ARE_YOU_ALIVE, "ARE_YOU_ALIVE");
    }

    //Fila de mensagens para aguardar resposta
    NekoMessageQueue messageQueue[];

    public NewHiADSD(NekoProcess process, SenderInterface sender, String name) {
        super(process,sender,name);

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

    public void cis(List<Integer> elements, int id, int cluster){
        // Primeiro elemento do cluster
        int xor = id ^ (int)(Math.pow(2, cluster - 1));

        // Adiciona elemento ao cluster
        elements.add(xor);

        for (int i = 1; i <= cluster - 1; i++) {
            cis(elements,xor,i);
        }

    }



    private void checkNewer(int[] source, int[] received){
        int len = source.length;

        for (int i = 0; i < len; i++) {
            if (source[i] < received[i]){
                source[i] = received[i];
            }
        }
    }
    @Override
    public void deliver(NekoMessage m) {
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
            send(m1);


        } else if (m.getType() == I_AM_ALIVE){

            if (states[m.getSource()] == STATE.FAULTY){
                states[m.getSource()] = STATE.FAULT_FREE;
                ts[m.getSource()]++;

                int[] data = (int[]) m.getContent();
                checkNewer(ts,data);
                unsuspect(m.getSource());
            } else {
                messageQueue[m.getSource()].put(m);
                //System.out.printf("p%s: Colocado na fila \n",me);
            }
        }


        super.deliver(m);




    }

    private void suspect(int p){
        for (FailureDetectorListener listener: listeners){
            listener.statusChange(true,p);
        }
    }
    private void unsuspect(int p){
        for (FailureDetectorListener listener: listeners){
            listener.statusChange(false,p);
        }
    }

    private int log2(int v){
        return (int) (Math.log(v)/Math.log(2));
    }

    /***
     *
     * @param i nó a ser verificado
     * @param s cluster que será utilizado
     * @return -1 se não encontrar nenhum nó livre de falhas; o número do processo vizinho do nó i
     */

    public int ff_neighboor(int i, int s){
        List<Integer> elements = new ArrayList<Integer>();

        cis(elements, i, s);

        // procura primeiro elemento no cluster J que seja livre de falhas
        int n = 0;
        do {
            if (states[elements.get(n)] == STATE.FAULT_FREE)
                return elements.get(n);
            n++;
        } while (n < elements.size());

        // Nenhum vizinho sem falha encontrado
        return -1;
    }

    /***
     *
     * @param p nó do hipercube em que será obtida a vizinhança
     * @param h quantidade de clusters do hipercubo que será obtido os processos diretamente conectados
     * @return Lista de inteiros com o vizinhos
     */
    public List<Integer> neighborhood(int p, int h){
        List<Integer> elements = new ArrayList<Integer>();

        // Verifica todos os cluster de 1 até h
        for (int i = 1; i <= h; i++) {
            int e = ff_neighboor(p,i);
            elements.add(e);
        }

        return elements;
    }

    @Override
    public void run() {
        double simulation_time = NekoSystem.instance().getConfig().getDouble("simulation.time");

        List<Integer> elementsI = new ArrayList<Integer>();
        List<Integer> responses = new ArrayList<Integer>();


        // Loop de repetição do relógio
        while (process.clock() < simulation_time) {
            if (isCrashed())
                return;

            responses.clear();
            // de  1 até o número de dimensões do hipercubo
            for (int i = 1; i <= log2(np); i++) {
                elementsI.clear();
                cis(elementsI, me, i);
                for (int j : elementsI) {
                    // Verifica i é o primeiro elemento livre de falha
                    if (ff_neighboor(j,i) == me && states[j] == STATE.FAULT_FREE) {
                        responses.add(j);
                        NekoMessage m = new NekoMessage(new int[]{j}, FailureDetectorInitializer.PROTOCOL_NAME, null, ARE_YOU_ALIVE);
                        send(m);

                    }
                }
            }
            // Checa as respostas
            for (Integer p : responses) {
                checkResponse(p);
            }

            // Exibe timestamps resultantes
            //logger.info(String.format("P%s: No tempo %s, ts = %s", me,process.clock(),Arrays.toString(ts)));

            try {
                sleep(INTERVAL);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }


    private void checkResponse(int j){
        NekoMessage m = messageQueue[j].get(DEFAULT_TIMEOUT);
        if(isCrashed()){
            return;
        }


        // Falha do process, não respondeu
        if (m == null){
            logger.fine(String.format("-------->>   processo %s falho !!!!!", j));
            states[j] = STATE.FAULTY;

            if(ts[j] == -1)
                ts[j] = 0;

            ts[j]++;

            // Suspeita de processo e avisa listeners
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
                        states[i] = STATE.FAULTY;
                        suspect(i);
                    } else {
                        states[i] = STATE.FAULT_FREE;
                        unsuspect(i);
                    }
               // }
            }
        }
    }

    public int cluster(int i, int j){
        return MSB(i,j) + 1;
    }

    public int MSB(int i, int j) {
        int s = 0;
        for (int k = i ^ j; k > 0; k = k >> 1) {
            s++;
        }
        return --s;
    }
}
