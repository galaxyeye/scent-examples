package ai.platon.scent.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class XClient {
    private URI scrapeApi = URI.create("http://api.platonic.fun/api/x/a/q");
    private HttpClient client = HttpClient.newHttpClient();

    public String submit(String sql) throws IOException, InterruptedException {
        String requestBody = "";
        HttpRequest request = HttpRequest.newBuilder(scrapeApi)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String uuid = response.body().toString();

        return uuid;
    }



    public static void main(String[] args) {

    }
}
