package co.dvchain.trade.rest;

import okhttp3.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;
import co.dvchain.trade.rest.model.PositionsResponse;
import co.dvchain.trade.rest.model.AuthResponse;
import co.dvchain.trade.rest.model.TradesResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

public class RestClient {
    private static final Logger logger = Logger.getLogger(RestClient.class.getName());
    
    private final String baseUrl;
    private final String apiKey;
    private final String secretKey;
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    
    private String currentToken;
    private long tokenExpiryTime;

    public RestClient(String baseUrl, String apiKey, String secretKey) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.objectMapper = new ObjectMapper();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        refreshToken();
    }

    private void refreshToken() {
        long currentTime = Instant.now().toEpochMilli();
        if (currentToken == null || currentTime >= tokenExpiryTime) {
            try {
                // Generate Basic Auth token
                String credentials = Base64.getEncoder().encodeToString(
                    String.format("%s:%s", apiKey, secretKey).getBytes(StandardCharsets.UTF_8)
                );

                String url = baseUrl + "/api/v5/auth";

                Request request = new Request.Builder()
                        .url(url)
                        .header("Authorization", "Basic " + credentials)
                        .get()
                        .build();

                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) {
                    String responseBodyString;
                    if (response.body() != null) {
                        responseBodyString = response.body().string();
                        response.body().close();
                    } else {
                        responseBodyString = "Response body was null";
                    }
                    throw new IOException("Authentication failed: " + response.code() + " - " + response.message() + "\n" + responseBodyString);
                }

                String responseBody = response.body().string();
                response.body().close();
                AuthResponse authResponse = objectMapper.readValue(responseBody, AuthResponse.class);
                currentToken = authResponse.getToken();
                tokenExpiryTime = Instant.now().plusSeconds(86400 - 60).toEpochMilli();
                logger.info("Authentication token refreshed. Valid until: " + Instant.ofEpochMilli(tokenExpiryTime));
            } catch (Exception e) {
                logger.severe("Failed to refresh authentication token: " + e.getMessage());
                throw new RuntimeException("Failed to refresh authentication token", e);
            }
        }
    }

    public Response executeRequest(Request request) throws IOException {
        refreshToken();
        Request authenticatedRequest = request.newBuilder()
                .header("Authorization", "Bearer " + currentToken)
                .build();
        return client.newCall(authenticatedRequest).execute();
    }

    private <T> T parseResponse(Response response, Class<T> responseType) throws IOException {
        if (!response.isSuccessful()) {
            String responseBodyString;
            if (response.body() != null) {
                responseBodyString = response.body().string();
                response.body().close();
            } else {
                responseBodyString = "Response body was null";
            }
            throw new IOException("Unexpected response code: " + response.code() + " - " + response.message() + "\n" + responseBodyString);
        }
        String responseBody = response.body().string();
        response.body().close();
        return objectMapper.readValue(responseBody, responseType);
    }

    public Response get(String endpoint) throws IOException {
        Request request = new Request.Builder()
                .url(baseUrl + endpoint)
                .get()
                .build();
        return executeRequest(request);
    }

    public Response post(String endpoint, RequestBody body) throws IOException {
        Request request = new Request.Builder()
                .url(baseUrl + endpoint)
                .post(body)
                .build();
        return executeRequest(request);
    }

    public Response put(String endpoint, RequestBody body) throws IOException {
        Request request = new Request.Builder()
                .url(baseUrl + endpoint)
                .put(body)
                .build();
        return executeRequest(request);
    }

    public Response delete(String endpoint) throws IOException {
        Request request = new Request.Builder()
                .url(baseUrl + endpoint)
                .delete()
                .build();
        return executeRequest(request);
    }

    public PositionsResponse getPositions() throws IOException {
        Response response = get("/api/v4/balances");
        return parseResponse(response, PositionsResponse.class);
    }

    public TradesResponse getTrades(Long afterTimestamp, String status) throws IOException {
        StringBuilder endpoint = new StringBuilder("/api/v4/trades");
        boolean hasParams = false;
        
        if (afterTimestamp != null) {
            endpoint.append("?after=").append(afterTimestamp);
            hasParams = true;
        }
        
        if (status != null) {
            endpoint.append(hasParams ? "&" : "?").append("status=").append(status);
            hasParams = true;
        }

        endpoint.append(hasParams ? "&" : "?").append("timetype=filled");
        
        Response response = get(endpoint.toString());
        try {
            return parseResponse(response, TradesResponse.class);
        } finally {
            if (response.body() != null) {
                response.body().close();
            }
        }
    }
}
