package br.unioeste.ppgcomp.broadcast;

import br.unioeste.ppgcomp.broadcast.core.AbstractBroadcast;
import br.unioeste.ppgcomp.data.*;
import br.unioeste.ppgcomp.topologia.AbstractTopology;
import com.google.common.collect.Maps;
import lse.neko.*;
import org.checkerframework.checker.units.qual.A;

import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public class NewAtomicBroadcast<D> extends AbstractBroadcast {

    private ConcurrentMap<BroadcastMessage<D>,Integer> stamped;
    private ConcurrentMap<BroadcastMessage<D>, TreeSet<Timestamp>> received;

//    Mapa do conjunto de timestamps enviados a cada processo e o to, from de cada ACK
    private ConcurrentMap<TreeMessage<D>, Map<Integer,Integer>> pendingAck;

    private List<Integer> last_i;

    //    relógio lógico que identifica unicamente as mensagens enviadas por i
    private int lc;

    //    timestamp utilizado para ordem total
    private int ts;

    private final static int TREE = 1045;
    private final static int ACK = 1046;

    static{
        MessageTypes.instance().register(TREE,"TREE");
        MessageTypes.instance().register(ACK,"ACK");

    }

    public NewAtomicBroadcast(NekoProcess process, SenderInterface sender, String name, AbstractTopology topology) {
        super(process, sender, name);

        stamped = Maps.newConcurrentMap();
        received = Maps.newConcurrentMap();
        pendingAck = Maps.newConcurrentMap();

        last_i = new ArrayList<>();
        for (int i = 0; i < np; i++) {
            last_i.add(-1);
        }

        // inicializa ts e lc
        ts = lc = 0;
    }

    public void broadcast(D data){
        BroadcastMessage m = new BroadcastMessage(data,me,lc);

        Timestamp timestamp = new Timestamp(me,ts);

        lc += 1;
        ts = Math.max(lc,ts);

        received.put(m, new TreeSet<>());
        received.get(m).add(timestamp);

        TreeSet<Timestamp> tsaggr = new TreeSet<>();
        tsaggr.add(timestamp);

        TreeMessage<D> tree = new TreeMessage<D>(m, me, tsaggr);

//        Calcula número de dimensões do VCube
        forward(tree,log2(np));

    }

    public void forward(TreeMessage<D> tree, int size){
        List<Integer> neighbors = topo.neighborhood(me,size);

        double delay = DELAY;
        for (int i: neighbors) {

            NekoMessage m = new NekoMessage(me,new int[]{i},getId(),tree,TREE);
            addAck(tree.getFrom(),i,tree);

            timer.schedule(new SenderTask(m),delay);

            delay+=DELAY;
        }

    }

    public int log2(int n){
        return (int) Math.ceil(Math.log(n)/Math.log(2));
    }

    private void receiveTree(NekoMessage m){
        int src = m.getSource();

        TreeMessage<D> tree = (TreeMessage<D>) m.getContent();

//        Verifica se processo de origem está suspeito, caso estiver mod2==1, nenhuma ação é tomada
        if (!topo.getCorrects().contains(src))
            return;

        ts = Math.max(ts+1,tree.getMaxTimestamp());

        TreeSet<Timestamp> tsaggr = new TreeSet(tree.getTsaggr());

        BroadcastMessage<D> data = tree.getData();
        if (data.getSeq() > last_i.get(data.getSrc()) &&
            !received.containsKey(data) && !stamped.containsKey(data)){

//           Local timestamp
            Timestamp timestamp = new Timestamp(me,ts);
            tsaggr.add(timestamp);

            List<Integer> neighbors = topo.neighborhood(me,log2(np));
            List<Integer> subtree = topo.neighborhood(me,cluster(me,src) - 1);

            // diferenla entre as duas listas, remove todas os processos na árvore do processo de origem
            neighbors.removeAll(subtree);
            double delay = DELAY;
            for (int i: neighbors) {
                TreeSet<Timestamp> re_ts = new TreeSet<>();
                re_ts.add(timestamp);

                TreeMessage<D> replay = new TreeMessage<>(data,me,re_ts);
                NekoMessage m_ = new NekoMessage(me,new int[]{i},getId(),replay,TREE);

                timer.schedule(new SenderTask(m_),delay);
                addAck(me,i,replay);

                delay += DELAY;
            }
        }
//      Adiciona todos os timestamps de tree recebidos
        if (received.get(data) != null){
            received.get(data).addAll(tsaggr);
        } else {
            received.put(data,tsaggr);
        }
//      Encaminha os processos à árvore do processo de origem
        TreeMessage<D> fwd = new TreeMessage<>(data,src,tsaggr);

//        Cluster em que processo está
        int s = cluster(me,src) - 1;
        forward(fwd.clone(), s);
        checkDeliverable(data);


        if (s > 0)
            checkAcks(src,fwd);
        else
            checkAcks(src,tree);


    }

    private void checkAcks(int src, TreeMessage<D> data) {
        if (pendingAck.get(data) == null){
            TreeSet<Timestamp> tsaggr = (TreeSet) data.getTsaggr();
            if (tsaggr.last().getId() == me)
                tsaggr.remove(tsaggr.last());

            ACKPending<D> ack = new ACKPending<D>(data,me);
            NekoMessage m = new NekoMessage(new int[]{src},getId(),ack,ACK);

            NekoSystem.instance().getTimer().schedule(new SenderTask(m),DELAY);
        }


    }

    private void receiveACk (NekoMessage m){
        int src = m.getSource();

        ACKPending<D> ack = (ACKPending<D>) m.getContent();
        TreeMessage<D> treeAck = ack.getData();

        AtomicReference<TreeMessage<D>> fromTree = new AtomicReference<>();
//        Remove pendencia de ACK de processo j (src)
        pendingAck.forEach((t, acks) -> {
            if (t.getData().equals(treeAck.getData()) && t.getTsaggr().equals(treeAck.getTsaggr()))
                fromTree.set(t);
        });
        TreeMessage<D> tree = fromTree.get();

        pendingAck.get(tree).remove(src);

//        Remove pendência chave de pendingAcks
        if (pendingAck.get(tree).size() == 0)
           pendingAck.remove(tree);

//        Verifica se é possível entregar mensagem
        checkDeliverable(tree.getData());

        if (tree.getFrom() != me)
            checkAcks(tree.getFrom(),tree);
    }

    private void checkDeliverable(BroadcastMessage data){
        if (received.get(data) == null)
            return;

        boolean deliverable = false;

//       Verifica quantos processos suspeitos enviaram os seus timestamps
        AtomicInteger fault = new AtomicInteger(0);
        received.get(data).forEach(timestamp -> {
            if (!topo.getCorrects().contains(timestamp.getId())){
                fault.incrementAndGet();
;            }
        });

//        Verifica se foi recebido ao menos os timestamps de todos os processos corretos
        if ((received.get(data).size() - fault.get()) >= topo.getCorrects().size()) {
            deliverable = true;
        }

        if (deliverable){
//            Maior ts adicionado ao TreeSet, a ordenação é baseada no valor de cada timestamp
            int sn = received.get(data).last().getTs();

            doDeliver(data, sn);
        }


    }

    private void doDeliver(BroadcastMessage data, int sn) {
//        Adiciona a mensagem com o timestamp associado
        if (data != null) {
            stamped.put(data, sn);
        }
        // TODO Verificar com o Luiz essa parte, linha 42
        if (data.getSeq() == last_i.get(data.getSrc())){
            last_i.set(data.getSrc(), data.getSeq());
        }

//      Remove todos as entradas associadas com a mensagem no received
        received.remove(data);

        ConcurrentMap<BroadcastMessage<D>, Integer> deliverable = Maps.newConcurrentMap();
        stamped.forEach((m_, ts_) -> {
            AtomicBoolean menor = new AtomicBoolean(true);
            received.forEach((m__, ts__) -> {
                if (ts_ > ts__.first().getTs())
                    menor.set(false);
            });

            if (menor.get()){
                deliverable.put(m_, ts_);
            }
        });

//        Ordena mensagens a serem entregues e realiza a entrega
        deliverable.entrySet()
                .stream()
                .sorted((t0, t1) -> {
                    if (t0.getValue() != t1.getValue())
                        return t0.getValue() - t1.getValue();
                    else if (t0.getKey().getSrc() != t1.getKey().getSrc())
                        return t0.getKey().getSrc() - t1.getKey().getSrc();

                    return 0;
                })
                .forEach(e -> {
                        publish(e.getKey());
                    }
                );
        //        Remove as mensagens de stamped
        deliverable.forEach((m, ts) -> {
            stamped.remove(m);
        });
    }

    private void addAck(int from, int to, TreeMessage<D> tree){
        if (pendingAck.get(tree) != null) {
            pendingAck.get(tree).put(to,from);
        } else {
            Map<Integer,Integer> acks = new HashMap<>();
            acks.put(to,from);

            pendingAck.put(tree, acks);
        }
    }

    @Override
    public void deliverMessage(NekoMessage m) {

        int type = m.getType();

        switch (type){
            case TREE:
                receiveTree(m);
                break;
            case ACK:
                receiveACk(m);
                break;
        }

    }

    public int cluster(int i, int j){
        return (MSB(i,j) + 1);
    }



    public int MSB(int i, int j) {
        int s = 0;
        for (int k = i ^ j; k > 0; k = k >> 1) {
            s++;
        }
        return --s;
    }

    @Override
    public void run() {

    }
}
