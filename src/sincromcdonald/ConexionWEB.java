package sincromcdonald;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.swing.JOptionPane;

/**
 *
 * @author Kevin Arnold
 */
public class ConexionWEB {
     
    public static Connection GetConnection()
    {
        Connection conexion=null;
     
        try
        {
            Class.forName("com.mysql.jdbc.Driver");
            String servidor = "jdbc:mysql://dmcdonald.com.ar/mcdonald_db";
            String usuarioDB="";
            String passwordDB="";
            conexion= DriverManager.getConnection(servidor,usuarioDB,passwordDB);
        }
        catch(ClassNotFoundException ex)
        {
            JOptionPane.showMessageDialog(null, ex, "Error1 en la Conexión con la BD "+ex.getMessage(), JOptionPane.ERROR_MESSAGE);
            conexion=null;
        }
        catch(SQLException ex)
        {
            JOptionPane.showMessageDialog(null, ex, "Error2 en la Conexión con la BD "+ex.getMessage(), JOptionPane.ERROR_MESSAGE);
            conexion=null;
        }
        catch(Exception ex)
        {
            JOptionPane.showMessageDialog(null, ex, "Error3 en la Conexión con la BD "+ex.getMessage(), JOptionPane.ERROR_MESSAGE);
            conexion=null;
        }
        finally
        {
            return conexion;
        }
    }
}
