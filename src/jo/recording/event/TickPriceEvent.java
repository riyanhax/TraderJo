package jo.recording.event;

import com.ib.client.TickType;

public class TickPriceEvent extends BaseEvent {
    private TickType tickType;
    private double price;
    private int canAutoExecute;

    public TickPriceEvent(TickType tickType, double price, int canAutoExecute) {
        super("TickPrice");
        this.tickType = tickType;
        this.price = price;
        this.canAutoExecute = canAutoExecute;
    }

    public TickType getTickType() {
        return tickType;
    }

    public void setTickType(TickType tickType) {
        this.tickType = tickType;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getCanAutoExecute() {
        return canAutoExecute;
    }

    public void setCanAutoExecute(int canAutoExecute) {
        this.canAutoExecute = canAutoExecute;
    }

}