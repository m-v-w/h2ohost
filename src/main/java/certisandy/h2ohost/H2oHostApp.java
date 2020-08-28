package certisandy.h2ohost;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class H2oHostApp {
    public H2oHostApp() {
    }
    public static void main(String[] args) {
        System.setProperty("java.net.preferIPv4Stack","true");
        SpringApplication.run(H2oHostApp.class, args);
    }
}