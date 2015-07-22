/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sincromcdonald;

/**
 *
 * @author witzkito
 */
public class Subrubro {
    
    private int id;
    private String subrubro;
    private Rubro rubro;

    public Subrubro(int id, String subrubro, Rubro rubro) {
        this.id = id;
        this.subrubro = subrubro;
        this.rubro = rubro;
    }

    public Subrubro() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSubrubro() {
        return subrubro;
    }

    public void setSubrubro(String subrubro) {
        this.subrubro = subrubro;
    }

    public Rubro getRubro() {
        return rubro;
    }

    public void setRubro(Rubro rubro) {
        this.rubro = rubro;
    }
    
    
    
    
}
