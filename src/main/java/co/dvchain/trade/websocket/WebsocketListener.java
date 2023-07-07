package co.dvchain.trade.websocket;

import co.dvchain.trade.clientmessages.Clientmessages.ClientMessage;
import co.dvchain.trade.clientmessages.Clientmessages.LevelData;
import co.dvchain.trade.clientmessages.Clientmessages.OrderCancelled;
import co.dvchain.trade.clientmessages.Clientmessages.OrderFilled;
import co.dvchain.trade.clientmessages.Clientmessages.OrderOpened;
import co.dvchain.trade.clientmessages.Clientmessages.PricesData;

public interface WebsocketListener {
    public void onClientMessage(ClientMessage message);
    public void onLevelUpdate(LevelData levelData);
    public void onPriceUpdate(PricesData pricesData);
    public void onOrderFill(OrderFilled orderFilled);
    public void onOrderCancel(OrderCancelled orderCancelled);
    public void onOrderOpened(OrderOpened orderOpened);
}
