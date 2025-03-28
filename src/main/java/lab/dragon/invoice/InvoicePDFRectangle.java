package lab.dragon.invoice;

import java.awt.*;


/**
 * 定义发票区域
 * @author mickey.wang
 */
public class InvoicePDFRectangle {
    /**
     * 上方中间位置 title 发票类型 电子发票（增值税专用发票）
     */
    public static final Rectangle RECTANGLE_1 = new Rectangle(158, 0, 270, 84);
    /**
     * 上方右侧位置 发票号码、开票日期、页码
     */
    public static final Rectangle RECTANGLE_2 = new Rectangle(158+270, 0, 172, 84);
    /**
     * 购买方信息
     */
    public static final Rectangle RECTANGLE_3 = new Rectangle(30, 85,266, 61);
    /**
     * 销售方信息
     */
    public static final Rectangle RECTANGLE_4 = new Rectangle(30+266+19, 85,266, 61);

}
