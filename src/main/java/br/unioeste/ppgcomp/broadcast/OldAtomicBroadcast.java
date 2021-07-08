package br.unioeste.ppgcomp.broadcast;

import br.unioeste.ppgcomp.broadcast.core.AbstractBroadcast;
import br.unioeste.ppgcomp.data.*;
import br.unioeste.ppgcomp.topologia.AbstractTopology;
import lse.neko.*;
import lse.neko.util.TimerTask;

import java.util.*;

public class OldAtomicBroadcast extends AbstractBroadcast {


    // Conjunto de mensagens marcadas
    private Map<Data,Integer> stamped;

    private Map<Data,Set<Timestamp>> received;

    private TreeSet<ACKMessage> pendingACK;

    private List<Data> last_i;


    // Conjunto de mensagens que são marcadas para entrega, mas que precisam de ordenação
    private TreeSet<AtomicData> delivered;

    private List<Data> dataset;

    // Marcador de tempo local
    private int lc;

    // IDs das mensagens utilizadas
    private static final int TREE = 1301;
    private static final int FWD = 1302;
    private static final int ACK = 1303;
    private static final int RPL = 1304;

    static{
        MessageTypes.instance().register(TREE,"TREE");
        MessageTypes.instance().register(RPL,"RPL");
        MessageTypes.instance().register(FWD,"FWD");
        MessageTypes.instance().register(ACK,"ACK");

    }

    public OldAtomicBroadcast(NekoProcess process, SenderInterface sender, String name, AbstractTopology topo) {
        super(process, sender, name);

        stamped = new HashMap<>();
        received = new HashMap<>();
        delivered = new TreeSet<>();
        pendingACK = new TreeSet<>();
        dataset = new ArrayList<>();

        this.lc = 0;
        this.topo = topo;

        last_i = new ArrayList<>();
        for (int i=0; i < np; i++){
            last_i.add(null);
        }



    }

       public void broadcast_tree(Data m){


        TreeSet<Timestamp> timestamps = new TreeSet<>();
        timestamps.add(new Timestamp(me,lc));

        // Encaminha para subárvore
        forward(me,me,m,timestamps,TREE);

        received.put(m,timestamps);

        dataset.add(m);


        // Incrementa contador
        lc++;
    }

    public void replay(int source, int from, int id, Data data, TreeSet<Timestamp> tsaggr, int type ){
        AtomicMessage treeMessage = new AtomicMessage(source,tsaggr,data);

        NekoMessage m = new NekoMessage(from,new int[]{id},getId(),treeMessage,type);
        NekoSystem.instance().getTimer().schedule(new SenderTask(m), DELAY);

        pendingACK.add(new ACKMessage(id,data, from,source));
    }

    public void forward(int source, int from, Data data, TreeSet<Timestamp> tsaggr,int type){
        // Utiliza overlay do vcube para definir destinos em uma topologia de árvore
        List<Integer> destinos = topo.destinations(me,from);

        double delay = DELAY;
        for (int p : destinos){
            AtomicMessage treeMessage = new AtomicMessage(source,tsaggr,data);

            NekoMessage m = new NekoMessage(from,new int[]{p},getId(),treeMessage,type);
            NekoSystem.instance().getTimer().schedule(new SenderTask(m), delay);

            delay += DELAY;

            pendingACK.add(new ACKMessage(p,data, from,source));
        }
    }
    public boolean contains(TreeSet<AtomicData> list,Data o){
        for (AtomicData t : list){
            if (t.getData().equals(o))
                return true;
        }
        return false;
    }


    @Override
    public void deliverMessage(NekoMessage m) {
        if (isCrashed())
            return;

        // Processa ACK
        if (m.getType() == ACK){
            ACKMessage ack = (ACKMessage) m.getContent();
            int source = m.getSource();

//            ACKMessage ack = null;
//            for (ACKMessage pending : pendingACK){
//                if (pending.getRoot() == receivedAck.getRoot() &&
//                        pending.getId() == source &&
//                        pending.getData().equals(receivedAck.getData())
//                    ){
//                    ack = pending;
//                    break;
//                }
//            }
//            if (ack != null)
                pendingACK.remove(ack);

//            if (ack != null && ack.getSource() != me)
            if (ack.getSource() != me)
                checkAcks(ack.getSource(), ack.getData(),ack.getRoot());

            return;
        }


        //Dados da mensagem
        AtomicMessage data = (AtomicMessage) m.getContent();



        this.lc = Math.max(lc + 1,data.getMaxTimestamp());

        TreeSet<Timestamp> tsaggr = new TreeSet<>(data.getTsaggr());

        // Verifica processos vizinhos que pertencem a raiz da mensagem
        List<Integer> subtree_src = topo.destinations(me, m.getSource());

         if (!dataset.contains(data.getData())) {
            dataset.add(data.getData());

            TreeSet<Timestamp> tsi = new TreeSet<>();
            Timestamp ts = new Timestamp(me,lc);

            tsaggr.add(ts);
            tsi.add(ts);

            double delay = DELAY;
            for (int i : topo.destinations(me)) {
                if (!subtree_src.contains(i)) {

                    AtomicMessage tree = new AtomicMessage(me, tsi, data.getData());
                    NekoMessage m1 = new NekoMessage(me, new int[]{i}, getId(), tree, RPL);

                    NekoSystem.instance().getTimer().schedule(new SenderTask(m1), delay);

                    delay += DELAY;

                    pendingACK.add(new ACKMessage(i,data.getData(), me,me));
                }
            }
        }


        forward(data.getSource(),m.getSource(),data.getData(),tsaggr,FWD);
//        for (int j : subtree_src){
//            // Para recuperar confirmações de entrega
//            pendingACK.add(new ACKMessage(j,data.getData(), m.getSource(),data.getSource()));
//        }

//        Verifica se a mensagem já foi entregue, sem processamento desnecessario se já houver sido
        if (!contains(delivered, data.getData()) && stamped.get(data.getData()) == null)
            if (!received.containsKey(data.getData()))
                received.put(data.getData(),new TreeSet<>(tsaggr));
            else
                received.get(data.getData()).addAll(tsaggr);






        if (data.getSource() != me)
            checkAcks(m.getSource(),data.getData(),data.getSource());

//        System.out.println(String.format("p%s: Received de %s processos, clock=%s", me,received.size(),process.clock()));

        if (isReceivedFromAll(received.get(data.getData()))){
            int sm = max(received.get(data.getData()));

            doDeliver(data.getData(),sm);
        }
    }

    public void checkAcks(int src, Data data, int root){
        for (ACKMessage pending: pendingACK){
            if (pending.getRoot() == root  && pending.getData().equals(data)){
                return;
            }
        }

        ACKMessage ack = new ACKMessage(me,data,src, root);

        NekoMessage mr = new NekoMessage(new int[]{src},getId(),ack,ACK);

        NekoSystem.instance().getTimer().schedule(new SenderTask(mr),DELAY);

    }

    public Timestamp getTsOfMe(Set<Timestamp> timestamps){
        if (timestamps == null || timestamps.size() == 0) {
            return null;
        }

        for (Timestamp ts : timestamps)
            if (ts.getId() == me){
                return ts;
            }

        return null;
    }

    private void doDeliver(Data data,int sm){
   //     System.out.println(received.toString());


        if (data != null) {
            stamped.put(data, sm);
        }

        received.remove(data);


//        Ordenar lista de mensagens marcadas.

        ArrayList<AtomicData> deliverList = new ArrayList<>();
        for (Map.Entry<Data,Integer> m_ : stamped.entrySet()) {
            boolean menor = true;

            for (Map.Entry<Data,Set<Timestamp>> m__ : received.entrySet()) {
                if (m_.getValue() > max(m__.getValue()) || (m_.getValue() == (max(m__.getValue())) && m_.getKey().getSrc() > m__.getKey().getSrc())) {
                    menor = false;
                }
            }

            if (menor){
                deliverList.add(new AtomicData(m_.getKey(),m_.getValue()));
            }
        }

        for (AtomicData atomic : deliverList){
            stamped.remove(atomic.getData());
        }






        deliverList.sort(new DeliverComparator());
        for (AtomicData m_ : deliverList) {
            delivered.add(m_);

            Data d = m_.getData();
            publish(new BroadcastMessage(d.getData(),d.getSrc(),-1));
//            if (DEBUG)
                logger.info(String.format("p%s: delivered %s", me, m_.toString()) );
        }
    }
    private int max(Set<Timestamp> set){
        int max = 0;

        for (Timestamp t : set){
            if (t.getTs() > max)
                max = t.getTs();
        }

        return max;
    }
    private boolean isReceivedFromAll(Set<Timestamp> timestamps){
        if (timestamps == null) {
            return false;
        }

        int counter = timestamps.size();

        // Se contador for menor significa que nem todos receberam a mensagem
        return counter >= topo.getCorrects().size();
    }

    @Override
    public synchronized void statusChange(boolean suspected, int p) {
        super.statusChange(suspected,p);

        if (isCrashed())
            return;

        if (suspected && isPending(p) ){
            TreeSet<ACKMessage> removeAcks = new TreeSet<>();

            // Recupera ack pendente de p
            for (ACKMessage ack : pendingACK){
               if (ack.getId() == p) {
                   // Registro de ACKs pendentes que devem ser removidos e reprocessados
                   removeAcks.add(ack);

                   // Remove timestamp de p
                   Timestamp ts = null;
                   if (received.containsKey(ack.getData()))
                       for (Timestamp t : received.get(ack.getData())) {
                           if (t.getId() == p) {
                               ts = t;
                               break;
                           }
                       }
                   if (ts != null)
                       received.get(ack.getData()).remove(ts);
               }}
                pendingACK.removeAll(removeAcks);
                 for (ACKMessage ack  : removeAcks){
                     // próximo vizinho sem falha
                     int next = topo.nextNeighboor(me,ack.getId());

                    // Reencaminha (FWD) mensagem de p
                    // Necessário identificar os Ts que agregariam a mensagem de p
                    if (next >= 0){
                        List<Integer> pais = topo.fathers(me,ack.getRoot());


                        TreeSet<Timestamp> tsaggr = new TreeSet<>();
                        // Verifica se dados foram entregues ou marcados
                        if (contains(delivered,ack.getData())){
                            AtomicData deliveredData = null;
                            for (AtomicData d : delivered){
                                if (d.getData().equals(ack.getData())) {
                                    deliveredData = d;
                                    break;
                                }
                            }
                            tsaggr.add(new Timestamp(me,deliveredData.getTimestamp()));
                        } else if (stamped.containsKey(ack.getData())){
                            int ts = 0;
                            for (Map.Entry<Data,Integer> d : stamped.entrySet()){
                                if (d.getKey().equals(ack.getData())) {
                                    ts = d.getValue();
                                    break;
                                }
                            }
                            tsaggr.add(new Timestamp(me,ts));
                        } else {
                            tsaggr = getTsOfProcesses(ack.getData(), pais);
                        }


                        if (ack.getRoot() == me)
                            replay(me, ack.getSource(), next, ack.getData(),tsaggr,RPL);
                        else
                            replay(ack.getRoot(), ack.getSource(), next, ack.getData(),tsaggr,RPL);

                        //newPendings.add(new ACKMessage(next,ack.getData(), ack.getSource(),ack.getRoot()));
                        System.out.println("Reenviando mensagem para " + next);
                    }

                    // Para evitar que envie confirmação para ele mesmo
                    if ( ack.getRoot() != me)
                        checkAcks(ack.getSource(),ack.getData(),ack.getRoot());

                    System.out.printf("P%s: falha de %s - pendente em %s da árvore de %s -- próximo sem falha %s \n ",me,p,me,ack.getRoot(),next);
                }


            }

            // Recupera dados pendentes
            HashSet<Data> pendingData = new LinkedHashSet<>();
            for (Map.Entry<Data, Set<Timestamp>> entry : received.entrySet()){
                pendingData.add(entry.getKey());
            }
            // Para cada mensagem pendente tenta fazer o deliver
            for (Data data : pendingData) {
                if (isReceivedFromAll(received.get(data))) {
                    int sm = max(received.get(data));

                    doDeliver(data, sm);
                }
            }
        }




    // Função que retorna os Timestamps dos processos identificados em p
    public TreeSet<Timestamp> getTsOfProcesses(Data data, List<Integer> p){
        TreeSet<Timestamp> stree = new TreeSet<>();

        if (!received.containsKey(data)){
            return stree;
        }

        for (Timestamp ts : received.get(data)){
            for (int i : p){
                if (ts.getId() == i ){
                    stree.add(ts);
                }
            }
            if (stree.size() == p.size())
                break;
        }

        return stree;
    }



    // Verifica se o processo P possui ACKs pendentes
    public boolean isPending(int p){
        for (ACKMessage ack : pendingACK){
            if (ack.getId() == p)
                return true;
        }
        return false;

    }

    @Override
    public void run() {

//        if (me == 0){
            NekoSystem.instance().getTimer().schedule(new TimerTask() {
                @Override
                public void run() {
                    String message = String.format("p%s:exec-%d",me,1);

                    Data m = new Data(me,message);

                    broadcast_tree(m);
                }
            }, 400);
//        }

    }


}
