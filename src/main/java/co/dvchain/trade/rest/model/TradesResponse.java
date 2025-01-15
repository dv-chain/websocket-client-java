package co.dvchain.trade.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TradesResponse {
    @JsonProperty("data")
    private List<Trade> data;
    
    private int total;
    private int pageCount;

    public List<Trade> getData() {
        return data;
    }

    public void setData(List<Trade> data) {
        this.data = data;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Trade {
        @JsonProperty("_id")
        private String id;
        private String createdAt;
        private double price;
        private double quantity;
        private String side;
        private String tradeKey;
        private String clientTag;
        private User user;
        private String asset;
        private String counterAsset;
        private String status;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }

        public double getPrice() {
            return price;
        }

        public void setPrice(double price) {
            this.price = price;
        }

        public double getQuantity() {
            return quantity;
        }

        public void setQuantity(double quantity) {
            this.quantity = quantity;
        }

        public String getSide() {
            return side;
        }

        public void setSide(String side) {
            this.side = side;
        }

        public String getTradeKey() {
            return tradeKey;
        }

        public void setTradeKey(String tradeKey) {
            this.tradeKey = tradeKey;
        }

        public String getClientTag() {
            return clientTag;
        }

        public void setClientTag(String clientTag) {
            this.clientTag = clientTag;
        }

        public User getUser() {
            return user;
        }

        public void setUser(User user) {
            this.user = user;
        }

        public String getAsset() {
            return asset;
        }

        public void setAsset(String asset) {
            this.asset = asset;
        }

        public String getCounterAsset() {
            return counterAsset;
        }

        public void setCounterAsset(String counterAsset) {
            this.counterAsset = counterAsset;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class User {
        @JsonProperty("_id")
        private String id;
        private String firstName;
        private String lastName;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }
    }
}
