package com.mobila.shop;

public class CartLine {
    private final Product product;
    private final int qty;

    public CartLine(Product product, int qty) {
        this.product = product;
        this.qty = qty;
    }

    public Product getProduct() { return product; }
    public int getQty() { return qty; }
    public double getSub() { return product.price * qty; }
}
