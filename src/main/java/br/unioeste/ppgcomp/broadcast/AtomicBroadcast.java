package br.unioeste.ppgcomp.broadcast;

import br.unioeste.ppgcomp.data.AtomicDataMessage;
import br.unioeste.ppgcomp.data.Data;
import br.unioeste.ppgcomp.data.TSDataMessage;
import br.unioeste.ppgcomp.data.TreeData;
import br.unioeste.ppgcomp.fault.CrashProtocol;
import br.unioeste.ppgcomp.overlay.VCube;
import lse.neko.*;

import java.util.*;

public class AtomicBroadcast extends CrashProtocol {

    // Conjunto de mensagens marcadas
    private TreeSet<TSDataMessage> stamped;

    // Conjunto de mensagens recebidas
    private TreeSet<TSDataMessage> received;

    // Conjunto de mensagens entregues
    private TreeSet<TSDataMessage> delivered;

    // Conjunto de mensagens que são marcadas para entrega, mas que precisam de ordenação
    private TreeSet<TSDataMessage> mdeliverable;

    // Marcador de tempo local
    private int lc;

    // Identificador do process
    private int me;

    // Numero de processos
    private int np;


    private List<Integer> corretos;

    // Overlay dos processos para encaminhamento em árvore
    private VCube vcube;

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

        init();
    }

    private void init(){
        np = process.getN();
        me = process.getID();


        stamped = new TreeSet<>();
        received = new TreeSet<>();
        delivered = new TreeSet<>();
        mdeliverable = new TreeSet<>();

        this.lc = 0;

        corretos = new ArrayList<>();
        for (int i = 0; i < np; i++) {
            corretos.add(i);
        }

        vcube = new VCube(log2(np));
        vcube.setCorrects(corretos);
    }

    public void broadcast(Data m){
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

        for (int p : destinos){
            AtomicDataMessage treeMessage = new AtomicDataMessage(source,tsaggr,data);

            NekoMessage m = new NekoMessage(from,new int[]{p},getId(),treeMessage,type);
            send(m);
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
    public void deliver(NekoMessage m) {
        //Dados da mensagem
        AtomicDataMessage data = (AtomicDataMessage) m.getContent();


        this.lc = Math.max(lc + 1,data.getMaxTimestamp());
        TreeSet<TSDataMessage> tsaggr = new TreeSet<>(data.getTsaggr());


        // subree src
        List<Integer> subtree_src = vcube.subtree(me, data.getSource());


        //System.out.println(String.format("p%s: src=%s == me=%s", me,subtree_src.toString(),subtree_me.toString()));


        if (!contains(received,data.getData())) {
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

        received.addAll(tsaggr);




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


        iterator = stamped.iterator();
        while (iterator.hasNext()) {
            boolean deliverable = true;

            TSDataMessage st = iterator.next();

            for (TSDataMessage t : received) {
                if (t.getTs() >= st.getTs()) {
                    deliverable = false;
                    break;
                }
            }

            if (deliverable){
                mdeliverable.add(ts);
                iterator.remove();

                System.out.println(String.format("p%s: Delivered mensagem de p%s com ts=%s", me,st.getP(),st.getTs()));
            }
        }






    }

    private int max(Set<TSDataMessage> set,Data data){
        int max = 0;

        for (TSDataMessage t : set){
            if (t.getData().equals(data) && t.getData().getTs() > max)
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
        return counter == np;
    }

    private int log2(int v){
        return (int) (Math.log(v)/Math.log(2));
    }

    @Override
    public void run() {
        if (me < 4){
            String message = String.valueOf(45+me);
            Data m = new Data(me,message,lc);

            broadcast(m);
        }

    }
}
