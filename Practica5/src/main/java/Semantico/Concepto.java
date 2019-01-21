package Semantico;

import java.util.ArrayList;
import java.util.List;

public class Concepto {
    public List<Concepto> broader = new ArrayList<>();
    public List<Concepto> narrower = new ArrayList<>();
    public List<String> prefLabel = new ArrayList<>();
    public List<String> altLabel = new ArrayList<>();
    public String base;

    public Concepto(String base) {
        this.base=base;
    }

    public String isRelated(String concepto) {
        for(Concepto i : broader)
            if(i.equals(concepto)) {
                return i.getBase();
            }
        for(Concepto i : narrower)
            if(i.equals(concepto)) {
                return i.getBase();
            }
        if(prefLabel.contains(concepto))return base;
        else if(altLabel.contains(concepto))return base;
        else if(base == concepto)return base;
        else return "";
    }

    public String getBase() {
        return base;
    }

    public void addBroader(Concepto concepto) {
        broader.add(concepto);
    }
    public void addNarrower(Concepto concepto) {
        narrower.add(concepto);
    }
    public void addPrefLabel(String concepto) {
        prefLabel.add(concepto);
    }
    public void addAltLabel(String concepto) {
        altLabel.add(concepto);
    }

    @Override
    public boolean equals(Object concept) {
        String cmp = (String) concept;
        return this.base.equals(cmp);
    }
}
