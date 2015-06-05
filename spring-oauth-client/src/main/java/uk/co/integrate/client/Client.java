package uk.co.integrate.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Minimal example client, demonstrating the use of Spring Security OAuth to connect to Integrate APIs.
 */
@Component
public class Client {

    @Autowired
    RestTemplate restTemplate;

    /**
     * Runs a simple test, accessing the same protected URL over and over at five second intervals. This demonstrates
     * that the Spring client detects when a token expires and re-authorises automatically - doing all the heavy
     * lifting for you.
     *
     * In real use you would simply inject the RestTemplate wherever you need to access the API and make requests as
     * needed. See the Spring docs. for more on RestTemplate and its use.
     */
    public void accessResource() {

        while (true) {

            String jsonString = restTemplate.getForObject(
                    "http://localhost:8090/simple-web-app/api/users", String.class);

            System.out.println(jsonString);

            try {
                Thread.sleep(5000);
            }
            catch (InterruptedException e) {

                // Restore the interrupted status
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void main(String[] args) {

        // Initialise the context
        AbstractApplicationContext applicationContext =
                new AnnotationConfigApplicationContext("uk.co.integrate.client");
        applicationContext.registerShutdownHook();

        // Run the client test
        Client client = applicationContext.getBean(Client.class);
        client.accessResource();
    }
}
