package co.dvchain.trade.websocket;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class WebsocketClientTest {
    @Test
    public void testConnect() {
        OkHttpClient mockClient = mock(OkHttpClient.class);
        WebSocket mockSocket = mock(WebSocket.class);
        when(mockClient.newWebSocket(any(Request.class), any(WebSocketListener.class))).thenReturn(mockSocket);
        WebsocketClient client = new WebsocketClient("ws://sandbox.trade.dvchain.co/ws", "183c5515-a3de-44a1-bdeb-cf70aab4c2c6", "126d644fc423bf8dbf1d56027bb7edf3");
        client.setClient(mockClient); // you would have to expose a setter for this
        client.connect();
        verify(mockClient).newWebSocket(any(Request.class), any(WebSocketListener.class));
    }
    
}
