/*
 * Witzkito - 01/09/2015 - witzkito@gmail.com
 */
package sincromcdonald;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author witzkito
 */
public class Clientes {
    
    private int id;
    private String codigo;
    private String nombre;
    private String tipoDoc;
    private String doc;
    private String direccion;
    private String telefono;
    private String email;
    private Localidad localidad;

    public Clientes() {
    }

    public Clientes(int id, String codigo, String nombre, String tipoDoc, String doc, String direccion, String telefono, String email) {
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
        this.tipoDoc = tipoDoc;
        this.doc = doc;
        this.direccion = direccion;
        this.telefono = telefono;
        this.email = email;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTipoDoc() {
        return tipoDoc;
    }

    public void setTipoDoc(String tipoDoc) {
        this.tipoDoc = tipoDoc;
    }

    public String getDoc() {
        return doc;
    }

    public void setDoc(String doc) {
        this.doc = doc;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Localidad getLocalidad() {
        return localidad;
    }

    public void setLocalidad(Localidad localidad) {
        this.localidad = localidad;
    }

    
    
}
