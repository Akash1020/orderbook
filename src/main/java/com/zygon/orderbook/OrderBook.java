
package com.zygon.orderbook;

import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.xeiam.xchange.currency.CurrencyPair;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 *
 * @author zygon
 */
public class OrderBook {
    
    private final Multimap<BigDecimal, Order> asks = Multimaps.newListMultimap(
            new TreeMap<BigDecimal, Collection<Order>>(), 
            
            new Supplier<List<Order>>() {
                @Override
                public List<Order> get() {
                    return Lists.newArrayList();
                }
            }
        );
    
    private final Multimap<BigDecimal, Order> bids = Multimaps.newListMultimap(
            new TreeMap<BigDecimal, Collection<Order>>(), 
            
            new Supplier<List<Order>>() {
                @Override
                public List<Order> get() {
                    return Lists.newArrayList();
                }
            }
        );
    
    public synchronized void addOrder(final Order order) {
        
        // TBD: determine if the order will cross over using locally cached 
        // variables.  For now just create a filter and if nothing comes back
        // then you know.
        
        final com.xeiam.xchange.dto.trade.LimitOrder.OrderType orderType = order.getType();
        Multimap<BigDecimal, Order> filterBook = null;
        final BigDecimal filterValue = order.getLimitPrice();
        
        BigDecimal fillDecrementer = order.getTradableAmount();
        
        switch (orderType) {
            case ASK:
                filterBook = this.bids;
                break;
            case BID:
                filterBook = this.asks;
                break;
        }
        
        Multimap<BigDecimal, Order> filterKeys = 
                Multimaps.filterKeys(filterBook, new Predicate<BigDecimal>() {
                    @Override
                    public boolean apply(final BigDecimal t) {
                        boolean filter = false;
                        int compare = t.compareTo(filterValue);
                        
                        switch (orderType) {
                            case ASK:
                                filter = compare >= 0;
                                break;
                            case BID:
                                filter = compare <= 0;
                                break;
                        }

                        return filter;
                    }
                });
        
        if (!filterKeys.isEmpty()) {
            System.out.println("process transactions!!");
            TreeMap<BigDecimal, Collection<Order>> filteredMap = Maps.newTreeMap();
            filteredMap.putAll(filterKeys.asMap());
            
            while (fillDecrementer.compareTo(BigDecimal.ZERO) > 0 && !filteredMap.isEmpty()) {
                
                Iterator<BigDecimal> keyIter = filteredMap.keySet().iterator();
                while (fillDecrementer.compareTo(BigDecimal.ZERO) > 0 && keyIter.hasNext()) {
                    
                    // The key
                    BigDecimal tradePrice = keyIter.next();
                    // The values for this key
                    Collection<Order> values = filteredMap.get(tradePrice);
                    // The iterator of the values
                    Iterator<Order> valIter = values.iterator();
                                
                    System.out.println("Remaining fill: " + fillDecrementer + ", looking at price " + tradePrice);
                    
                    boolean doneWithValues = false;
                    
                    while (!doneWithValues && valIter.hasNext()) {
                        Order val = valIter.next();
                        // Decide if the trade ammount is (greater than or equal 
                        // to) OR strictly less than the required fill
                        
                        System.out.println("Eligible orders:");
                        System.out.println(getMapDisplayString(orderType, filterKeys));
                        
                        BigDecimal tradableAmount = val.getTradableAmount();
                        
                        // comparing the existing order to the incoming order
                        int comparison = tradableAmount.compareTo(fillDecrementer);
                        
                        if (comparison > 0) {
                            // The existing order is larger than our incoming order.
                            // Can fill the order with left over value on the order in the collection
                            System.out.println(tradableAmount + " can fill the remaining required fill " + fillDecrementer);
                            
                            // profit value
                            BigDecimal multiply = tradePrice.multiply(fillDecrementer);
                            
                            // TODO: process a kind of 'receipt' for the original
                            // order (probably by limitOrder ID).
                            
                            val.subtractTradeable(fillDecrementer);
                            
                            fillDecrementer = BigDecimal.ZERO;
                            
                            doneWithValues = true;
                        } else if (comparison == 0) {
                            // The existing order is equal to the incoming order
                            // We can finish the order and remove the existing
                            // order.
                            System.out.println(tradableAmount + " can EXACTLY fill the remaining required fill " + fillDecrementer);
                            
                            // profit value
                            BigDecimal multiply = tradePrice.multiply(fillDecrementer);
                            
                            // TODO: process a kind of 'receipt' for the original
                            // order (probably by limitOrder ID).
                            
                            fillDecrementer = BigDecimal.ZERO;
                            
                            valIter.remove();
                            doneWithValues = true;
                        } else {
                            // The existing order cannot fill our incoming order
                            // We will use it up and remove it from the order collection
                            System.out.println(tradableAmount + " can PARTIALLY fill the remaining required fill " + fillDecrementer);
                            
                            // profit value
                            BigDecimal multiply = tradePrice.multiply(tradableAmount);
                            
                            // TODO: process a kind of 'receipt' for the original
                            // order (probably by limitOrder ID).
                            
                            fillDecrementer = fillDecrementer.subtract(tradableAmount);
                            
                            valIter.remove();
                        }
                    }
                    
                    // If we run out of values in this price, then remove the whole key
                    if (values.isEmpty()) {
                        keyIter.remove();
                    }
                }
            }
            
            System.out.println("done processing transactions!!");
            System.out.println("Unfilled order: " + fillDecrementer);
            
        } else {
            switch (orderType) {
                case ASK:
                    this.asks.put(order.getLimitPrice(), order);
                    break;
                case BID:
                    this.bids.put(order.getLimitPrice(), order);
                    break;
            }
        }
    }

    // order type for convenience - it could be discovered.
    private String getMapDisplayString(com.xeiam.xchange.dto.trade.LimitOrder.OrderType type, Multimap<BigDecimal, Order> map) {
        StringBuilder sb = new StringBuilder();
        
        sb.append(type).append("\n");
        
        for (BigDecimal key: map.keySet()) {
            sb.append(key).append(":");
            sb.append("|");
            for (Order order : map.get(key)) {
                sb.append(order.getTradableAmount());
                sb.append("|");
            }
            sb.append("\n");
        }
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        sb.append(this.getMapDisplayString(com.xeiam.xchange.dto.trade.LimitOrder.OrderType.ASK, this.asks));
        sb.append("\n\n");
        sb.append(this.getMapDisplayString(com.xeiam.xchange.dto.trade.LimitOrder.OrderType.BID, this.bids));
        
        return sb.toString();
    }
    
    
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
        
        // Cross the price barrier
        
        // allowing zero fill for testing
        book.addOrder(create(com.xeiam.xchange.dto.trade.LimitOrder.OrderType.BID, BigDecimal.valueOf(15 + new Random().nextInt(25)), BigDecimal.valueOf(10 + new Random().nextInt(8))));
        
        System.out.println(book);
    }
}
