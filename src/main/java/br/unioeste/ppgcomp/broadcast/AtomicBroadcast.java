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

        stamped = new TreeSet<>();
        received = new TreeSet<>();
        delivered = new TreeSet<>();

        this.lc = 0;

    }

       public void broadcast_tree(Data m){
        TreeSet<TSDataMessage> tsaggr = new TreeSet<>();
        TSDataMessage t = new TSDataMessage(me,m,lc);
        tsaggr.add(t);

        // Encaminha para subárvore
        forward(me,me,m,tsaggr,TREE);
        received.add(t);

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

        if (!contains(received,data.getData()) && !contains(delivered,data.getData()) && !contains(stamped,data.getData()) ) {
            TSDataMessage ts = new TSDataMessage(me, data.getData(), lc);
            tsaggr.add(ts);

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

        // Verifica se não possui mensagem com falha
        Iterator<TSDataMessage> iterator = tsaggr.iterator();
        while (iterator.hasNext()){
            TSDataMessage d = iterator.next();

            if (!vcube.getCorrects().contains(d.getP())){
                System.out.println("Removendo mensagem");
                iterator.remove();
            }
        }

        if (!contains(delivered,data.getData()))
            received.addAll(tsaggr);



       // System.out.println(String.format("p%s: Received de %s processos, clock=%s", me,received.size(),process.clock()));

        if (isReceivedFromAll(data.getData()) && !contains(delivered,data.getData())){
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
                logger.info("delivered " + m_.toString());
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
    public void run() {
//        if (me == 0){
//            String message = "Dados"+me;
//            Data m = new Data(me,message);
//
//            broadcast_tree(m);
//        }

        if (me < 3){
            NekoSystem.instance().getTimer().schedule(new TimerTask() {
                @Override
                public void run() {
                    String message = "Dados"+me;
                    Data m = new Data(me,message);

                    broadcast_tree(m);
                }
            }, 0);
        }
    }
}
