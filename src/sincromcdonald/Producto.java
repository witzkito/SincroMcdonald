/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sincromcdonald;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author witzkito
 */
public class Producto {

    private int id;
    private int codigo;
    private String nombre;
    private String descripcion;
    private String precio;
    private double stock;
    private Subrubro Subrubro;
    private Marca marca;
    private Date modificacion;
    private boolean oferta;
    private int stockMinimo;
    private boolean vigente;

    public Producto(int id, int codigo, String nombre, String descripcion, String precio, double stock) {
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.stock = stock;
    }
    
    public Producto(){}
    
    public double getId() {
        return id;
    }

    public int getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getPrecio() {
        return precio;
    }

    public double getStock() {
        return stock;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setCodigo(int codigo) {
        this.codigo = codigo;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public void setPrecio(String precio) {
        this.precio = precio;
    }

    public void setStock(double stock) {
        this.stock = stock;
    }

    public Date getModificacion() {
        return modificacion;
    }

    public void setModificacion(Date modificacion) {
        this.modificacion = modificacion;
    }

    public boolean isOferta() {
        return oferta;
    }

    public void setOferta(boolean oferta) {
        this.oferta = oferta;
    }

    public int getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(int stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public Marca getMarca() {
        return marca;
    }

    public void setMarca(Marca marca) {
        this.marca = marca;
    }

    public Subrubro getSubrubro() {
        return Subrubro;
    }

    public void setSubrubro(Subrubro Subrubro) {
        this.Subrubro = Subrubro;
    }

    public boolean isVigente() {
        return vigente;
    }

    public void setVigente(boolean vigente) {
        this.vigente = vigente;
    }
    
}