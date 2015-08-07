package sincromcdonald;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 *
 * @author witzkito
 */
public class ConexionWEB {
     
    public static Connection GetConnection()
    {
        Connection conexion=null;
     
        try
        {
            Class.forName("com.mysql.jdbc.Driver");
            String servidor = "jdbc:mysql://";
            String usuarioDB="";
            String passwordDB="";          
           conexion= DriverManager.getConnection(servidor,usuarioDB,passwordDB);
        }
        catch(ClassNotFoundException ex)
        {
            System.out.println("Error1 en la Conexión con la WEB "+ex.getMessage());
            SincroMcdonald.guardarLog("Error1 en la Conexión con la WEB "+ex.getMessage());
            conexion=null;
        }
        catch(SQLException ex)
        {
            System.out.println("Error2 en la Conexión con la WEB "+ex.getMessage());
            SincroMcdonald.guardarLog("Error2 en la Conexión con la WEB "+ex.getMessage());
            conexion=null;
        }
        catch(Exception ex)
        {
            System.out.println("Error3 en la Conexión con la WEB "+ex.getMessage());
            SincroMcdonald.guardarLog("Error2 en la Conexión con la WEB "+ex.getMessage());
            conexion=null;
        }
        finally
        {
            return conexion;
        }
    }
}
