package br.unioeste.ppgcomp.fault;

import lse.neko.*;
import lse.neko.util.TimerTask;
import lse.neko.util.logging.NekoLogger;
import org.apache.java.util.Configurations;

import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class CrashProtocol extends ActiveReceiver
        implements CrashInterface,SenderInterface {

    private Boolean crashed;

    protected static Logger logger = NekoLogger.getLogger("messages");
    public static final String LOG_DROP = "d";
    public static final String LOG_CRASH = "crash";
    public static final String LOG_RECOVER = "recover";

    private SenderInterface sender;


    public CrashProtocol(NekoProcess process, SenderInterface sender, String name) {
        super(process, "crash-"+name);


        this.sender = sender;

        // Configurações de crash
        loadConfig();


        crashed = new Boolean(false);
    }

    public void crash() {
        if (!crashed) {
            crashed = true;
            logger.fine("crash started at "+this.getName());
        } else {
            logger.fine("WARNING: process already crashed!");
        }
    }

    public void recover() {
        if (crashed) {
            crashed = false;
            logger.fine("crash stoped at "+this.getName());
        } else {
            logger.fine("WARNING:process was not crashed!");
        }
    }



    public boolean isCrashed() {
        return crashed;
    }

    public void setSender(SenderInterface sender) {
        this.sender = sender;
    }

    @Override
    public NekoMessage receive() {
        if (isCrashed())
            return null;

        return super.receive();
    }

    private void loadConfig(){
        Configurations config = NekoSystem.instance().getConfig();

        if (config == null) {
            return;
        }

        // ID do processo atual
        int id = process.getID();

        // Parâmetros para definição de instantes de falhas
        String paramStartSingle = String.format("process.%s.crash.start", id);
        String paramStartMultiple = "process.crash.start";
        String paramEndMultiple = "process.crash.stop";
        String paramEndSingle = String.format("process.%s.crash.stop",id);

        // Recupera das configurações tempo para inicio da falha (crash-start
        String[] cs_single = config.getStringArray(paramStartSingle);
        String[] cs_multiple = config.getStringArray(paramStartMultiple);
        String[] cs_end_single = config.getStringArray(paramEndSingle);
        String[] cs_end_multiple = config.getStringArray(paramEndMultiple);

        // Interrompe configuração, nenhum parâmetro foi configurado
        if (cs_multiple == null || cs_single == null){
            logger.fine(String.format("None fault configuration found for process %s!!", process.getID()));
            return;
        }

        double[] startTimes = null;

        // Atinge somente o processo atual
        if (cs_single != null){
            startTimes = getTimes(cs_single);
        } else { // Configuração que geral, não especifica um único processo
            startTimes = getTimes(cs_end_multiple);
        }

        // Verifica se algum tempo foi recuperado
        if (startTimes != null)
            // Agenda as tarefas de crash no simulador
            for (int i = 0; i < startTimes.length; i++) {
                CrashTask task = new CrashTask(this);
                taskScheduler(task,startTimes[i]);
            }

        double[] endTimes = null;

        // Verificar se as configurações para os tempos para Recover estão definidos
        if (cs_end_single != null) { // Configuração somente do processo atual
            endTimes = getTimes(cs_end_single);
        } else if (cs_end_multiple != null){ // Configuração geral
            endTimes = getTimes(cs_end_multiple);
        }

        // Verifica se algum tempo foi recuperado
        if (endTimes != null)
            // Agenda as tarefas de crash no simulador
            for (int i = 0; i < endTimes.length; i++) {
                RecoverTask task = new RecoverTask(this);
                taskScheduler(task,endTimes[i]);
            }

    }

    public void send(NekoMessage m) {
        if (!crashed) {
            sender.send(m);
        } else {// is crashed
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE,
                        "",
                        new Object[] {
                                new NekoMessageEvent(LOG_DROP, m)
                        });
            }
        }

    }

    private double[] getTimes(String[] t){
        double[] tv = new double[t.length];

        for (int i = 0; i < t.length; i++) {
            try {
                t[i] = t[i].trim();
                tv[i] = Double.parseDouble(t[i]);

            } catch (Exception e){
                throw new RuntimeException(String.format("Error in crash.start parameter %s for process %s\n Erro: %s", t[i],process.getID(),e.getMessage()));
            }

        }
        return tv;
    }

    private void taskScheduler(TimerTask task, double time){
        NekoSystem.instance().getTimer().schedule(task, Double.valueOf(time));
    }

    class CrashTask extends TimerTask {
        CrashProtocol t;

        public CrashTask(CrashProtocol t) {
            this.t = t;
        }

        @Override
        public void run() {
            logger.fine("Crash started!!!!!");
            t.crash();
        }
    }

    class RecoverTask extends TimerTask {
        CrashProtocol t;

        public RecoverTask(CrashProtocol t) {
            this.t = t;
        }

        @Override
        public void run() {
            logger.fine("Crash recovered!!!!!");
            t.recover();
        }

    }
}
