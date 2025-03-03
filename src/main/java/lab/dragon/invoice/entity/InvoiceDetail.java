package lab.dragon.invoice.entity;

import lab.dragon.invoice.VO.InvoiceDetailVO;

import java.math.BigDecimal;

/**
 * 货物或应税劳务、服务名称
 * 数量
 * 单价
 * 金额
 * 税率
 * 税额
 */
public class InvoiceDetail {
    private String name; //货物或应税劳务、服务名称
    private String model; // 规格型号
    private String unit; //单位
    private BigDecimal count; //数量:
    private BigDecimal price; //单价:
    private BigDecimal amount; //金额:
    private BigDecimal taxRate; // 税率:
    private BigDecimal taxAmount; // 税额:

    public InvoiceDetailVO convert() {
        InvoiceDetailVO detailVO = new InvoiceDetailVO();
        detailVO.setName(this.name);
        detailVO.setModel(this.model);
        detailVO.setUnit(this.unit);
        if (this.count != null) detailVO.setCount(this.count.toString());
        if (this.price != null) detailVO.setPrice(this.price.toString());
        if (this.amount != null) detailVO.setAmount(this.amount.toString());
        if (this.taxRate != null) detailVO.setTaxRate(String.format("%d", (int)(this.taxRate.doubleValue() * 100)) + "%");
        if (this.taxAmount != null) detailVO.setTaxAmount(this.taxAmount.toString());
        if (this.amount != null) {
            if (this.taxAmount == null) detailVO.setTotalAmount(this.getAmount().toString());
            else detailVO.setTotalAmount(this.getAmount().add(this.taxAmount).toString());
        }

        return detailVO;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getCount() {
        return count;
    }

    public void setCount(BigDecimal count) {
        this.count = count;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    @Override
    public String toString() {
        return "Detail [name=" + name + ", model=" + model + ", unit=" + unit + ", count=" + count + ", price=" + price
                + ", amount=" + amount + ", taxRate=" + taxRate + ", taxAmount=" + taxAmount + "]";
    }
}