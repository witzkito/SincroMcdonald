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
public class Marca {
    
    private int id;
    private String nombre;
    private String web;
    private String logo;
    private int estado;
    private String slug;
    private int orden;
    private int show;

    public Marca(int id, String nombre, String web, String logo, int estado, String slug, int orden, int show) {
        this.id = id;
        this.nombre = nombre;
        this.web = web;
        this.logo = logo;
        this.estado = estado;
        this.slug = slug;
        this.orden = orden;
        this.show = show;
    }

    public Marca() {
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getWeb() {
        return web;
    }

    public String getLogo() {
        return logo;
    }

    public int getEstado() {
        return estado;
    }

    public String getSlug() {
        return slug;
    }

    public int getOrden() {
        return orden;
    }

    public int getShow() {
        return show;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setWeb(String web) {
        this.web = web;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public void setEstado(int estado) {
        this.estado = estado;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public void setOrden(int orden) {
        this.orden = orden;
    }

    public void setShow(int show) {
        this.show = show;
    }
    
}
