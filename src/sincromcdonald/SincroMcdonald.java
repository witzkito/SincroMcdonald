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
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;


/**
 *
 * @author witzkito
 */
public class SincroMcdonald {
        static String rutaLocalImagen = "uploads/productos/";
        static String rutaLocalImagenThumbs = "uploads/productos/thumbs/";
        static String server = "dmcdonald.com.ar";
        static int port = 21;
        static String user = "mcdonald";
        static String pass = "sWuuHPswac8E";
        static FileWriter logFile;
        static FTPFile[] filesFtp;
        static FTPFile[] filesFtpThumbs;
        static Map localidades = new HashMap();
        private static final String PATTERN_EMAIL = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
            + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        localidades = cargarLocalidades();
        cargarClientes();
        iniciar();        
    }
       
   private static void iniciar() throws IOException{
       //Conexiones
        Connection conDb; Connection conWeb;
        ResultSet rsDb;ResultSet rsCount; Producto producto;
        ResultSet rsWeb = null; Float progreso= new Float(0).floatValue();
        conDb = ConexionDB.GetConnection();
        conWeb = ConexionWEB.GetConnection();
        DecimalFormat decimal = new DecimalFormat("#.##");
        try {
        guardarLog("--- Se inicio Script --- " + new SimpleDateFormat("dd-MM-yyyy_HH:mm:ss").format(Calendar.getInstance().getTime()));
        Statement cmdDb = conDb.createStatement();
        Statement cmdWeb = conWeb.createStatement();
        Statement cmdBorrar = conWeb.createStatement();
        cmdBorrar.executeUpdate("UPDATE productos set borrar = true");
        rsCount = cmdDb.executeQuery("SELECT count(CARTICULO) as cantidad from articulos WHERE vigente = true");
        rsCount.next();
        float cantidad = new Float(100).floatValue() / new Float(rsCount.getInt("cantidad")).floatValue();
        rsDb = cmdDb.executeQuery("SELECT * FROM articulos WHERE vigente = true");
        
        getFilesFtp();
        
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
                producto.setNombre(rsDb.getString("ARTICULO_LARGO"));
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
                cmdWeb.executeUpdate("DELETE FROM productos WHERE codigo LIKE '" + producto.getCodigo() + "'");
                cmdWeb.executeUpdate("INSERT INTO productos (codigo, nombre, descripcion, precio, stock, subrubro_id, modificacion, oferta, stock_minimo, marca_id) VALUES ('"+ producto.getCodigo() +"','"+
                        producto.getNombre() + "','" + producto.getDescripcion()+ "','" + producto.getPrecio() +"', " + producto.getStock() +
                        ", " + insertSubRubro(producto.getSubrubro()) + ", '" + new SimpleDateFormat("yyyy/MM/dd H:m:s").format(fechaUpdate) + "', " + producto.isOferta() + "," + producto.getStockMinimo()+ 
                        ", " + insertMarca(producto.getMarca()) +")");
               
                cmdDbStock.close();
            }
            cmdBorrar.executeUpdate("UPDATE productos set borrar = false where codigo = " + rsDb.getInt("CARTICULO"));
            
            //Controla si existe la foto, en caso contrario la sube
            if (!isExistImagen(rsDb)){
                cargarImagen(rsDb.getInt("CARTICULO"));
            }
            if (!isExistImagenThumbs(rsDb)){
                cargarImagen(rsDb.getInt("CARTICULO"));
            }
            
        }
        ResultSet conBorrar = cmdBorrar.executeQuery("SELECT count(id) as cantidad from productos WHERE borrar = true");
        conBorrar.next();
        System.out.println("Se encontrar " + conBorrar.getInt("cantidad") + " productos a limpiar... limpiando");
        cmdBorrar.executeUpdate("DELETE FROM productos where borrar = true");
        guardarLog("--- Se finalizo Script --- " + new SimpleDateFormat("dd-MM-yyyy_HH:mm:ss").format(Calendar.getInstance().getTime()));
        subirLog();
        }catch (SQLException ex) {

            System.out.println("SQLException Iniciar: " + ex.getMessage());
            guardarLog("SQLException Iniciar: " + ex.getMessage());

        }
   }
   
   private static void getFilesFtp()
   {
        try {
            FTPClient ftpClient = new FTPClient();
            ftpClient.connect(server, port);
            ftpClient.login(user, pass);
            ftpClient.changeWorkingDirectory("/public_html/uploads/productos/");
            
            filesFtp = ftpClient.listFiles();
            
            ftpClient.changeWorkingDirectory("/public_html/uploads/productos/thumbs/");
            filesFtpThumbs = ftpClient.listFiles();
        } catch (IOException ex) {
            System.out.println("Error Trayendo listas de archivos FTP: " + ex.getMessage());
            guardarLog("Error: " + ex.getMessage());
        }
   }
   
   /**
    * Recorre la lista de archivos para ver si existe ya o no la imagen
    * @param rs
    * @return 
    */
   private static boolean isExistImagen(ResultSet rs)
   {
            try {
                String codigoArt = String.valueOf(rs.getInt("CARTICULO"));
                for (FTPFile file : filesFtp) {
                    if (file.getName().equals(codigoArt + ".jpg")){
                        return true;
                    }
                }
                return false;
            } catch (SQLException ex) {
               System.out.println("Error si existe imagen: " + ex.getMessage());
            guardarLog("Error si existe imagen: " + ex.getMessage());
            return true;
            }
   }
   
   /**
    * Recorre la lista de archivos thumbs para ver si existe ya o no la imagen
    * @param rs
    * @return 
    */
   private static boolean isExistImagenThumbs(ResultSet rs)
   {
        try {
            String codigoArt = String.valueOf(rs.getInt("CARTICULO"));
            for (FTPFile file : filesFtpThumbs) {
                if (file.getName().equals(codigoArt + ".jpg")){
                    return true;
                }
            }
            return false;
        } catch (SQLException ex) {
            System.out.println("Error si existe thumbs: " + ex.getMessage());
            guardarLog("Error si existe thumbs: " + ex.getMessage());
            return true;
        }
   }
   
   private static Subrubro getSubrubro(int id, int id_rubro) throws SQLException, IOException
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
            guardarLog("SQLException getSubRubro: " + ex.getMessage());
       }
       return null;
   }
   
   private static Rubro getRubro(String id) throws IOException
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
            guardarLog("SQLException getRubro: " + ex.getMessage());
       }
       return null;
   }
   
   private static Marca getMarca(int id) throws IOException{
      
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
            guardarLog("SQLException getMarca: " + ex.getMessage());

       }
       return null;
   }
   
   private static int insertSubRubro(Subrubro subrubro) throws IOException
   {
       Connection conWeb;
       conWeb = ConexionWEB.GetConnection();
       try{
            Statement stWeb = conWeb.createStatement();
            ResultSet rs = stWeb.executeQuery("SELECT * FROM subrubros"
                    + " inner join rubros on subrubros.rubro_id = rubros.id"
                    + " WHERE subrubro LIKE '"+ subrubro.getSubrubro() + "' and rubros.rubro LIKE '"+ subrubro.getRubro().getRubro() + "'" );
            if (rs.next()){
                int id = rs.getInt("id");
                if (!rs.getString("subrubro").equals(subrubro.getSubrubro())){
                    stWeb.executeUpdate("UPDATE subrubros set subrubro = '" + subrubro.getSubrubro() +"' where id = " + id);
                }
                conWeb.close();
                return id;
            }else{
                stWeb.executeUpdate("INSERT INTO subrubros (rubro_id, subrubro) VALUES ('" + insertRubro(subrubro.getRubro()) + "', '" + subrubro.getSubrubro() + "')");
                ResultSet rs2 = stWeb.executeQuery("SELECT * FROM subrubros"
                    + " inner join rubros on subrubros.rubro_id = rubros.id"
                    + " WHERE subrubro LIKE '"+ subrubro.getSubrubro() + "' and rubros.rubro LIKE '"+ subrubro.getRubro().getRubro() + "'" );
                if (rs2.next()){
                    int id = rs2.getInt("id");
                    conWeb.close();
                    return id;
                }
            }
            conWeb.close();
        }catch (SQLException ex) {
            System.out.println("SQLException insertSubrubro: " + ex.getMessage());
            guardarLog("SQLException insertSubrubro: " + ex.getMessage());
       }
       return 0;
   }
   
   private static int insertRubro(Rubro rubro) throws IOException
   {
       Connection conWeb;
       conWeb = ConexionWEB.GetConnection();
       try{
            Statement stWeb = conWeb.createStatement();   
            ResultSet rs = stWeb.executeQuery("SELECT * FROM rubros WHERE rubro LIKE '"+ rubro.getRubro() + "'");
            if (rs.next()){
                int id = rs.getInt("id");
                if (!rs.getString("rubro").equals(rubro.getRubro())){
                    stWeb.executeUpdate("UPDATE rubros set rubro = '" + rubro.getRubro() +"' where id = " + id);
                }
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
            guardarLog("SQLException insertSubrubro: " + ex.getMessage());
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
                int id = marca.getId();
                if (!rsExiste.getString("nombre").equals(marca.getNombre())){
                    stWeb.executeUpdate("UPDATE marcas set nombre = '" + marca.getNombre() +"' where id = " + id);
                }
                conWeb.close();
                return marca.getId();
            }
        }catch (SQLException ex) {
            System.out.println("SQLException insertMarca: " + ex.getMessage());
            guardarLog("SQLException insertMarca: " + ex.getMessage());
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
               
               File file = new File(rutaLocalImagenThumbs + codigo + ".jpg");
               FileImageOutputStream output = new FileImageOutputStream(file);
               
               Iterator iter = ImageIO.getImageWritersByFormatName("jpeg");
               ImageWriter writer = (ImageWriter)iter.next();
               ImageWriteParam iwp = writer.getDefaultWriteParam();
               iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
               iwp.setCompressionQuality(0.8F);
               
                BufferedImage originalImage = ImageIO.read(f);
                int type = originalImage.getType() == 0? BufferedImage.TYPE_INT_ARGB : originalImage.getType();
 
		BufferedImage resizeImageJpg = resizeImage(originalImage, type);
                writer.setOutput(output);
                IIOImage image = new IIOImage(resizeImageJpg, null, null);
                writer.write(null, image, iwp);
                writer.dispose();
		//ImageIO.write(resizeImageJpg, "jpg", new File(rutaLocalImagenThumbs + codigo + ".jpg"));
                subirFTP(codigo);
            }
        }catch (SQLException ex) {
            System.out.println("SQLException Get Imagenes: " + ex.getMessage());
            guardarLog("SQLException Get Imagenes: " + ex.getMessage());
        }catch(IOException e){
            System.out.println("Se produjo el error : "+e.toString());
            guardarLog("Se produjo el error: " + e.getMessage());
        }
        
    }
    
    
    
    private static BufferedImage resizeImage(BufferedImage originalImage, int type){
        int total = (500*100)/originalImage.getWidth();
        float ratio = (430*100)/originalImage.getWidth();
        int width = new Float(originalImage.getWidth() * (ratio/100)).intValue();
        int height =  new Float(originalImage.getHeight() * (ratio/100)).intValue();
        System.out.println(ratio);
        System.out.println(width);
        System.out.println(height);
        System.out.println(type);
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
            guardarLog("SQLException Comprobando actualizado: " + ex.getMessage());
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
            ftpClient.changeWorkingDirectory("/public_html/uploads/productos/");
 
            System.out.println("Comenzando a subir imagen");
            boolean done = ftpClient.storeFile(firstRemoteFile, inputStream);
            inputStream.close();
            if (done) {
                System.out.println("La imagen se subio correctamente");
            }else{
                System.out.println("Error al subir la imagen");
                guardarLog("Error al subir la imagen");
            }
            
            File secondLocalFile = new File(rutaLocalImagenThumbs + codigo + ".jpg");
 
            String secondRemoteFile = codigo + ".jpg";
            InputStream inputSecondStream = new FileInputStream(secondLocalFile);
            ftpClient.changeWorkingDirectory("/public_html/uploads/productos/thumbs/");
 
            System.out.println("Comenzando a subir imagen thumbs");
            boolean doneSecond = ftpClient.storeFile(secondRemoteFile, inputSecondStream);
            inputSecondStream.close();
            if (doneSecond) {
                System.out.println("La imagen thumbs se subio correctamente");
            }else{
                System.out.println("Error al subir la imagen thumbs");
                guardarLog("Error al subir la imagen");
            }
            
            
 
        } catch (IOException ex) {
            System.out.println("Error: " + ex.getMessage());
            ex.printStackTrace();
            guardarLog("Error: " + ex.getMessage());
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                guardarLog("Error: " + ex.getMessage());
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
            guardarLog("SQLException Comprobando actualizado: " + ex.getMessage());
        }
        return retornar;
    }
    
    public static void guardarLog(String mensaje) {
            try {
                File filelog = new File("config.txt"); String line = "";
                String newLine =  line + mensaje;
                PrintWriter pw = new PrintWriter(new FileWriter(filelog, true));
                pw.print(newLine + "\n");
                pw.close();
            } catch (IOException ex) {
                System.out.println("Error al registrar el log!! " + ex.getMessage());                
            }
    }
    
    private static void subirLog()
    {
        FTPClient ftpClient = new FTPClient();
        try {
 
            ftpClient.connect(server, port);
            ftpClient.login(user, pass);
            ftpClient.enterLocalPassiveMode();
 
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
 
            File file = new File("config.txt");
 
            InputStream inputStream = new FileInputStream(file);
            ftpClient.changeWorkingDirectory("/public_html/");
 
            boolean done = ftpClient.storeFile("logSincro.txt", inputStream);
            inputStream.close();
            if (done) {
                System.out.println("El Archivo se subio correctamente");
            }else{
                System.out.println("Error al subir el archivo");
                guardarLog("Error al subir el lig");
            }
        } catch (IOException ex) {
            System.out.println("Error al subir log: " + ex.getMessage());
            ex.printStackTrace();
            guardarLog("Error al subir log: " + ex.getMessage());
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                guardarLog("Error al subir log: " + ex.getMessage());
            }
        }
    }
    
    private static void cargarClientes()
    {
        System.out.println("Cargando Clientes");
        Map clientes = cargarMapClientes();
        System.out.println(clientes.size() + " Clientes Encontrados");
        
        Connection conDB = ConexionDB.GetConnection();
        ResultSet rs;
        try {
            rs = conDB.createStatement().executeQuery("SELECT * FROM clientes WHERE vigente = 1");

            while (rs.next())
            {
                Clientes cliente = (Clientes)clientes.get(rs.getString("CCLIENTE"));
                if (cliente == null)
                {
                    insertarCliente(rs);
                }else{
                    if (rs.getDate("FMODI") != null){
                        if(cliente.getModificacion().compareTo(rs.getDate("FMODI")) < 0 )
                        {
                            Connection conWEB = ConexionWEB.GetConnection();
                            Statement st = conWEB.createStatement();
                            st.execute("DELETE FROM clientes WHERE codigo = " + rs.getDate("FMODI"));
                            insertarCliente(rs);
                        }
                    }
                    
                }
            }
            conDB.close();
        } catch (SQLException ex) {
                guardarLog("Error Cargando clientes: " + ex.getMessage());
       }
    }
    
    private static void insertarCliente(ResultSet rs)
    {
        try {
            Clientes cli = new Clientes();
            cli.setCodigo(rs.getString("CCLIENTE"));            
            cli.setDireccion(rs.getString("DIRECCION"));
            cli.setDoc(rs.getString("NIDENTIFICACION"));
            cli.setTipoDoc(rs.getString("IDENTIFICACION"));
            if (rs.getString("OBSERVACIONES") != null){
                Pattern pattern = Pattern.compile(PATTERN_EMAIL);
                Matcher matcher = pattern.matcher(rs.getString("OBSERVACIONES"));
                if (matcher.matches()){
                    cli.setEmail(rs.getString("OBSERVACIONES"));
                }else{
                    cli.setEmail("");
                }
            }else{
                cli.setEmail("");
            }
            cli.setNombre(rs.getString("CLIENTE"));
            cli.setTelefono(rs.getString("TELEFONO"));
            cli.setLocalidad(getLocalidad(rs.getInt("CLOCALIDAD")));
            cli.setModificacion(new Date());
            if( !insertarCliente(cli)){
                System.out.println("Se inserto cliente -" + cli.getNombre());
            }else{
                System.out.println("Error al insertar Cliente");
                guardarLog("Error al insertar Cliente");
            }
        } catch (SQLException ex) {
                guardarLog("Error al insertar el cliente: " + ex.getMessage());
        }
    }
    
    private static Map cargarMapClientes(){
        Map retornar = new HashMap();
        Connection conWEB = ConexionWEB.GetConnection();
        ResultSet rs;
        try {
            rs = conWEB.createStatement().executeQuery("SELECT * from clientes");
            while (rs.next())
            {
                Clientes cli = new Clientes();
                cli.setId(rs.getInt("id"));
                cli.setCodigo(rs.getString("codigo"));
                cli.setDireccion(rs.getString("direccion"));
                cli.setDoc(rs.getString("doc"));
                cli.setTipoDoc(rs.getString("tipoDoc"));
                cli.setEmail(rs.getString("email"));
                cli.setNombre(rs.getString("nombre"));
                cli.setTelefono(rs.getString("telefono"));
                cli.setLocalidad(getLocalidad(rs.getInt("localidad_id")));
                cli.setModificacion(rs.getDate("modificacion"));
                retornar.put(cli.getCodigo(), cli);
                
            }
            conWEB.close();
            return retornar;
        } catch (SQLException ex) {
            guardarLog("Error cargando mapas de clientes: " + ex.getMessage());
        }
        return null;
    }
    
    private static boolean insertarCliente(Clientes cli)
    {
        Connection conWEB = ConexionWEB.GetConnection();
        String sql = "INSERT INTO clientes ";
        sql = sql + "(email, nombre, doc, direccion, telefono, codigo, tipoDoc, localidad_id, modificacion) ";
        sql = sql + "VALUES (";
        sql = sql + "'" + cli.getEmail() +"',";
        sql = sql + "'" + cli.getNombre() +"',";
        sql = sql + "'" + cli.getDoc() +"',";
        sql = sql + "'" + cli.getDireccion() +"',";
        sql = sql + "'" + cli.getTelefono() +"',";
        sql = sql + "'" + cli.getCodigo() +"',";
        sql = sql + "'" + cli.getTipoDoc() +"',";
        sql = sql + cli.getLocalidad().getId() + ",'";
        sql = sql + new SimpleDateFormat("yyyy/MM/dd H:m:s").format(cli.getModificacion()) + "'";
        sql = sql + ")";
        try {
            boolean bool = conWEB.createStatement().execute(sql);
            conWEB.close();
            return bool;            
        } catch (SQLException ex) {
            guardarLog("Error insertando clientes en la bd: " + ex.getMessage());
        }
        return false;
    }
    
    private static Localidad getLocalidad(int clocalidad)
    {
       if ((Localidad)localidades.get(clocalidad) != null)
       {
           return (Localidad)localidades.get(clocalidad);
       }else{
           Connection conDB = ConexionDB.GetConnection();
           ResultSet rs;
           try {
                rs = conDB.createStatement().executeQuery("SELECT * from localidad WHERE clocalidad LIKE " + clocalidad);
                rs.next();
                Localidad loc = new Localidad();
                loc.setId(rs.getInt("CLOCALIDAD"));
                loc.setNombre(rs.getString("LOCALIDAD"));
                loc.setCpp(rs.getString("CP"));
                loc.setProvincia(rs.getInt("CPROVINCIA"));
                conDB.close();
                return insertarLocalidad(loc);
           } catch (SQLException ex) {
               guardarLog("Error trayendo localidad: " + ex.getMessage());
           }
       }
       return null;
    }
    
    private static Map cargarLocalidades(){
        Map retornar = new HashMap();
        Connection conWEB = ConexionWEB.GetConnection();
        ResultSet rs;
        try {
            rs = conWEB.createStatement().executeQuery("SELECT * from localidades");
            while (rs.next())
            {
                Localidad loc = new Localidad();
                loc.setId(rs.getInt("id"));
                loc.setCpp(rs.getString("ccpp"));
                loc.setNombre(rs.getString("localidad"));
                loc.setProvincia(rs.getInt("provincia_id"));
                retornar.put(loc.getId(), loc);
                
            }
            conWEB.close();
            return retornar;
        } catch (SQLException ex) {
            guardarLog("Error Cargando Localidades en el mapa: " + ex.getMessage());
        }
        return null;
    }
    
    private static Localidad insertarLocalidad(Localidad loc)
    {
        Connection conWEB = ConexionWEB.GetConnection();
        String sql = "INSERT INTO localidades ";
        sql = sql + "(id, localidad,ccpp, provincia_id) ";
        sql = sql + "VALUES (";
        sql = sql + "'" + loc.getId() +"',";
        sql = sql + "'" + loc.getNombre() +"',";
        sql = sql + "'" + loc.getCpp() +"', ";
        sql = sql + loc.getProvincia() ;
        sql = sql + " )";
        try {
            conWEB.createStatement().execute(sql);
            localidades.put(loc.getId(), loc);
            conWEB.close();
            return loc;
        } catch (SQLException ex) {
            guardarLog("Error Insertando localidad: " + ex.getMessage());
        }
        return null;
    }
}
