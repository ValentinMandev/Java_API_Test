package common;

public class Order {
    private final String symbol;
    private final int amount;
    private final String side;
    private final String tif;

    public Order(String symbol, int amount, String side, String tif) {
        this.symbol = symbol;
        this.amount = amount;
        this.side = side;
        this.tif = tif;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getAmount() {
        return amount;
    }

    public String getSide() {
        return side;
    }

    public String getTif() {
        return tif;
    }

}
