package sincromcdonald;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 *
 * @author Witzkito
 */
public class ConexionDB {
     
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
            System.out.println("Error1 en la Conexión con la BD "+ ex.getMessage());
            SincroMcdonald.guardarLog("Error1 en la Conexión con la BD "+ ex.getMessage());
            conexion=null;
        }
        catch(SQLException ex)
        {
            System.out.println("Error2 en la Conexión con la BD "+ex.getMessage());
            SincroMcdonald.guardarLog("Error2 en la Conexión con la BD "+ex.getMessage());
            conexion=null;
        }
        catch(Exception ex)
        {
            System.out.println("Error3 en la Conexión con la BD "+ ex.getMessage());
            SincroMcdonald.guardarLog("Error3 en la Conexión con la BD "+ ex.getMessage());
            conexion=null;
        }
        finally
        {
            return conexion;
        }
    }
}
