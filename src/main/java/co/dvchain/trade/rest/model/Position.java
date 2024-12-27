package co.dvchain.trade.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Position {
    @JsonProperty("asset")
    private String asset;

    @JsonProperty("maxSell")
    private double maxSell;

    @JsonProperty("maxBuy")
    private double maxBuy;

    @JsonProperty("position")
    private double position;

    public String getAsset() {
        return asset;
    }

    public double getMaxSell() {
        return maxSell;
    }

    public double getMaxBuy() {
        return maxBuy;
    }

    public double getPosition() {
        return position;
    }

    @Override
    public String toString() {
        return "Position{" +
                "asset='" + asset + '\'' +
                ", maxSell=" + maxSell +
                ", maxBuy=" + maxBuy +
                ", position=" + position +
                '}';
    }
}
