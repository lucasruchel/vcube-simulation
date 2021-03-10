package br.unioeste.ppgcomp.broadcast;

import br.unioeste.ppgcomp.broadcast.core.AbstractBroadcast;
import br.unioeste.ppgcomp.data.*;
import lse.neko.*;
import lse.neko.util.TimerTask;

import java.util.*;

public class AtomicBroadcast extends AbstractBroadcast {


    // Conjunto de mensagens marcadas
    private Map<Data,Timestamp> stamped;

    private Map<Data,Set<Timestamp>> received;


    // Conjunto de mensagens que são marcadas para entrega, mas que precisam de ordenação
    private TreeSet<AtomicData> delivered;

    private List<Data> dataset;

    // Marcador de tempo local
    private int lc;

    // IDs das mensagens utilizadas
    private static final int TREE = 1301;
    private static final int FWD = 1302;
    private static final int ACK = 1303;

    static{
        MessageTypes.instance().register(TREE,"TREE");
        MessageTypes.instance().register(FWD,"FWD");
        MessageTypes.instance().register(ACK,"ACK");

    }

    public AtomicBroadcast(NekoProcess process, SenderInterface sender, String name) {
        super(process, sender, name);

        stamped = new HashMap<>();
        received = new HashMap<>();
        delivered = new TreeSet<>();
        dataset = new ArrayList<>();

        this.lc = 0;

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

    public void forward(int source, int from, Data data, TreeSet<Timestamp> tsaggr,int type){
        // Utiliza overlay do vcube para definir destinos em uma topologia de árvore
        List<Integer> destinos = vcube.subtree(me,from);

        double delay = DELAY;
        for (int p : destinos){
            AtomicMessage treeMessage = new AtomicMessage(source,tsaggr,data);

            NekoMessage m = new NekoMessage(from,new int[]{p},getId(),treeMessage,type);
            NekoSystem.instance().getTimer().schedule(new SenderTask(m), delay);
            delay += DELAY;
        }
    }
    public boolean contains(TreeSet<AtomicData> list,Data o){
        for (AtomicData t : list){
            if (t.getData() == o)
                return true;
        }
        return false;
    }


    @Override
    public void deliverMessage(NekoMessage m) {
        if (isCrashed())
            return;

        //Dados da mensagem
        AtomicMessage data = (AtomicMessage) m.getContent();

        this.lc = Math.max(lc + 1,data.getMaxTimestamp());

        TreeSet<Timestamp> tsaggr = new TreeSet<>(data.getTsaggr());


        if (!dataset.contains(data.getData())) {
            dataset.add(data.getData());

            TreeSet<Timestamp> tsi = new TreeSet<>();
            Timestamp ts = new Timestamp(me,lc);

            tsaggr.add(ts);
            tsi.add(ts);

            // subree src
            List<Integer> subtree_src = vcube.subtree(me, m.getSource());

            for (int i : vcube.subtree(me,me)) {
                if (!subtree_src.contains(i)) {

                    AtomicMessage ack = new AtomicMessage(me, tsi, data.getData());
                    send(new NekoMessage(me, new int[]{i}, getId(), ack, ACK));
                }
            }
        }


        forward(data.getSource(),m.getSource(),data.getData(),tsaggr,FWD);

//        Verifica se a mensagem já foi entregue, sem processamento desnecessario se já houver sido
        if (!contains(delivered, data.getData()) && stamped.get(data.getData()) == null)
            if (!received.containsKey(data.getData()))
                received.put(data.getData(),new TreeSet<>(tsaggr));
            else
                received.get(data.getData()).addAll(tsaggr);



//        System.out.println(String.format("p%s: Received de %s processos, clock=%s", me,received.size(),process.clock()));

        if (isReceivedFromAll(received.get(data.getData()))){
            int sm = max(received.get(data.getData()));

            doDeliver(data.getData(),sm);
        }

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

        Timestamp ts = new Timestamp(data.getSrc(),sm);
        if (data != null) {
            stamped.put(data, ts);
        }

        received.remove(data);


//        Ordenar lista de mensagens marcadas.

        ArrayList<AtomicData> deliverList = new ArrayList<>();
        for (Map.Entry<Data,Timestamp> m_ : stamped.entrySet()) {
            boolean menor = true;

            for (Map.Entry<Data,Set<Timestamp>> m__ : received.entrySet()) {
                if (m_.getValue().getTs() > max(m__.getValue()) || (m_.getValue().getTs() == (max(m__.getValue())) && m_.getValue().getId() > m__.getKey().getSrc())) {
                    menor = false;
                }
            }

            if (menor){
                deliverList.add(new AtomicData(m_.getKey(),m_.getValue()));
                stamped.remove(m_);
            }
        }




        deliverList.sort(new DeliverComparator());
        for (AtomicData m_ : deliverList) {
            delivered.add(m_);

            if (DEBUG)
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
        return counter >= vcube.getCorrects().size();
    }

    @Override
    public void statusChange(boolean suspected, int p) {
        super.statusChange(suspected,p);
    }

    @Override
    public void run() {
//        if (me == 0){
//            String message = "Dados"+me;
//            Data m = new Data(me,message);
//
//            broadcast_tree(m);
//        }

        if (me < 2){
            NekoSystem.instance().getTimer().schedule(new TimerTask() {
                @Override
                public void run() {
                    String message = "Dados"+me +"-c" + process.clock();
                    Data m = new Data(me,message);

                    broadcast_tree(m);
                }
            }, 0);
        }
//        if (me < 1){
//            NekoSystem.instance().getTimer().schedule(new TimerTask() {
//                @Override
//                public void run() {
//                    String message = "Dados"+me +"-c" + process.clock();
//                    Data m = new Data(me,message);
//
//                    broadcast_tree(m);
//                }
//            }, 8);
//        }


    }
}
