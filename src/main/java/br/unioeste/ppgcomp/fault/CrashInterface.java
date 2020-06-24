package br.unioeste.ppgcomp.fault;

public interface CrashInterface {
     void crash();
     void recover();
     boolean isCrashed();
}
