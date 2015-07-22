/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sincromcdonald;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;


/**
 *
 * @author witzkito
 */
public class SincroMcdonald {
        static String rutaLocalImagen = "uploads/productos/";
        static String rutaLocalImagenThumbs = "uploads/productos/thumbs/";
        static String server = "dmcdonald.com.ar";
        static int port = 21;
        static String user = "";
        static String pass = "";
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        iniciar();        
    }
       
   private static void iniciar(){
       //Conexiones
        Connection conDb; Connection conWeb;
        ResultSet rsDb;ResultSet rsCount; Producto producto;
        ResultSet rsWeb = null; Float progreso= new Float(0).floatValue();
        conDb = ConexionDB.GetConnection();
        conWeb = ConexionWEB.GetConnection();
        DecimalFormat decimal = new DecimalFormat("#.##");
        try {

        Statement cmdDb = conDb.createStatement();
        Statement cmdWeb = conWeb.createStatement();
        Statement cmdBorrar = conWeb.createStatement();
        cmdBorrar.executeUpdate("UPDATE productos set borrar = true");
        rsCount = cmdDb.executeQuery("SELECT count(CARTICULO) as cantidad from articulos WHERE vigente = true");
        rsCount.next();
        float cantidad = new Float(100).floatValue() / new Float(rsCount.getInt("cantidad")).floatValue();
        rsDb = cmdDb.executeQuery("SELECT * FROM articulos WHERE vigente = true");
        System.out.println("Llenando la memoria de productos");
        Map productos = llenarProductos();
        while (rsDb.next()){
            progreso = progreso + cantidad;
            System.out.println("------------------   " + decimal.format(progreso) + "%   ------------------");
            System.out.println("Verificando actualizacion en " + rsDb.getString("CARTICULO") + " - " +rsDb.getString("ARTICULO_CORTO"));
            if (!actualizado(rsDb, productos)){
                producto = new Producto();
                producto.setCodigo(rsDb.getInt("CARTICULO"));
                producto.setDescripcion(rsDb.getString("OBSERVACIONES"));
                if(producto.getDescripcion() == null){
                    producto.setDescripcion(" ");
                }
                producto.setNombre(rsDb.getString("ARTICULO_CORTO"));
                producto.setPrecio(rsDb.getString("PU_CON_IVA_1"));
                producto.setSubrubro(getSubrubro(rsDb.getInt("CSUBRUBRO"), rsDb.getInt("CRUBRO")));
                producto.setMarca(getMarca(rsDb.getInt("CMARCA")));
                producto.setOferta(rsDb.getBoolean("ES_OFERTA"));
                producto.setStockMinimo(rsDb.getInt("EXISTENCIA_MINIMA"));
                Date fechaUpdate;
                if (rsDb.getDate("FMODI") == null){
                    fechaUpdate = new Date();
                }else{
                    fechaUpdate = rsDb.getDate("FMODI");
                }
                Statement cmdDbStock = conDb.createStatement();
                ResultSet rsStock = cmdDbStock.executeQuery("SELECT CANTIDAD FROM stock WHERE CARTICULO LIKE " + producto.getCodigo());
                if(rsStock.next()){
                    producto.setStock(rsStock.getDouble("CANTIDAD"));
                }
                cargarImagen(producto.getCodigo());
                cmdWeb.executeUpdate("DELETE FROM productos WHERE codigo = " + producto.getCodigo());
                cmdWeb.executeUpdate("INSERT INTO productos (codigo, nombre, descripcion, precio, stock, subrubro_id, modificacion, oferta, stock_minimo, marca_id) VALUES ('"+ producto.getCodigo() +"','"+
                        producto.getNombre() + "','" + producto.getDescripcion()+ "','" + producto.getPrecio() +"', " + producto.getStock() +
                        ", " + insertSubRubro(producto.getSubrubro()) + ", '" + new SimpleDateFormat("yyyy/MM/dd H:m:s").format(fechaUpdate) + "', " + producto.isOferta() + "," + producto.getStockMinimo()+ 
                        ", " + insertMarca(producto.getMarca()) +")");
               
                cmdDbStock.close();
            }
            cmdBorrar.executeUpdate("UPDATE productos set borrar = false where codigo = " + rsDb.getInt("CARTICULO"));            
        }
        ResultSet conBorrar = cmdBorrar.executeQuery("SELECT count(id) as cantidad from productos WHERE borrar = true");
        conBorrar.next();
        System.out.println("Se encontrar " + conBorrar.getInt("cantidad") + " productos a limpiar... limpiando");
        cmdBorrar.executeUpdate("DELETE FROM productos where borrar = true");
        
        }catch (SQLException ex) {

            System.out.println("SQLException Iniciar: " + ex.getMessage());

        }
   }
   
   private static Subrubro getSubrubro(int id, int id_rubro) throws SQLException
   {
       Connection conDb; String codigo;
       conDb = ConexionDB.GetConnection();
       try{
            Statement st= conDb.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM subrubros WHERE CSUBRUBRO = " + id + " and CRUBRO = " + id_rubro);
            if (rs.next()){
                Subrubro subrubro = new Subrubro();
                subrubro.setId(rs.getInt("CSUBRUBRO"));
                subrubro.setSubrubro(rs.getString("SUBRUBRO"));
                codigo = rs.getString("CRUBRO");
                conDb.close();                
                subrubro.setRubro(getRubro(codigo));
                return subrubro;
            }            
       }catch (SQLException ex) {

            System.out.println("SQLException getSubRubro: " + ex.getMessage());
       }
       return null;
   }
   
   private static Rubro getRubro(String id)
   {
       Connection conDb; 
       conDb = ConexionDB.GetConnection();
       try{
            Statement st= conDb.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM rubros WHERE CRUBRO = " + id);
            if (rs.next()){
                Rubro rubro = new Rubro();
                rubro.setId(rs.getInt("CRUBRO"));
                rubro.setRubro(rs.getString("RUBRO"));
                conDb.close();
                return rubro;
            }
       }catch (SQLException ex) {

            System.out.println("SQLException getRubro: " + ex.getMessage());
       }
       return null;
   }
   
   private static Marca getMarca(int id){
      
       Connection conDb; 
       conDb = ConexionDB.GetConnection();
       try{
            Statement st= conDb.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM marcas WHERE CMARCA = " + id);
            if (rs.next()){
                Marca marca = new Marca();
                marca.setId(rs.getInt("CMARCA"));
                marca.setNombre(rs.getString("MARCA"));
                marca.setOrden(0);
                marca.setShow(0);
                marca.setSlug(toSlug(rs.getString("MARCA")));
                marca.setWeb(null);
                marca.setLogo(toSlug(rs.getString("MARCA"))+ ".jpg");
                conDb.close();
                return marca;
            }
       }catch (SQLException ex) {

            System.out.println("SQLException getMarca: " + ex.getMessage());

       }
       return null;
   }
   
   private static int insertSubRubro(Subrubro subrubro)
   {
       Connection conWeb;
       conWeb = ConexionWEB.GetConnection();
       try{
            Statement stWeb = conWeb.createStatement();
            ResultSet rs = stWeb.executeQuery("SELECT * FROM subrubros WHERE subrubro LIKE '"+ subrubro.getSubrubro() + "'");
            if (rs.next()){
                int id = rs.getInt("id");
                conWeb.close();
                return id;
            }else{
                stWeb.executeUpdate("INSERT INTO subrubros (rubro_id, subrubro) VALUES ('" + insertRubro(subrubro.getRubro()) + "', '" + subrubro.getSubrubro() + "')");
                ResultSet rs2 = stWeb.executeQuery("SELECT * FROM subrubros WHERE subrubro LIKE '"+ subrubro.getSubrubro() + "'");
                if (rs2.next()){
                    int id = rs2.getInt("id");
                    conWeb.close();
                    return id;
                }
            }
            conWeb.close();
        }catch (SQLException ex) {
            System.out.println("SQLException insertSubrubro: " + ex.getMessage());
       }
       return 0;
   }
   
   private static int insertRubro(Rubro rubro)
   {
       Connection conWeb;
       conWeb = ConexionWEB.GetConnection();
       try{
            Statement stWeb = conWeb.createStatement();   
            ResultSet rs = stWeb.executeQuery("SELECT * FROM rubros WHERE rubro LIKE '"+ rubro.getRubro() + "'");
            if (rs.next()){
                int id = rs.getInt("id");
                conWeb.close();
                return id;
            }else{
                stWeb.executeUpdate("INSERT INTO rubros (rubro) VALUES ('" + rubro.getRubro() + "')");
                ResultSet rs2 = stWeb.executeQuery("SELECT * FROM rubros WHERE rubro LIKE '"+ rubro.getRubro() + "'");
                if (rs2.next()){
                    int id = rs2.getInt("id");
                    conWeb.close();
                    return id;
                }
            }
            conWeb.close();
        }catch (SQLException ex) {
            System.out.println("SQLException insertSubrubro: " + ex.getMessage());
       }
       return 0;
   }
   
   private static int insertMarca(Marca marca)
   {
       Connection conWeb; String sql = "";
       conWeb = ConexionWEB.GetConnection();
       try{
            Statement stWeb = conWeb.createStatement();
            ResultSet rsExiste = stWeb.executeQuery("SELECT * from marcas WHERE nombre LIKE '"+ marca.getNombre()+"'");
            if(!rsExiste.next()){
                sql= "INSERT INTO marcas (id, nombre, web, logo, estado, slug, orden, shows) VALUES ("+ marca.getId() + ",'" + marca.getNombre() + "','"
                        + marca.getWeb() + "','"+ marca.getLogo() + "'," + marca.getEstado() + ", '" + marca.getSlug() +
                        "'," + marca.getOrden() + ",0)";
                stWeb.executeUpdate("INSERT INTO marcas (id, nombre, web, logo, estado, slug, orden, shows) VALUES ("+ marca.getId() + ",'" + marca.getNombre() + "','"
                        + marca.getWeb() + "','"+ marca.getLogo() + "'," + marca.getEstado() + ", '" + marca.getSlug() +
                        "'," + marca.getOrden() + ",0)");
                ResultSet rs = stWeb.executeQuery("SELECT * FROM marcas WHERE nombre LIKE '"+ marca.getNombre() + "'");
                if (rs.next()){
                    int id = rs.getInt("id");
                    conWeb.close();
                    return id;
                }
                conWeb.close();
            }else{
                conWeb.close();
                return marca.getId();
            }
        }catch (SQLException ex) {
            System.out.println("SQLException insertMarca: " + ex.getMessage() + " - " + sql);
       }
       return 0;
   }
   
  private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
  private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

  public static String toSlug(String input) {
    String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
    String normalized = Normalizer.normalize(nowhitespace, Form.NFD);
    String slug = NONLATIN.matcher(normalized).replaceAll("");
    return slug.toLowerCase(Locale.ENGLISH);
  }
  
    private static void cargarImagen(int codigo){
        Connection conDb;
        conDb = ConexionDB.GetConnection();
        try{
            Statement stDb = conDb.createStatement();
            ResultSet rs = stDb.executeQuery("SELECT * FROM articulos_imagenes WHERE CARTICULO = '" + codigo + "'");
            if (rs.next()){
               InputStream binaryStream = rs.getBinaryStream("IMAGEN");
               
               File f=new File(rutaLocalImagen + codigo + ".jpg");
               OutputStream salida=new FileOutputStream(f);
               byte[] buf =new byte[1024];//Actualizado me olvide del 1024
               int len;
               while((len=binaryStream.read(buf))>0){
                   salida.write(buf,0,len);
               }
               f.createNewFile();
               salida.close();
               binaryStream.close();
               System.out.println("Se realizo la conversion con exito");
               conDb.close();
               
               //se carga la imagen en tumbs
                BufferedImage originalImage = ImageIO.read(f);
                int type = originalImage.getType() == 0? BufferedImage.TYPE_INT_ARGB : originalImage.getType();
 
		BufferedImage resizeImageJpg = resizeImage(originalImage, type);
		ImageIO.write(resizeImageJpg, "jpg", new File(rutaLocalImagenThumbs + codigo + ".jpg"));
                subirFTP(codigo);
            }
        }catch (SQLException ex) {
            System.out.println("SQLException Get Imagenes: " + ex.getMessage());
        }catch(IOException e){
            System.out.println("Se produjo el error : "+e.toString());
        }
        
    }
    
    
    
    private static BufferedImage resizeImage(BufferedImage originalImage, int type){
        int total = (500*100)/originalImage.getWidth();
        int width = (originalImage.getWidth()*total)/100;
        int height =  (originalImage.getHeight()*total)/100;
        
	BufferedImage resizedImage = new BufferedImage(width, height, type);
	Graphics2D g = resizedImage.createGraphics();
       
	g.drawImage(originalImage, 0, 0, width, height, null);
	g.dispose();
 
	return resizedImage;
    }
    
    private static boolean actualizado(ResultSet rsDb, Map productos){
        Statement stWeb; String sql = ""; Connection conWeb; Connection conDb;
        try{
            conDb = ConexionDB.GetConnection();
            Statement cmdDbStock = conDb.createStatement();
            ResultSet rsStock = cmdDbStock.executeQuery("SELECT CANTIDAD FROM stock WHERE CARTICULO LIKE " + rsDb.getInt("CARTICULO"));
            Producto producto = (Producto)productos.get(rsDb.getInt("CARTICULO"));
            if(producto != null){
                if( rsDb.getDate("FMODI") == null){
                    if (rsStock.next()){
                            if (producto.getStock() != rsStock.getInt("CANTIDAD") ){
                                conDb.close();
                                return false;
                            }else{
                                conDb.close();
                                return true;
                            }
                        }
                    conDb.close();
                    return true;
                }else{
                    if (producto.getModificacion().compareTo(rsDb.getDate("FMODI")) < 0 ){
                        conDb.close();
                        return false;
                    }else{
                        if (rsStock.next()){
                            if (producto.getStock() != rsStock.getInt("CANTIDAD") ){
                                conDb.close();
                                return false;
                            }else{
                                conDb.close();
                                return true;
                            }
                        }else{
                            conDb.close();
                            return true;
                        }
                        
                    }
                }
            }
            conDb.close();
            return false;
        }catch (SQLException ex) {
            System.out.println("SQLException Comprobando actualizado: " + ex.getMessage());
        }
        return true;
    }
    
    private static void subirFTP(int codigo)
    {
        FTPClient ftpClient = new FTPClient();
        try {
 
            ftpClient.connect(server, port);
            ftpClient.login(user, pass);
            ftpClient.enterLocalPassiveMode();
 
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
 
            // APPROACH #1: uploads first file using an InputStream
            File firstLocalFile = new File(rutaLocalImagen + codigo + ".jpg");
 
            String firstRemoteFile = codigo + ".jpg";
            InputStream inputStream = new FileInputStream(firstLocalFile);
            ftpClient.changeWorkingDirectory("/uploads/productos/");
 
            System.out.println("Comenzando a subir imagen");
            boolean done = ftpClient.storeFile(firstRemoteFile, inputStream);
            inputStream.close();
            if (done) {
                System.out.println("La imagen se subio correctamente");
            }else{
                System.out.println("Error al subir la imagen");
            }
            
            File secondLocalFile = new File(rutaLocalImagenThumbs + codigo + ".jpg");
 
            String secondRemoteFile = codigo + ".jpg";
            InputStream inputSecondStream = new FileInputStream(secondLocalFile);
            ftpClient.changeWorkingDirectory("/uploads/productos/thumbs/");
 
            System.out.println("Comenzando a subir imagen thumbs");
            boolean doneSecond = ftpClient.storeFile(secondRemoteFile, inputSecondStream);
            inputSecondStream.close();
            if (doneSecond) {
                System.out.println("La imagen thumbs se subio correctamente");
            }else{
                System.out.println("Error al subir la imagen thumbs");
            }
            
            
 
        } catch (IOException ex) {
            System.out.println("Error: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    private static Map llenarProductos()
    {
        Connection conWeb = ConexionWEB.GetConnection(); Map retornar = new HashMap();
        Producto producto;
        try{
            Statement st = conWeb.createStatement();
            ResultSet rsweb = st.executeQuery("SELECT * FROM productos");
            while (rsweb.next())
            {
                producto = new Producto();
                producto.setCodigo(rsweb.getInt("codigo"));
                producto.setNombre(rsweb.getString("nombre"));
                producto.setDescripcion(rsweb.getString("descripcion"));
                producto.setId(rsweb.getInt("id"));
                producto.setPrecio(rsweb.getString("precio"));
                producto.setStock(rsweb.getInt("stock"));
                producto.setModificacion(rsweb.getDate("modificacion"));
                producto.setOferta(rsweb.getBoolean("oferta"));
                producto.setStockMinimo(rsweb.getInt("stock_minimo"));
                retornar.put(producto.getCodigo(),producto);
                
            }            
        }catch (SQLException ex) {
            System.out.println("SQLException Comprobando actualizado: " + ex.getMessage());
        }
        return retornar;
    }
}
