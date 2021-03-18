package br.unioeste.ppgcomp.topologia;

import java.util.ArrayList;
import java.util.List;

public class All2AllTopology extends AbstractTopology{


    public All2AllTopology(int n) {
        super(n);
    }

    @Override
    public List<Integer> destinations(int me) {
        List<Integer> dest = new ArrayList<>();

        for (int i=0; i < corrects.size(); i++)
            if (corrects.get(i) != me)
                dest.add(corrects.get(i));

        return dest;
    }

    @Override
    public List<Integer> destinations(int me, int source) {
        if (me == source)
            return destinations(me);

        return new ArrayList<>();
    }
}
