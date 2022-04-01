package Server;
import java.util.Properties;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication // if we use 'mvnw spring-boot:run' we start the application
public class NamingServerSpringApp {
    public static void main(String[]  args) {
        SpringApplication springApp = new SpringApplication(NamingServerSpringApp.class); // new spring app we can run
        Properties properties = new Properties(); //property list for springApp
        properties.put("server.port", "8081"); // use server port 8088
        springApp.setDefaultProperties(properties);
        springApp.run(args); // Run the springApp (naming server)
    }
}
