package co.dvchain.trade.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PositionsResponse {
    @JsonProperty("assets")
    private List<Position> assets;

    @JsonProperty("cadBalance")
    private double cadBalance;

    @JsonProperty("usdBalance")
    private double usdBalance;

    @JsonProperty("usdcBalance")
    private double usdcBalance;

    @JsonProperty("usdtBalance")
    private double usdtBalance;

    public List<Position> getAssets() {
        return assets;
    }

    public double getCadBalance() {
        return cadBalance;
    }

    public double getUsdBalance() {
        return usdBalance;
    }

    public double getUsdcBalance() {
        return usdcBalance;
    }

    public double getUsdtBalance() {
        return usdtBalance;
    }

    @Override
    public String toString() {
        return "PositionsResponse{" +
                "assets=" + assets +
                ", cadBalance=" + cadBalance +
                ", usdBalance=" + usdBalance +
                ", usdcBalance=" + usdcBalance +
                ", usdtBalance=" + usdtBalance +
                '}';
    }
}
