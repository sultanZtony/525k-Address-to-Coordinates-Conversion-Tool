package bmt;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.http.client.utils.URIBuilder;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;

public class GeocodeService {

    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final Gson GSON = new Gson();
    private static final String API_KEY = "API_Key"; // Your API Key

    public static Double[] getGeocode(String location, String city, JsonObject bounds) {
        try {
            URIBuilder uriBuilder = new URIBuilder("https://maps.googleapis.com/maps/api/geocode/json");
            uriBuilder.addParameter("address", location);
            uriBuilder.addParameter("components", "country:SA|locality:" + city);
            uriBuilder.addParameter("key", API_KEY);
    
            if (bounds != null) {
                uriBuilder.addParameter("bounds",
                        bounds.get("southwest").getAsJsonObject().get("lat").getAsString() + "," +
                                bounds.get("southwest").getAsJsonObject().get("lng").getAsString() + "|" +
                                bounds.get("northeast").getAsJsonObject().get("lat").getAsString() + "," +
                                bounds.get("northeast").getAsJsonObject().get("lng").getAsString()
                );
            }
    
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

    public static JsonObject getBounds(String city) {
        try {
            URIBuilder uriBuilder = new URIBuilder("https://maps.googleapis.com/maps/api/geocode/json");
            uriBuilder.addParameter("address", city);
            uriBuilder.addParameter("components", "country:SA");
            uriBuilder.addParameter("key", API_KEY);
            URI uri = uriBuilder.build();
    
            HttpRequest request = HttpRequest.newBuilder().uri(uri).GET().build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    
            JsonObject jsonObject = GSON.fromJson(response.body(), JsonObject.class);
            if (jsonObject.get("results").getAsJsonArray().size() > 0) {
                JsonObject results = jsonObject.get("results").getAsJsonArray().get(0).getAsJsonObject().get("geometry").getAsJsonObject().get("bounds").getAsJsonObject();
    
                JsonObject northeast = results.get("northeast").getAsJsonObject();
                JsonObject southwest = results.get("southwest").getAsJsonObject();

                JsonObject bounds = new JsonObject();
                bounds.add("northeast", northeast);
                bounds.add("southwest", southwest);
                return bounds;
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        System.out.println("Null");

        return null;
    }

    public static boolean isPointInBounds(Double[] point, JsonObject bounds) {
        return (
            bounds.get("southwest").getAsJsonObject().get("lat").getAsDouble() <= point[0] &&
            point[0] <= bounds.get("northeast").getAsJsonObject().get("lat").getAsDouble() &&
            bounds.get("southwest").getAsJsonObject().get("lng").getAsDouble() <= point[1] &&
            point[1] <= bounds.get("northeast").getAsJsonObject().get("lng").getAsDouble()
        );
    }

}
