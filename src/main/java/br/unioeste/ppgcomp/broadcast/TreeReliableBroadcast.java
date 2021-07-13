package br.unioeste.ppgcomp.broadcast;

import br.unioeste.ppgcomp.broadcast.core.AbstractBroadcast;
import br.unioeste.ppgcomp.data.AckData;
import br.unioeste.ppgcomp.data.TreeData;
import br.unioeste.ppgcomp.topologia.VCubeTopology;
import lse.neko.*;

import java.util.ArrayList;
import java.util.List;

public class TreeReliableBroadcast extends AbstractBroadcast {

    private TreeData last[];
    private NekoMessageQueue pending_acks;
    private List<AckData> ack_set;
    private List<Integer> corrects;

    private List<TreeData> delivered;
    private VCubeTopology vcube;

    private int timestamp;
    private int np;
    private int me;

    private static int TREE = 1104;
    private static int ACK = 1105;
    private static int APP = 1106;

    private static int DEFAULT_INTERVAL = 2;

    static{
        MessageTypes.instance().register(TREE,"TREE");
        MessageTypes.instance().register(ACK,"ACK");
        MessageTypes.instance().register(APP,"APP");
    }

    public TreeReliableBroadcast(NekoProcess process, SenderInterface sender, String name) {
        super(process, sender, name);

        np = process.getN();
        me = process.getID();

        last = new TreeData[np];

        pending_acks = new NekoMessageQueue();
        corrects = new ArrayList<Integer>();

        ack_set = new ArrayList<AckData>();
        delivered = new ArrayList<TreeData>();


        for (int i = 0; i < np; i++) {
            corrects.add(i);
        }

        vcube = new VCubeTopology(log2(np));
        vcube.setCorrects(corrects);
    }



    public void broadcast(TreeData data){
        List<Integer> dests = vcube.neighborhood(data.getSource(),log2(np));


        if (data.getSource() == me && !delivered.contains(data)){
            delivered.add(data);
            logger.info(String.format("P%s: DELIVERED em broadcast m=%s", me,data));
        }


        for (int j: dests){
           // if (j == me)
           //     continue;

            NekoMessage mj = new NekoMessage(me,new int[]{j},getId(),data,TREE);
            send(mj);


            ack_set.add(new AckData(me,j,data));
        }
    }




    @Override
    public void run() {
        double simulation_time = NekoSystem.instance().getConfig().getDouble("simulation.time");


        if (me == 0){
            for (int i = 0; i < 1; i++) {
                if (isCrashed())
                    return;

                timestamp++;

                TreeData data = new TreeData(timestamp,me,me);

                broadcast(data);

                try {
                    NekoThread.sleep(DEFAULT_INTERVAL);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        // Para um segundo processo enviar as mensagens
  /*      if (me == 1) {
            while (process.clock() < simulation_time){
                if (process.clock() > 2){
                    timestamp++;
                    TreeData data = new TreeData(timestamp,me,"Novo");
                    broadcast(data);

                    break;
                }

                try {
                    NekoThread.sleep(DEFAULT_INTERVAL);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }*/
    }
    public void checkAcks(int src, TreeData data){
        for (AckData pending: ack_set){
            if (pending.getM() == data){
                return;
            }
        }
        NekoMessage mr = new NekoMessage(new int[]{src},getId(),data,ACK);
        send(mr);
        logger.info(String.format("P%s: Todos ACKs entregues do processo com origem em P%s e ts=%s entregues!!", me,data.getSource(),data.getTs()));
    }

    // crash
    public void statusChange(boolean suspected, int p) {
        if (suspected && corrects.contains(p)) {
          //  logger.info(String.format("p%s: Detectou falha de P%s", me,p));

            // retira processo suspeito
            for (int i = 0; i < corrects.size(); i++) {
                if (corrects.get(i) == p) {
                    corrects.remove(i);
                    break;
                }
            }

            int k = vcube.ff_neighboor(me, (vcube.cluster(me, p)) );



            // Cria um temp dos acks
            List<AckData> temp_set = new ArrayList<AckData>(ack_set);

            if (k != -1) { // possui mais nós naquele cluster!
                for (AckData ack : temp_set) {
                    if (ack.getDest() == p) { // Se havia ACKs pendentes do processo que falhou
                        send(new NekoMessage(me, new int[]{k}, getId(), ack.getM(), TREE));
                        ack_set.add(new AckData(me, k, ack.getM()));

                        ack_set.remove(ack);
                    }
                }
            } else if (last[p] != null){ // Nenhum nó restante no cluster que falhou refazendo broadcast
                broadcast(last[p]);
            }
        }
    }
    private int log2(int v){
        return (int) (Math.log(v)/Math.log(2));
    }



    @Override
    public void doDeliver(NekoMessage m) {
        if (isCrashed())
            return;

        int type = m.getType();

        if (type == TREE){
            TreeData ms = (TreeData) m.getContent();

            if (last[ms.getSource()] == null || ms.getTs() == (last[ms.getSource()].getTs() + 1)){
                last[ms.getSource()] = ms;

                delivered.add(ms);
                logger.info(String.format("P%s: DELIVERED em receive m=%s", me,ms));
            }
            // Acho que o source tem que ser o processo que fez o broadcast, não a origem da mensagem
            if (!corrects.contains(m.getSource())){
                broadcast(ms);
                return;
            }

            //int source = ms.getSource();
            int c = (vcube.cluster(m.getSource(),me) - 1);

            List<Integer> childs = vcube.neighborhood(me, c);

            for (int k: childs){
                NekoMessage message = new NekoMessage(me,new int[]{k},getId(),ms,TREE);
                send(message);

                ack_set.add(new AckData(m.getSource(),k,ms));
            }

            checkAcks(m.getSource(),ms);
        } else if (type == ACK){
            int source = m.getSource();
            TreeData data = (TreeData) m.getContent();

            AckData ack = null;
            for (AckData pending: ack_set){
                if (pending.getDest() == source && pending.getM() == data){
                    ack = pending;
                    ack_set.remove(pending);
                    break;
                }
            }
            if (ack != null) {
                checkAcks(ack.getSource(),data);
            }
        }
    }


}
