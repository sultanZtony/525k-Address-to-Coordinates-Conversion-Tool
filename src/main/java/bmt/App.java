package bmt;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.http.client.utils.URIBuilder;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.BufferedReader;
// import java.io.FileReader;
// import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.io.File;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedWriter;

public class App {

    private static final String API_KEY = "API_Key"; // Your API Key
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final Gson GSON = new Gson();

    public static void main(String[] args) {
        File csvInputFile = new File(""); // Set your input file 
        File csvOutputFile = new File(""); // Set your Ouput File


        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(csvInputFile), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvOutputFile), StandardCharsets.UTF_8))) {


            String[] headers = reader.readLine().split(",");
            headers = Arrays.copyOf(headers, headers.length + 2);
            headers[headers.length - 2] = "Lat";
            headers[headers.length - 1] = "Lng";
            writer.write(String.join(",", headers) + "\n");

            String line;
            while ((line = reader.readLine()) != null) {
                String[] row = line.split(",");
                String nameShipToParty = row[1] + ", " + row[3] + ", Saudi Arabia";
                String street = row[2] + ", " + row[3] + ", Saudi Arabia";
                String city = row[3] + ", Saudi Arabia";

                String[] locations = {street, nameShipToParty, city};
                Double[] latLng = null;
                for (String location : locations) {
                    latLng = getGeocode(location);
                    if (latLng != null) {
                        break;
                    }
                }

                if (latLng != null) {
                    row = Arrays.copyOf(row, row.length + 2);
                    row[row.length - 2] = latLng[0].toString();
                    row[row.length - 1] = latLng[1].toString();
                } else {
                    row = Arrays.copyOf(row, row.length + 1);
                    row[row.length - 1] = "0.0000, 0.0000";
                    System.out.println("Could not get coordinates for address: " + city + ", " + nameShipToParty + ", or " + street);
                }
                writer.write(String.join(",", row) + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Double[] getGeocode(String location) {
        try {
            URIBuilder uriBuilder = new URIBuilder("https://maps.googleapis.com/maps/api/geocode/json");
            uriBuilder.addParameter("address", location);
            uriBuilder.addParameter("key", API_KEY);
            URI uri = uriBuilder.build();

            HttpRequest request = HttpRequest.newBuilder().uri(uri).GET().build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            JsonObject jsonObject = GSON.fromJson(response.body(), JsonObject.class);
            if (jsonObject.get("results").getAsJsonArray().size() > 0) {
                JsonObject results = jsonObject.get("results").getAsJsonArray().get(0).getAsJsonObject();
                String[] types = GSON.fromJson(results.get("types"), String[].class);
                if (Arrays.stream(types).anyMatch(t -> t.equals("country") || t.equals("administrative_area_level_2"))) {
                    System.out.println("Result is too general, will try next specific location.");
                } else {
                    JsonObject locationObject = results.get("geometry").getAsJsonObject().get("location").getAsJsonObject();
                    Double lat = locationObject.get("lat").getAsDouble();
                    Double lng = locationObject.get("lng").getAsDouble();
                    return new Double[]{lat, lng};
                }
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        return null;
    }
}
