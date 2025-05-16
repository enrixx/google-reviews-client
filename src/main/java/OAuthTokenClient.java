import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class OAuthTokenClient {

    private static final String CLIENT_ID = "";
    private static final String CLIENT_SECRET = "";
    private static final String REDIRECT_URI = ""; // configured redirect URI on Google Cloud
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";

    public static void main(String[] args) {
        try {
            // Step 1: Get authorization code manually through browser flow
            System.out.println("Visit this URL in your browser:");
            System.out.println(getAuthorizationUrl());
            System.out.println("\nAfter authorization, you'll be redirected to your redirect URI.");
            System.out.println("Copy the 'code' parameter from the URL and paste it here:");

            Scanner scanner = new Scanner(System.in);
            String authCode = scanner.nextLine().trim();
            scanner.close();

            // Step 2: Exchange authorization code for tokens
            String tokenResponse = getAccessToken(authCode);
            System.out.println("\nToken Response:");
            System.out.println(tokenResponse);

            // Parse and display token information
            JsonObject jsonResponse = new Gson().fromJson(tokenResponse, JsonObject.class);

            if (jsonResponse.has("access_token")) {
                System.out.println("\nAccess Token: " + jsonResponse.get("access_token").getAsString());
                System.out.println("Token Type: " + jsonResponse.get("token_type").getAsString());
                System.out.println("Expires In: " + jsonResponse.get("expires_in").getAsString() + " seconds");

                if (jsonResponse.has("refresh_token")) {
                    System.out.println("\nRefresh Token: " + jsonResponse.get("refresh_token").getAsString());
                    System.out.println("\nSave this refresh token! You can use it to generate new access tokens without user interaction.");
                } else {
                    System.out.println("\nNo refresh token received. Make sure to include 'access_type=offline' in the authorization URL.");
                }
            } else if (jsonResponse.has("error")) {
                System.out.println("\nError: " + jsonResponse.get("error").getAsString());
                System.out.println("Error Description: " + jsonResponse.get("error_description").getAsString());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Generates the authorization URL for the OAuth flow.
     */
    private static String getAuthorizationUrl() {
        return "https://accounts.google.com/o/oauth2/auth" +
                "?client_id=" + CLIENT_ID +
                "&redirect_uri=" + REDIRECT_URI +
                "&scope=https://www.googleapis.com/auth/business.manage" +
                "&response_type=code" +
                "&access_type=offline" +
                "&prompt=consent";
    }

    /**
     * Exchanges an authorization code for access and refresh tokens.
     */
    private static String getAccessToken(String authorizationCode) throws IOException {
        URL url = new URL(TOKEN_URL);
        String params = "client_id=" + CLIENT_ID +
                "&client_secret=" + CLIENT_SECRET +
                "&code=" + authorizationCode +
                "&redirect_uri=" + REDIRECT_URI +
                "&grant_type=authorization_code";

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(params.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        StringBuilder response = new StringBuilder();
        try (Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8.name())) {
            while (scanner.hasNextLine()) {
                response.append(scanner.nextLine());
            }
        } catch (IOException e) {
            // Handle error response
            try (Scanner scanner = new Scanner(conn.getErrorStream(), StandardCharsets.UTF_8.name())) {
                while (scanner.hasNextLine()) {
                    response.append(scanner.nextLine());
                }
            }
        }

        return response.toString();
    }
}