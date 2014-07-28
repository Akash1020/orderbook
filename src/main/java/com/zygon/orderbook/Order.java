
package com.zygon.orderbook;

import com.xeiam.xchange.dto.trade.LimitOrder;
import java.math.BigDecimal;

/**
 *
 * @author zygon
 */
public class Order {
    
    private final LimitOrder originalLimitOrder;
    private LimitOrder mutableLimitOrder = null;

    public Order(LimitOrder limitOrder) {
        this.originalLimitOrder = limitOrder;
        this.mutableLimitOrder = this.originalLimitOrder;
    }
    
    public void addTradeable (BigDecimal ammount) {
        this.mutableLimitOrder = new LimitOrder(this.mutableLimitOrder.getType(), 
                this.mutableLimitOrder.getTradableAmount().add(ammount), 
                this.mutableLimitOrder.getCurrencyPair(), this.mutableLimitOrder.getId(), 
                this.mutableLimitOrder.getTimestamp(), this.mutableLimitOrder.getLimitPrice());
    }
    
    public String getId() {
        return this.mutableLimitOrder.getId();
    }
    
    public BigDecimal getLimitPrice() {
        return this.mutableLimitOrder.getLimitPrice();
    }
    
    public com.xeiam.xchange.dto.trade.LimitOrder.OrderType getType() {
        return this.mutableLimitOrder.getType();
    }
    
    public BigDecimal getTradableAmount() {
        return this.mutableLimitOrder.getTradableAmount();
    }
    
    public void subtractTradeable (BigDecimal ammount) {
        this.mutableLimitOrder = new LimitOrder(this.mutableLimitOrder.getType(), 
                this.mutableLimitOrder.getTradableAmount().subtract(ammount), 
                this.mutableLimitOrder.getCurrencyPair(), this.mutableLimitOrder.getId(), 
                this.mutableLimitOrder.getTimestamp(), this.mutableLimitOrder.getLimitPrice());
    }
}
