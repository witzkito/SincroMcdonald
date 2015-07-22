/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sincromcdonald;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author witzkito
 */
public class Rubro {
    
    private int id;
    private String rubro;
    private Map subrubro = new HashMap();

    public Map getSubrubro() {
        return subrubro;
    }

    public void setSubrubro(Map subrubro) {
        this.subrubro = subrubro;
    }

    public Rubro() {
    }

    public int getId() {
        return id;
    }

    public String getRubro() {
        return rubro;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setRubro(String rubro) {
        this.rubro = rubro;
    }    
}
