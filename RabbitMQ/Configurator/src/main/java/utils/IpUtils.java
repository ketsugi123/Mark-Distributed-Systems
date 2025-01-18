package utils;

import org.slf4j.Logger;
import org.slf4j.simple.SimpleLoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


public class IpUtils {

    static Logger logger = new SimpleLoggerFactory().getLogger(IpUtils.class.getName());

    public static String getExternalIp() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://api.ipify.org"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (Exception e) {
            logger.error(e.getMessage());
            return null;
        }
    }
}
