package br.unioeste.ppgcomp.broadcast;

import br.unioeste.ppgcomp.broadcast.core.AbstractBroadcast;
import br.unioeste.ppgcomp.data.AtomicDataMessage;
import br.unioeste.ppgcomp.data.Data;
import br.unioeste.ppgcomp.data.TSDataMessage;
import lse.neko.*;
import lse.neko.util.TimerTask;

import java.util.*;

public class AtomicBroadcast extends AbstractBroadcast {



    // Conjunto de mensagens marcadas
    private TreeSet<TSDataMessage> stamped;

    // Conjunto de mensagens recebidas
    private TreeSet<TSDataMessage> received;


    // Conjunto de mensagens que são marcadas para entrega, mas que precisam de ordenação
    private TreeSet<TSDataMessage> delivered;

    private List<Data> dataset;

    // Marcador de tempo local
    private int lc;

    // IDs das mensagens utilizadas
    private static final int TREE = 1301;
    private static final int FWD = 1302;
    private static final int ACK = 1303;
    private static final int NFWD = 1304;

    static{
        MessageTypes.instance().register(TREE,"TREE");
        MessageTypes.instance().register(FWD,"FWD");
        MessageTypes.instance().register(ACK,"ACK");
        MessageTypes.instance().register(NFWD,"NFWD");
    }

    public AtomicBroadcast(NekoProcess process, SenderInterface sender, String name) {
        super(process, sender, name);

        stamped = new TreeSet<>();
        received = new TreeSet<>();
        delivered = new TreeSet<>();
        dataset = new ArrayList<>();

        this.lc = 0;

    }

       public void broadcast_tree(Data m){
        TreeSet<TSDataMessage> tsaggr = new TreeSet<>();
        TSDataMessage t = new TSDataMessage(me,m,lc);
        tsaggr.add(t);

        // Encaminha para subárvore
        forward(me,me,m,tsaggr,TREE);
        received.addAll(tsaggr);

        // Incrementa contador
        lc++;
    }

    public void forward(int source, int from, Data data, TreeSet<TSDataMessage> tsaggr,int type){
        // Utiliza overlay do vcube para definir destinos em uma topologia de árvore
        List<Integer> destinos = vcube.subtree(me,from);

        double delay = DELAY;
        for (int p : destinos){
            AtomicDataMessage treeMessage = new AtomicDataMessage(source,tsaggr,data);

            NekoMessage m = new NekoMessage(from,new int[]{p},getId(),treeMessage,type);
            NekoSystem.instance().getTimer().schedule(new SenderTask(m), delay);
            delay += DELAY;
        }
    }
    public boolean contains(TreeSet<TSDataMessage> list,Data o){
        for (TSDataMessage t : list){
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
        AtomicDataMessage data = (AtomicDataMessage) m.getContent();

        this.lc = Math.max(lc + 1,data.getMaxTimestamp());
        TreeSet<TSDataMessage> tsaggr = new TreeSet<>(data.getTsaggr());

        // subree src
        List<Integer> subtree_src = vcube.subtree(me, m.getSource());

        TSDataMessage ts = new TSDataMessage(me, data.getData(), lc);
        tsaggr.add(ts);

        if (!dataset.contains(data.getData())) {
            dataset.add(data.getData());

            for (int i : vcube.subtree(me,me)) {
                if (!subtree_src.contains(i)) {
                    TreeSet<TSDataMessage> tsi = new TreeSet<TSDataMessage>();
                    tsi.add(ts);

                    AtomicDataMessage ack = new AtomicDataMessage(me, tsi, data.getData());
                    send(new NekoMessage(me, new int[]{i}, getId(), ack, ACK));
                }
            }
        }



        forward(data.getSource(),m.getSource(),data.getData(),tsaggr,FWD);

//        Verifica se a mensagem já foi entregue, sem processamento desnecessario se já houver sido
        if (!contains(delivered, data.getData()) && !contains(stamped,data.getData()))
            received.addAll(tsaggr);
        else
            return;



//        System.out.println(String.format("p%s: Received de %s processos, clock=%s", me,received.size(),process.clock()));

        if (isReceivedFromAll(data.getData())){
            int sm = max(received,data.getData());

            doDeliver(data.getData(),sm);
        }

    }

    private void doDeliver(Data data,int sm){
   //     System.out.println(received.toString());

        TSDataMessage ts = new TSDataMessage(data.getSrc(),data,sm);
        stamped.add(ts);

        Iterator<TSDataMessage> iterator = received.iterator();
        while (iterator.hasNext()){
            TSDataMessage next = iterator.next();
            if (next.getData().equals(data))
                iterator.remove();
        }


//        Ordenar lista de mensagens marcadas.

        ArrayList<TSDataMessage> deliverList = new ArrayList<>();
        for (TSDataMessage m_ : stamped) {
            boolean menor = true;

            for (TSDataMessage m__ : received) {
                if (m_.getTs() > m__.getTs() || (m_.getTs() == m__.getTs() && m_.getData().getSrc() > m__.getData().getSrc())) {
                    menor = false;
                }
            }

            if (menor){
                deliverList.add(m_);

            }
        }

       stamped.removeAll(deliverList);

        deliverList.sort(new DeliverComparator());
        for (TSDataMessage m_ : deliverList) {
            delivered.add(m_);

            if (DEBUG)
                logger.info(String.format("p%s: delivered %s", me, m_.toString()) );
        }
    }
    private int max(Set<TSDataMessage> set,Data data){
        int max = 0;

        for (TSDataMessage t : set){
            if (t.getData().equals(data) && t.getTs() > max)
                max = t.getTs();
        }

        return max;
    }
    private boolean isReceivedFromAll(Object data){
        int counter = 0;
        for (TSDataMessage t : received){
            if (t.getData() == data){
                counter++;
            }
        }
        // Se contador for menor significa que nem todos receberam a mensagem
        return counter >= vcube.getCorrects().size();
    }

    @Override
    public void statusChange(boolean suspected, int p) {
        HashSet<Data> perdidas = new LinkedHashSet<>();

        if (suspected && vcube.getCorrects().contains(p)) {


            /***
             * Verificar em received se possui mensagens do processo falho...
             */
            Iterator iterator = received.iterator();

            while (iterator.hasNext()) {
                TSDataMessage m = (TSDataMessage) iterator.next();

                if (m.getP() == p) {
                    perdidas.add(m.getData());
                    System.out.printf("p%s: Removido mensagem de %s com origem em %s \n", me, p, m.getData().getSrc());
                    iterator.remove();
                }
            }

            super.statusChange(suspected, p);

            //        Verificar mensagens que estao em received e que podem ser entregues...
            HashSet<Data> dataset = new LinkedHashSet<>();
            for (TSDataMessage m: received)
                dataset.add(m.getData());

            iterator = dataset.iterator();
            while (iterator.hasNext()){
                Data data = (Data) iterator.next();
                int sm = max(received,data);


                TreeSet<TSDataMessage> tsi = new TreeSet<TSDataMessage>();
                tsi.add(new TSDataMessage(me,data,lc));

                boolean deliverable = isReceivedFromAll(data);

                AtomicDataMessage ack = new AtomicDataMessage(me, tsi, data);
                ack.setDeliverable(deliverable);

                // Reenvia para os vizinhos a mensagem
                for (int i : vcube.subtree(me,me)) {
                    send(new NekoMessage(me, new int[]{i}, getId(), ack, ACK));
                }

                // Se houver informacoes de todos os processos e não tiver sido entregue antes
                if (deliverable && !contains(delivered,data)){
                    doDeliver(data,sm);
                    iterator.remove();
                    continue;
                }


            }





        }



    }

    @Override
    public void run() {
//        if (me == 0){
//            String message = "Dados"+me;
//            Data m = new Data(me,message);
//
//            broadcast_tree(m);
//        }

        if (me == 0){
            NekoSystem.instance().getTimer().schedule(new TimerTask() {
                @Override
                public void run() {
                    String message = "Dados"+me +"-c" + process.clock();
                    Data m = new Data(me,message);

                    broadcast_tree(m);
                }
            }, 0);
        }

        if (me == 1){
            NekoSystem.instance().getTimer().schedule(new TimerTask() {
                @Override
                public void run() {
                    String message = "Dados"+me +"-c" + process.clock();
                    Data m = new Data(me,message);

                    broadcast_tree(m);
                }
            }, 5);
        }
    }
}
