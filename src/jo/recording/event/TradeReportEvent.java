package jo.recording.event;

import com.ib.client.Contract;
import com.ib.client.Execution;

public class TradeReportEvent extends BaseEvent {
    private String tradeKey;
    private Contract contract;
    private Execution execution;

    public TradeReportEvent(String tradeKey, Contract contract, Execution execution) {
        super("TradeReport");
        this.tradeKey = tradeKey;
        this.contract = contract;
        this.execution = execution;
    }

    public String getTradeKey() {
        return tradeKey;
    }

    public void setTradeKey(String tradeKey) {
        this.tradeKey = tradeKey;
    }

    public Contract getContract() {
        return contract;
    }

    public void setContract(Contract contract) {
        this.contract = contract;
    }

    public Execution getExecution() {
        return execution;
    }

    public void setExecution(Execution execution) {
        this.execution = execution;
    }
}
