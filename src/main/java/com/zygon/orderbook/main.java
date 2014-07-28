package com.zygon.orderbook;

import com.xeiam.xchange.currency.CurrencyPair;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Random;

public class main {
    /*
     * So.. order book
     * * Need at least two sorted queues, sorted by price
     *   possibly need other queues with the same data but 
     *   indexed differently
     * * Need some transaction type information (type, ammount, etc)
     * * 
     * 
     */
    
    ///// helper functions for testing /////
    
    private static int ID = 0;
    private static Order create (com.xeiam.xchange.dto.trade.LimitOrder.OrderType type, BigDecimal ammount, BigDecimal price) {
        ID++;
        return new Order(new com.xeiam.xchange.dto.trade.LimitOrder(type, ammount, CurrencyPair.BTC_USD, String.valueOf(ID), new Date(), price));
    }
    
    public static void main(String[] args) {
        OrderBook book = new OrderBook();
        
        for (int i = 10; i < 20; i++) {
            for (int j = 1; j < 1 + new Random().nextInt(5); j++) {
                book.addOrder(create(com.xeiam.xchange.dto.trade.LimitOrder.OrderType.ASK, BigDecimal.valueOf(new Random().nextInt(10)), BigDecimal.valueOf(i)));
            }
//            System.out.println(book);
        }
        
        
        for (int i = 1; i < 9; i++) {
            for (int j = 1; j < 1 + new Random().nextInt(5); j++) {
                book.addOrder(create(com.xeiam.xchange.dto.trade.LimitOrder.OrderType.BID, BigDecimal.valueOf(new Random().nextInt(10)), BigDecimal.valueOf(i)));
            }
//            System.out.println(book);
        }
        
        System.out.println(book);
        
        for (int j = 0; j < ID; j++) {
            if (new Random().nextInt(10) < 1) {
                book.cancelOrder(String.valueOf(j));
            }
        }
        
        System.out.println(book);
        
        // Cross the price barrier
        
        // allowing zero fill for testing
        book.addOrder(create(com.xeiam.xchange.dto.trade.LimitOrder.OrderType.BID, BigDecimal.valueOf(15 + new Random().nextInt(25)), BigDecimal.valueOf(10 + new Random().nextInt(8))));
        
        System.out.println(book);
    }
}
