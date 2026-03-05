import java.sql.Connection;
import java.sql.DriverManager;

public class DbTest {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/?useSSL=false&serverTimezone=UTC";
        String user = "root";
        String pass = "1333719559";
        try (Connection con = DriverManager.getConnection(url, user, pass)) {
            System.out.println("Connection successful!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
