package artmann.microjava.codegen;

import java.util.ArrayList;
import java.util.List;

public final class Label {

    private int adr;

    private final Code code;

	public Label(Code code) {
        this.code = code;
	    adr = -1;
    }

    private List<Integer> fixupList = new ArrayList<>();

    public void put() {
        if(isDefined()){
            code.put2(adr - (code.pc -1));
        } else {
            fixupList.add(code.pc);
            code.put2(0);
        }
    }


    public void here() {
        if(isDefined()){
            throw new IllegalStateException("label defined twice");
        }

        for(int pos : fixupList){
            code.put2(pos, code.pc - (pos-1));
        }

        fixupList = null;
        adr = code.pc;
    }

    private boolean isDefined(){
        return adr >= 0;
    }
}
