import com.google.gson.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class GoogleBusinessProfileClient {

    private static final String CLIENT_ID = "";
    private static final String CLIENT_SECRET = "";
    private static final String REFRESH_TOKEN = "";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String locationId = ""; // Look below to see how we can get locationId
    private static final int NUMBER_OF_REVIEWS = 10;

    private static final HttpClient httpClient = HttpClient.newHttpClient();

    public static void main(String[] args) {
        try {
            // Step 1: Get access token using refresh token
            String accessToken = getAccessTokenFromRefreshToken();
            System.out.println("Access Token: " + accessToken);

            // Step 2: Get all accounts
            System.out.println("\n--- ACCOUNTS ---");
            JsonObject accountsResponse = fetchAccounts(accessToken);
            if (accountsResponse != null && accountsResponse.has("accounts")) {
                JsonArray accounts = accountsResponse.getAsJsonArray("accounts");
                System.out.println("Found " + accounts.size() + " accounts:");

                // Process each account
                for (int i = 0; i < accounts.size(); i++) {
                    JsonObject account = accounts.get(i).getAsJsonObject();
                    String accountName = account.get("name").getAsString();
                    String accountDisplayName = account.get("accountName").getAsString();

                    System.out.println("\nAccount " + (i + 1) + ": " + accountDisplayName);
                    System.out.println("Account ID: " + accountName);

                    // Step 3: Get locations for this account
//                    System.out.println("\n--- LOCATIONS FOR " + accountDisplayName + " ---");
                    JsonObject locationsResponse = fetchLocations(accessToken, accountName);

                    if (locationsResponse != null && locationsResponse.has("locations")) {
                        JsonArray locations = locationsResponse.getAsJsonArray("locations");
                        System.out.println("Found " + locations.size() + " locations:");

                        // Process each location
                        for (int j = 0; j < locations.size(); j++) {
                            JsonObject location = locations.get(j).getAsJsonObject();
                            String locationName = location.get("name").getAsString();
                            String locationTitle = location.has("title") ? location.get("title").getAsString() : "Unnamed Location";

                            System.out.println("\nLocation " + (j + 1) + ": " + locationTitle);
                            System.out.println("Location ID: " + locationName);

                            // Step 4: Get reviews for this location
                            System.out.println("\n--- REVIEWS FOR " + locationTitle + " ---");
                            JsonObject reviewsResponse = fetchReviews(accessToken, accountName, locationName);

                            if (reviewsResponse != null && reviewsResponse.has("reviews")) {
                                JsonArray reviews = reviewsResponse.getAsJsonArray("reviews");
                                System.out.println("Found " + reviews.size() + " reviews:");

                                // Print NUMBER_OF_REVIEWS
                                for (int k = 0; k < NUMBER_OF_REVIEWS; k++) {
                                    JsonObject review = reviews.get(k).getAsJsonObject();

                                    String reviewer = review.has("reviewer") && review.getAsJsonObject("reviewer").has("displayName")
                                            ? review.getAsJsonObject("reviewer").get("displayName").getAsString()
                                            : "Anonymous";

                                    String rating = review.has("starRating") ? review.get("starRating").getAsString() : "No rating";
                                    String comment = review.has("comment") ? review.get("comment").getAsString() : "No comment";
                                    String createTime = review.has("createTime") ? review.get("createTime").getAsString() : "Unknown date";

                                    System.out.println("\nReview " + (k + 1) + ":");
                                    System.out.println("Reviewer: " + reviewer);
                                    System.out.println("Rating: " + rating);
                                    System.out.println("Date: " + createTime);
                                    System.out.println("Comment: " + comment);
                                }
                            } else {
                                System.out.println("No reviews found or error fetching reviews.");
                            }
                        }
                    } else {
                        System.out.println("No locations found or error fetching locations.");
                    }
                }
            } else {
                System.out.println("No accounts found or error fetching accounts.");
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Gets an access token using the refresh token
     */
    private static String getAccessTokenFromRefreshToken() throws IOException {
        URL url = new URL(TOKEN_URL);
        String params = "client_id=" + CLIENT_ID +
                "&client_secret=" + CLIENT_SECRET +
                "&refresh_token=" + REFRESH_TOKEN +
                "&grant_type=refresh_token";

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(params.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        if (conn.getResponseCode() == 200) {
            StringBuilder response = new StringBuilder();
            try (Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8.name())) {
                while (scanner.hasNextLine()) {
                    response.append(scanner.nextLine());
                }
            }

            JsonObject jsonResponse = JsonParser.parseString(response.toString()).getAsJsonObject();
            return jsonResponse.get("access_token").getAsString();
        } else {
            StringBuilder errorResponse = new StringBuilder();
            try (Scanner scanner = new Scanner(conn.getErrorStream(), StandardCharsets.UTF_8.name())) {
                while (scanner.hasNextLine()) {
                    errorResponse.append(scanner.nextLine());
                }
            }

            System.out.println("Error getting access token: " + conn.getResponseCode());
            System.out.println("Error details: " + errorResponse.toString());
            throw new IOException("Failed to get access token. HTTP error code: " + conn.getResponseCode());
        }
    }

    /**
     * Fetches all accounts associated with the authenticated user
     */
    private static JsonObject fetchAccounts(String accessToken) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://mybusinessaccountmanagement.googleapis.com/v1/accounts"))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return JsonParser.parseString(response.body()).getAsJsonObject();
        } else {
            System.out.println("Error fetching accounts: " + response.statusCode());
            System.out.println("Response: " + response.body());
            return null;
        }
    }

    /**
     * Fetches all locations associated with an account
     */
    private static JsonObject fetchLocations(String accessToken, String accountName) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://mybusinessbusinessinformation.googleapis.com/v1/" + accountName + "/locations?read_mask=name,title"))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return JsonParser.parseString(response.body()).getAsJsonObject();
        } else {
            System.out.println("Error fetching locations: " + response.statusCode());
            System.out.println("Response: " + response.body());
            return null;
        }
    }

    /**
     * Fetches all reviews for a location
     */
    private static JsonObject fetchReviews(String accessToken, String accountId, String locationName) throws IOException, InterruptedException {
        String apiUrl = "https://mybusiness.googleapis.com/v4/" + accountId + "/" + locationName + "/reviews";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return JsonParser.parseString(response.body()).getAsJsonObject();
        } else {
            System.out.println("Error fetching reviews: " + response.statusCode());
            System.out.println("Response: " + response.body());
            return null;
        }
    }

}