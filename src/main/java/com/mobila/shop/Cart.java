package com.mobila.shop;

import java.time.LocalDate;
import java.util.List;

public class Cart {
    private final List<CartLine> lines;
    private final double subtotal;
    private final double shipping;
    private final int days;
    private final double discount;        // reducere din codul promoțional
    private final String couponCode;      // null = fără cod aplicat
    private final double assembly;        // montaj inclus în total (0 dacă nu e ales)
    private final double assemblyOffer;   // costul montajului pentru coșul curent (0 = nu există piese cu montaj)
    private final boolean assemblyOn;

    public Cart(List<CartLine> lines, double subtotal, double shipping, int days) {
        this(lines, subtotal, shipping, days, 0, null, 0, 0, false);
    }

    public Cart(List<CartLine> lines, double subtotal, double shipping, int days, double discount, String couponCode,
                double assembly, double assemblyOffer, boolean assemblyOn) {
        this.lines = lines; this.subtotal = subtotal; this.shipping = shipping; this.days = days;
        this.discount = discount; this.couponCode = couponCode;
        this.assembly = assembly; this.assemblyOffer = assemblyOffer; this.assemblyOn = assemblyOn;
    }

    public List<CartLine> getLines() { return lines; }
    public double getSubtotal() { return subtotal; }
    public double getShipping() { return shipping; }
    public int getDays() { return days; }
    public double getDiscount() { return discount; }
    public String getCouponCode() { return couponCode; }
    public double getAssembly() { return assembly; }
    public double getAssemblyOffer() { return assemblyOffer; }
    public boolean isAssemblyOn() { return assemblyOn; }
    public double getTotal() { return subtotal - discount + shipping + assembly; }
    public double getFreeFrom() { return ShopService.FREE_SHIPPING; }
    /** Data estimată a livrării (ISO): cel mai lent produs din coș, +1 zi dacă e ales montajul. */
    public String getEta() { return lines.isEmpty() ? null : LocalDate.now().plusDays(days + (assemblyOn ? 1 : 0)).toString(); }
}
