package co.dvchain.trade.websocket;

import okhttp3.*;
import okio.ByteString;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import co.dvchain.trade.clientmessages.Clientmessages.ClientMessage;
import co.dvchain.trade.clientmessages.Clientmessages.CreateOrderMessage;
import co.dvchain.trade.clientmessages.Clientmessages.LimitsResponse;
import co.dvchain.trade.clientmessages.Clientmessages.OrderSide;
import co.dvchain.trade.clientmessages.Clientmessages.OrderType;
import co.dvchain.trade.clientmessages.Clientmessages.StatusMessage;
import co.dvchain.trade.clientmessages.Clientmessages.TradeStatusResponse;
import co.dvchain.trade.clientmessages.Clientmessages.Types;
import java.util.logging.Logger;


public class WebsocketClient {
    private final static Logger logger = Logger.getLogger(WebsocketClient.class.getName());
    private String WS_URL;
    private int RECONNECT_DELAY = 5; // in seconds
    private long TIME_WINDOW = 60000; // 1 minute
    private String API_KEY;
    private String SECRET_KEY;;

    private OkHttpClient client;
    private WebSocket webSocket;
    private boolean isConnected;
    private WebsocketListener listener;

    private final ConcurrentHashMap<String, CompletableFuture<? extends Object>> responseFutures = new ConcurrentHashMap<>();

    private ArrayList<String> levelSubscriptions = new ArrayList<String>();
    private ArrayList<String> priceSubscriptions = new ArrayList<String>();;

    public WebsocketClient(String url, String apiKey, String secretKey) {
        this.WS_URL = url;
        this.API_KEY = apiKey;
        this.SECRET_KEY = secretKey;
        client = new OkHttpClient.Builder()
                .pingInterval(10, TimeUnit.SECONDS)
                .build();
    }

    public void setMessageHandler(WebsocketListener listener) {
        this.listener = listener;
    }

    String generateSignature(String apiKey, String secretKey, long timeWindow) {
        long timestamp = System.currentTimeMillis();
        String message = apiKey + timestamp + timeWindow;
        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKeySpec);
            byte[] signatureBytes = sha256Hmac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            String signature = Base64.getEncoder().encodeToString(signatureBytes);
            return signature;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void connect() {
        logger.info("Connecting to websocket "+WS_URL);
        Request request = new Request.Builder()
                .url(WS_URL)
                .header("DV-API-KEY", API_KEY)
                .header("DV-TIMESTAMP", String.valueOf(System.currentTimeMillis()))
                .header("DV-TIMEWINDOW", String.valueOf(TIME_WINDOW))
                .header("DV-SIGNATURE", generateSignature(API_KEY, SECRET_KEY, TIME_WINDOW))
                .build();

        WebSocketListener listener = new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                logger.info("Connected to websocket");
                isConnected = true;
                subscribeNotifications();
                for (String symbol : levelSubscriptions) {
                    sendLevelSubscription(symbol);
                }
                for (String symbol : priceSubscriptions) {
                    sendPriceSubscription(symbol);
                }
            }

            private void subscribeNotifications() {
                ClientMessage message = ClientMessage.newBuilder()
                        .setType(Types.subscribe)
                        .setEvent("notifications")
                        .setTopic("ORDER_FILLED")
                        .build();
                webSocket.send(ByteString.of(message.toByteArray()));
                ClientMessage message1 = ClientMessage.newBuilder()
                        .setType(Types.subscribe)
                        .setEvent("notifications")
                        .setTopic("ORDER_CANCELLED")
                        .build();
                webSocket.send(ByteString.of(message1.toByteArray()));
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                try {
                    ClientMessage message = ClientMessage.parseFrom(bytes.toByteArray());
                    if (WebsocketClient.this.listener != null) {
                        WebsocketClient.this.listener.onClientMessage(message);
                    }
                    if (message.getType() == Types.subscribe) {
                        String event = message.getEvent();
                        switch (event) {
                            case "levels":
                                if (WebsocketClient.this.listener != null) {
                                    WebsocketClient.this.listener.onLevelUpdate(message.getLevelData());
                                }
                                break;
                            case "prices":
                                if (WebsocketClient.this.listener != null) {
                                        WebsocketClient.this.listener.onPriceUpdate(message.getPricesData());
                                }
                                break;
                            case "notifications":
                                if (WebsocketClient.this.listener != null) {
                                    String notificationType = message.getTopic();
                                    switch (notificationType) {
                                        case "ORDER_FILLED":
                                            WebsocketClient.this.listener.onOrderFill(message.getNotification().getOrderFilled());
                                            break;
                                        case "ORDER_CANCELLED":
                                            WebsocketClient.this.listener.onOrderCancel(message.getNotification().getOrderCancelled());
                                            break;
                                    }
                                }
                                break;
                            default:
                                System.out.println("Unknown event type: " + event);
                        }
                    } else if (message.getType() == Types.requestresponse) {
                        System.out.println("Received response for request id: " + message);
                        String requestId = message.getEvent();
                        CompletableFuture<?> future = responseFutures.get(requestId);
                        if (future != null) {
                            if (message.hasErrorMessage()) {
                                future.completeExceptionally(new Exception(message.getErrorMessage().getMessage()));
                                return;
                            }
                            if (message.getTopic() == "error") {
                                future.completeExceptionally(new Exception(message.getErrorMessage().getMessage()));
                                return;
                            }
                            switch(message.getTopic()) {
                                case "createorder":
                                    completeResponse(future, message.getTradeStatusResponse());
                                    break;
                                case "limits":
                                    completeResponse(future, message.getLimitResponse());
                                    break;
                                default:
                                    if (message.getTopic().startsWith("cancelorder/")) {
                                        completeResponse(future, message.getTradeStatusResponse());
                                    } else {
                                        future.completeExceptionally(new Exception("invalid response received for request id: " + requestId));
                                    }
                            }
                            responseFutures.remove(requestId);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                isConnected = false;
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                isConnected = false;
                logger.warning("Connection closed "+ code +" "+ reason);
                reconnect();
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                logger.warning("Connection failed "+ t.getMessage());
                isConnected = false;
                reconnect();
            }
        };

        webSocket = client.newWebSocket(request, listener);
    }

    public void subscribeLevel(String symbol) {
        levelSubscriptions.add(symbol);
        sendLevelSubscription(symbol);
    }

    private void sendLevelSubscription(String symbol) {
        if (webSocket != null && isConnected) {
            ClientMessage subscriptionMessage = ClientMessage.newBuilder()
                    .setType(Types.subscribe)
                    .setEvent("levels")
                    .setTopic(symbol)
                    .build();

            byte[] message = subscriptionMessage.toByteArray();
            ByteString socketMessage = new ByteString(message);
            System.out.println("Sending message: levels" + symbol);
            webSocket.send(socketMessage);
        }
    }

    public void subscribePrices(String symbol) {
        priceSubscriptions.add(symbol);
        sendPriceSubscription(symbol);
    }

    private void sendPriceSubscription(String symbol) {
        if (webSocket != null && isConnected) {
            ClientMessage subscriptionMessage = ClientMessage.newBuilder()
                    .setType(Types.subscribe)
                    .setEvent("prices")
                    .setTopic(symbol)
                    .build();

            byte[] message = subscriptionMessage.toByteArray();
            ByteString socketMessage = new ByteString(message);
            System.out.println("Sending message: prices" + symbol);
            webSocket.send(socketMessage);
        }
    }

    private void reconnect() {
        try {
            logger.info("Reconnecting in " + RECONNECT_DELAY + " seconds");
            Thread.sleep(RECONNECT_DELAY * 1000);
            connect();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean sendClientMessage(ClientMessage message) {
        if (webSocket != null && isConnected) {
            webSocket.send(ByteString.of(message.toByteArray()));
            return true;
        }
        return false;
    }

    public CompletableFuture<TradeStatusResponse> sendMarketOrder(String quote_id, String asset, String counterAsset, Double price, String side, Double quantity, String clientTag) {
        UUID uuid = UUID.randomUUID();
        CompletableFuture<TradeStatusResponse> future = new CompletableFuture<>();
        responseFutures.put(uuid.toString(), future);
        OrderSide orderSide = side.trim().toUpperCase().equals("BUY") ? OrderSide.Buy : OrderSide.Sell;
        if (webSocket != null && isConnected) {
            CreateOrderMessage order = CreateOrderMessage.newBuilder()
                    .setPrice(price)
                    // .setLimitPrice(null)
                    .setQty(quantity)
                    .setQuoteId(quote_id)
                    .setSide(orderSide)
                    .setOrderType(OrderType.MARKET)
                    .setAsset(asset)
                    .setCounterAsset(counterAsset)
                    .setClientTag(clientTag)
                    .setExpires(-1)
                    .build();
            ClientMessage orderMessage = ClientMessage.newBuilder()
                    .setType(Types.requestresponse)
                    .setEvent(uuid.toString())
                    .setTopic("createorder")
                    .setCreateOrderRequest(order)
                    .build();

            byte[] message = orderMessage.toByteArray();
            ByteString socketMessage = new ByteString(message);
            logger.info("Sending market order: " + quote_id + " " + asset + " " + counterAsset + " " + price + " " + side.toUpperCase() + " " + quantity + " " + clientTag);
            webSocket.send(socketMessage);
        } else {
            future.completeExceptionally(new Exception("Connection not available"));
        }
        return future;
    }

    public CompletableFuture<TradeStatusResponse> sendLimitOrder(String asset, String counterAsset, Double price, String side, Double quantity, String clientTag) {
        UUID uuid = UUID.randomUUID();
        CompletableFuture<TradeStatusResponse> future = new CompletableFuture<>();
        responseFutures.put(uuid.toString(), future);
        OrderSide orderSide = side.trim().toUpperCase().equals("BUY") ? OrderSide.Buy : OrderSide.Sell;
        if (webSocket != null && isConnected) {
            CreateOrderMessage order = CreateOrderMessage.newBuilder()
                    .setLimitPrice(price)
                    .setQty(quantity)
                    .setSide(orderSide)
                    .setOrderType(OrderType.LIMIT)
                    .setAsset(asset)
                    .setCounterAsset(counterAsset)
                    .setClientTag(clientTag)
                    .setExpires(-1)
                    .build();
            ClientMessage orderMessage = ClientMessage.newBuilder()
                    .setType(Types.requestresponse)
                    .setEvent(uuid.toString())
                    .setTopic("createorder")
                    .setCreateOrderRequest(order)
                    .build();

            byte[] message = orderMessage.toByteArray();
            ByteString socketMessage = new ByteString(message);
            logger.info("Sending limit order: " + asset + " " + counterAsset + " " + price + " " + side.toUpperCase() + " " + quantity + " " + clientTag);
            webSocket.send(socketMessage);
        } else {
            future.completeExceptionally(new Exception("Connection not available"));
        }
        return future;
    }

    public CompletableFuture<LimitsResponse> getLimits() {
        UUID uuid = UUID.randomUUID();
        CompletableFuture<LimitsResponse> future = new CompletableFuture<>();
        responseFutures.put(uuid.toString(), future);
        if (webSocket != null && isConnected) {
            ClientMessage orderMessage = ClientMessage.newBuilder()
                    .setType(Types.requestresponse)
                    .setEvent(uuid.toString())
                    .setTopic("limits")
                    .build();

            byte[] message = orderMessage.toByteArray();
            ByteString socketMessage = new ByteString(message);
            logger.info("send limits: ");
            webSocket.send(socketMessage);
        } else {
            future.completeExceptionally(new Exception("Connection not available"));
        }
        return future;
    }

    public CompletableFuture<TradeStatusResponse> cancelOrder(String orderId) {
        UUID uuid = UUID.randomUUID();
        CompletableFuture<TradeStatusResponse> future = new CompletableFuture<>();
        responseFutures.put(uuid.toString(), future);
        if (webSocket != null && isConnected) {
            ClientMessage cancelMessage = ClientMessage.newBuilder()
                    .setType(Types.requestresponse)
                    .setEvent(uuid.toString())
                    .setTopic("cancelorder/" + orderId)
                    .build();

            byte[] message = cancelMessage.toByteArray();
            ByteString socketMessage = new ByteString(message);
            logger.info("Sending cancel order request for order: " + orderId);
            webSocket.send(socketMessage);
        } else {
            future.completeExceptionally(new Exception("Connection not available"));
        }
        return future;
    }

    @SuppressWarnings("unchecked")
    private <T> void completeResponse(CompletableFuture<?> future, T response) {
        ((CompletableFuture<T>) future).complete(response);
    }

    public void setClient(OkHttpClient client) {
        this.client = client;
    }

    public OkHttpClient getClient() {
        return client;
    }
}
