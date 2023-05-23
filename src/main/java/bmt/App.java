package bmt;

import com.google.gson.JsonObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class App {

    public static void main(String[] args) {
        File csvInputFile = new File(""); // Set your input file 
        File csvOutputFile = new File(""); // Set your Ouput File


        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(csvInputFile), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvOutputFile), StandardCharsets.UTF_8))) {


            String[] headers = reader.readLine().split(",");
            headers = Arrays.copyOf(headers, headers.length + 3);
            headers[headers.length - 3] = "Lat";
            headers[headers.length - 2] = "Lng";
            headers[headers.length - 1] = "In_Bounds";
            writer.write(String.join(",", headers) + "\n");

            String line;
            while ((line = reader.readLine()) != null) {
                String[] row = line.split(",");
                String city = row[3];

                JsonObject cityBounds = GeocodeService.getBounds(city);
                if (cityBounds == null) {
                    System.out.println("Could not get bounds for city: " + city);
                    continue;
                }
                                double distance = DistanceService.haversineDistance(
                    cityBounds.get("southwest").getAsJsonObject().get("lat").getAsDouble(), 
                    cityBounds.get("southwest").getAsJsonObject().get("lng").getAsDouble(),
                    cityBounds.get("northeast").getAsJsonObject().get("lat").getAsDouble(), 
                    cityBounds.get("northeast").getAsJsonObject().get("lng").getAsDouble()
                );
                

                String street = row[2] + ", " + city;
                String nameShipToParty = row[1] + ", " + city;

                String[] locations = {street, nameShipToParty};
                Double[] latLng = null;
                for (String location : locations) {
                    latLng = GeocodeService.getGeocode(location,row[3] ,cityBounds);
                    if (latLng != null) {
                        break;
                    }
                }

                if (latLng != null) {
                    row = Arrays.copyOf(row, row.length + 3);
                    row[row.length - 3] = latLng[0].toString();
                    row[row.length - 2] = latLng[1].toString();
                    boolean inBounds = GeocodeService.isPointInBounds(latLng, cityBounds);
                    row[row.length - 1] = Boolean.toString(inBounds);
                    String checkStatus = "Check";
                    if (distance <= 150 || row[3].equals("Ahsa")) {
                        checkStatus = "OK";
                    }
                    row[row.length - 1] = checkStatus;
                } else {
                    row = Arrays.copyOf(row, row.length + 3);
                    row[row.length - 3] = "0.0000";
                    row[row.length - 2] = "0.0000";
                    row[row.length - 1] = "False";
                    System.out.println("Could not get coordinates for address: " + city + ", " + nameShipToParty + ", or " + street);
                }
                writer.write(String.join(",", row) + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}
