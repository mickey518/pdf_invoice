package lab.dragon.invoice.VO;


/**
 * 货物或应税劳务、服务名称
 * 数量
 * 单价
 * 金额
 * 税率
 * 税额
 */
public class InvoiceDetailVO {
    private int index;              // 序号，从 1 开始
    private String name;            //货物或应税劳务、服务名称
    private String model;           // 规格型号
    private String unit;            //单位
    private String count;           //数量:
    private String price;           //单价:
    private String amount;          //金额:
    private String taxRate;         // 税率:
    private String taxAmount;       // 税额:
    private String totalAmount;     // 单项总价 = 金额 + 税额
    private String remark;          // 备注

    @Override
    public String toString() {
        return "InvoiceDetailVO{" + "index=" + index + '\'' + "name='" + name + '\'' + ", model='" + model + '\'' + ", unit='" + unit + '\'' + ", count='" + count + '\'' + ", price='" + price + '\'' + ", amount='" + amount + '\'' + ", taxRate='" + taxRate + '\'' + ", taxAmount='" + taxAmount + '\'' + ", totalAmount='" + totalAmount + '\'' + ", remark='" + remark + '\'' + '}';
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
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

    public String getCount() {
        return count;
    }

    public void setCount(String count) {
        this.count = count;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(String taxRate) {
        this.taxRate = taxRate;
    }

    public String getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(String taxAmount) {
        this.taxAmount = taxAmount;
    }

    public String getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(String totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}