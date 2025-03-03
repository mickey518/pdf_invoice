package lab.dragon.invoice.service;

import lab.dragon.invoice.entity.Invoice;
import lab.dragon.invoice.entity.InvoiceDetail;
import lab.dragon.invoice.utils.CollectionUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PdfFullElectronicInvoiceService
 * 全电发票处理
 */
public class PdfFullElectronicInvoiceService {

    private static final Logger log = LoggerFactory.getLogger(PdfFullElectronicInvoiceService.class);

    /**
     * 全电发票处理
     *
     * @return
     */
    public static Invoice getFullElectronicInvoice(String fullText, String allText, int pageWidth, PDDocument doc, PDPage firstPage) throws IOException {

        int pages = 1;
        {
            // 识别总页数
            String reg = "共(?<pages>\\d{1})页";

            Pattern pattern = Pattern.compile(reg);
            Matcher matcher = pattern.matcher(allText);
            while (matcher.find()) {
                if (matcher.group("pages") != null) {
                    pages = (Integer.parseInt(matcher.group("pages")));
                }
            }
        }
        log.info("page size: {}", pages);

        // 将 allText 按照 “电子发票” 分割
        String[] texts = allText.split("电子发票");

        Invoice invoice = extractFirstPage("电子发票" + texts[1], fullText, doc, doc.getPage(0));
//        Invoice invoice2 = extractFirstPage("电子发票" + texts[2], fullText, doc, doc.getPage(1));
//        log.info("invoice 2: {}", invoice2);
        return invoice;
    }

    private static Invoice extractFirstPage(String allText, String fullText, PDDocument doc, PDPage firstPage) throws IOException {

        Invoice invoice = new Invoice();
        {
            String reg = "发票号码:(?<number>\\d{20})|:(?<date>\\d{4}年\\d{2}月\\d{2}日)|购名称:(?<buyerName>[\\u4e00-\\u9fa5]+公司)|销名称:(?<sellerAccount>[\\u4e00-\\u9fa5]+公司)";

            Pattern pattern = Pattern.compile(reg);
            Matcher matcher = pattern.matcher(allText);
            while (matcher.find()) {
                if (matcher.group("number") != null) {
                    invoice.setNumber(matcher.group("number"));
                } else if (matcher.group("date") != null) {
                    invoice.setDate(matcher.group("date"));
                } else if (matcher.group("buyerName") != null) {
                    invoice.setBuyerName(matcher.group("buyerName"));
                } else if (matcher.group("sellerAccount") != null) {
                    invoice.setSellerName(matcher.group("sellerAccount"));
                }
            }
        }
        {
            String reg = "纳税人识别号:([\\dA-Z]{18})";
            Pattern pattern = Pattern.compile(reg);
            Matcher matcher = pattern.matcher(allText);
            // 多个匹配 会匹配两次.第一次为购买方的税号，第二次为销售的税号
            int i = 0;
            while (matcher.find()) {
                if (i == 0) {
                    invoice.setBuyerCode(matcher.group(1));
                    i++;
                } else {
                    invoice.setSellerCode(matcher.group(1));
                }
            }
        }

        {
            String reg = "合计¥?(?<amount>[\\d.,]+)¥?(?<taxAmount>[\\d.,]*)"; // "合计¥?(?<amount>[^ \\f\\n\\r\\t\\v\\*]*)(?:¥?(?<taxAmount>\\S*)|\\*+)\\s";
            Pattern pattern = Pattern.compile(reg);
            Matcher matcher = pattern.matcher(allText);
            if (matcher.find()) {
                try {
                    invoice.setAmount(new BigDecimal(matcher.group("amount")));
                } catch (Exception e) {
                    invoice.setAmount(new BigDecimal(0));
                }
                try {
                    invoice.setTaxAmount(new BigDecimal(matcher.group("taxAmount")));
                } catch (Exception e) {
                    invoice.setTaxAmount(new BigDecimal(0));
                }
            }
        }
        if (null == invoice.getAmount()) {
            String reg = "合\\u0020*计\\u0020*¥?(?<amount>[^ ]*)\\u0020+¥?(?:(?<taxAmount>\\S*)|\\*+)\\s";
            Pattern pattern = Pattern.compile(reg);
            Matcher matcher = pattern.matcher(fullText);
            if (matcher.find()) {
                try {
                    invoice.setAmount(new BigDecimal(matcher.group("amount")));
                } catch (Exception e) {
                    invoice.setAmount(new BigDecimal(0));
                }
                try {
                    invoice.setTaxAmount(new BigDecimal(matcher.group("taxAmount")));
                } catch (Exception e) {
                    invoice.setTaxAmount(new BigDecimal(0));
                }
            }
        }
        {
            String reg = "价税合计\\u0028大写\\u0029(?<amountString>\\S*)\\u0028小写\\u0029¥?(?<amount>\\S*)\\s";
            Pattern pattern = Pattern.compile(reg);
            Matcher matcher = pattern.matcher(allText);
            if (matcher.find()) {
                invoice.setTotalAmountString(matcher.group("amountString"));
                try {
                    invoice.setTotalAmount(new BigDecimal(matcher.group("amount")));
                } catch (Exception e) {
                    invoice.setTotalAmount(new BigDecimal(0));
                }
            }
        }
        {
            Pattern type00Pattern = Pattern.compile("\\((.*发票?)\\)");
            Matcher m00 = type00Pattern.matcher(allText);
            if (m00.find()) {
                invoice.setType(m00.group(1));
            }
        }

        PDFKeyWordPosition kwp = new PDFKeyWordPosition();
        Map<String, List<Position>> positionListMap = kwp
                .getCoordinate(Arrays.asList("机器编号", "税率", "单价", "价税合计", "合计", "开票人", "开票日期", "规格型号", "车牌号", "开户行及账号", "密", "码", "区"), doc);

        PDFTextStripperByArea stripper = new PDFTextStripperByArea();
        stripper.setSortByPosition(true);
        PDFTextStripperByArea detailStripper = new PDFTextStripperByArea();
        detailStripper.setSortByPosition(true);

        {
            // 获取关键字的位置，确保不会为null
            Position taxRatePos = positionListMap.containsKey("税率") && !positionListMap.get("税率").isEmpty()
                    ? positionListMap.get("税率").get(0) : null;

            if (taxRatePos == null) {
                // 如果找不到“税率”，可以考虑其他处理方式，比如继续或者抛出异常
                log.error("关键字\"税率\"未找到。");
            }

            Position modelPos = null;
            if (positionListMap.containsKey("规格型号") && !positionListMap.get("规格型号").isEmpty()) {
                modelPos = positionListMap.get("规格型号").get(0);
            } else if (positionListMap.containsKey("车牌号") && !positionListMap.get("车牌号").isEmpty()) {
                modelPos = positionListMap.get("车牌号").get(0);
                modelPos.setX(modelPos.getX() - 15);
            } else if (positionListMap.containsKey("单价") && !positionListMap.get("单价").isEmpty()) {
                modelPos = positionListMap.get("单价").get(0);
                modelPos.setX(modelPos.getX() - 50);
            }

            int detailHeight;
            if (positionListMap.containsKey("合计") && !positionListMap.get("合计").isEmpty()) {
                Position posTmp = positionListMap.get("合计").get(0);
                detailHeight = (int) Math.max(0, posTmp.getY() - taxRatePos.getY() - 25);
            } else if (positionListMap.containsKey("开票人") && !positionListMap.get("开票人").isEmpty()) {
                Position posTmp = positionListMap.get("开票人").get(0);
                detailHeight = (int) Math.max(0, posTmp.getY() - taxRatePos.getY() - 70);
            } else {
                // 如果都找不到，设置默认高度或其他处理方式
                detailHeight = 50;
            }

            int x = 0;
            int y = (int) taxRatePos.getY() + 5;
            if (modelPos != null) {
                x = (int) (modelPos.getX() -13); // 假设x坐标在关键字右侧50单位
            }

            int height = detailHeight > 0 ? detailHeight : 20;

            if (height <= 0) {
                height = 20;
            }

            log.info("x: {}, y: {}, width: {}, height: {}", x, y, firstPage.getCropBox().getWidth(), height);

            detailStripper.addRegion("detail", new Rectangle(0, y, (int) firstPage.getCropBox().getWidth(), height));
            stripper.addRegion("detailName", new Rectangle(0, y, x, detailHeight));
            stripper.addRegion("detailPrice", new Rectangle(x, y, (int) firstPage.getCropBox().getWidth(), detailHeight));

        }
        stripper.extractRegions(firstPage);
        detailStripper.extractRegions(firstPage);
        doc.close();

        {
            List<String> skipList = CollectionUtil.newArrayList();
            List<InvoiceDetail> invoiceDetailList = CollectionUtil.newArrayList();


            String[] detailPriceStringArray = stripper.getTextForRegion("detailPrice").replaceAll("　", " ").replaceAll(" ", " ")
                    .replaceAll("\r", "").split("\\n");

            for (String detailString : detailPriceStringArray) {
                if (StringUtils.containsAny(detailString, "数 ：", "数：", "数:", "数: ", "数 :")) { //过滤发票旁边“下载次数：”信息
                    continue;
                } else if (StringUtils.contains(detailString, "等")) {
                    continue;
                }
                InvoiceDetail invoiceDetail = new InvoiceDetail();
                invoiceDetail.setName("");
                String[] itemArray = StringUtils.split(detailString, " ");
                if (2 == itemArray.length) {
                    if (detailString.contains("¥")) {
                        continue;
                    }
                    invoiceDetail.setAmount(new BigDecimal(itemArray[0]));
                    invoiceDetail.setTaxAmount(new BigDecimal(itemArray[1]));
                    invoiceDetailList.add(invoiceDetail);
                } else if (2 < itemArray.length) {
                    invoiceDetail.setAmount(new BigDecimal(itemArray[itemArray.length - 3]));
                    String taxRate = itemArray[itemArray.length - 2];
                    if (taxRate.indexOf("免税") > 0 || taxRate.indexOf("不征税") > 0 || taxRate.indexOf("出口零税率") > 0
                            || taxRate.indexOf("普通零税率") > 0 || taxRate.indexOf("%") < 0) {
                        invoiceDetail.setTaxRate(new BigDecimal(0));
                        invoiceDetail.setTaxAmount(new BigDecimal(0));
                    } else {
                        BigDecimal rate = new BigDecimal(Integer.parseInt(taxRate.replaceAll("%", "")));
                        invoiceDetail.setTaxRate(rate.divide(new BigDecimal(100)));
                        invoiceDetail.setTaxAmount(new BigDecimal(itemArray[itemArray.length - 1]));
                    }
                    for (int j = 0; j < itemArray.length - 3; j++) {
                        if (itemArray[j].matches("^(-?\\d+)(\\.\\d+)?$")) {
                            if (null == invoiceDetail.getCount()) {
                                invoiceDetail.setCount(new BigDecimal(itemArray[j]));
                            } else {
                                invoiceDetail.setPrice(new BigDecimal(itemArray[j]));
                            }
                        } else {
                            if (itemArray.length >= j + 1 && !itemArray[j + 1].matches("^(-?\\d+)(\\.\\d+)?$")) {
                                invoiceDetail.setUnit(itemArray[j + 1]);
                                invoiceDetail.setModel(itemArray[j]);
                                j++;
                            } else if (itemArray[j].length() > 2) {
                                invoiceDetail.setModel(itemArray[j]);
                            } else {
                                invoiceDetail.setUnit(itemArray[j]);
                            }
                        }
                    }
                    invoiceDetailList.add(invoiceDetail);
                } else {
                    skipList.add(detailString);
                }
            }


            String[] detailNameStringArray = stripper.getTextForRegion("detailName").replaceAll("　", " ").replaceAll(" ", " ")
                    .replaceAll("\r", "").split("\\n");
            String[] detailStringArray = lab.dragon.invoice.utils.StringUtils.replace(detailStripper.getTextForRegion("detail")).replaceAll("\r", "").split("\\n");
            int i = 0, j = 0, h = 0, m = 0;
            InvoiceDetail lastInvoiceDetail = null;
            log.info("detailNameStringArray length: {}", detailNameStringArray.length);

            log.info("detailStringArray length: {}", detailStringArray.length);
            for (String detailString : detailStringArray) {
                if (m < detailNameStringArray.length) {
                    if (detailString.matches("\\S+\\d*(%|免税|不征税|出口零税率|普通零税率)\\S*")
                            && !detailString.matches("^ *\\d*(%|免税|不征税|出口零税率|普通零税率)\\S*")
                            && detailString.matches("\\S+\\d+%[\\-\\d]+\\S*")
                            || detailStringArray.length > i + 1
                            && detailStringArray[i + 1].matches("^ *\\d*(%|免税|不征税|出口零税率|普通零税率)\\S*")) {
                        if (j < invoiceDetailList.size()) {
                            lastInvoiceDetail = invoiceDetailList.get(j);
                            lastInvoiceDetail.setName(detailNameStringArray[m]);
                        }
                        j++;
                    } else if (null != lastInvoiceDetail && StringUtils.isNotBlank(detailNameStringArray[m])) {
                        if (skipList.size() > h) {
                            String skip = skipList.get(h);
                            if (detailString.endsWith(skip)) {
                                if (detailString.equals(skip)) {
                                    m--;
                                } else {
                                    lastInvoiceDetail.setName(lastInvoiceDetail.getName() + detailNameStringArray[m]);
                                }
                                lastInvoiceDetail.setModel(lastInvoiceDetail.getModel() + skip);
                                h++;
                            } else {
                                lastInvoiceDetail.setName(lastInvoiceDetail.getName() + detailNameStringArray[m]);
                            }
                        } else {
                            lastInvoiceDetail.setName(lastInvoiceDetail.getName() + detailNameStringArray[m]);
                        }
                    }
                }
                i++;
                m++;
            }
            invoice.setDetailList(invoiceDetailList);
        }

        {
            if ((Optional.ofNullable(invoice.getDetailList()).isPresent()) && (invoice.getDetailList().size() > 0)) {
                List<InvoiceDetail> invoiceDetails = invoice.getDetailList();
                if (invoice.getAmount().equals(BigDecimal.valueOf(0))) {
                    BigDecimal amout = BigDecimal.valueOf(0);
                    for (InvoiceDetail invoiceDetail : invoiceDetails) {
                        if (!invoiceDetail.getAmount().equals(BigDecimal.valueOf(0))) {
                            amout = amout.add(invoiceDetail.getAmount());
                        }
                    }
                    if (!amout.equals(BigDecimal.valueOf(0))) {
                        invoice.setAmount(amout);
                    }

                }
                if (invoice.getTaxAmount().equals(BigDecimal.valueOf(0))) {
                    BigDecimal taxAmout = BigDecimal.valueOf(0);
                    for (InvoiceDetail invoiceDetail : invoiceDetails) {
                        if (!invoiceDetail.getTaxAmount().equals(BigDecimal.valueOf(0))) {
                            taxAmout = taxAmout.add(invoiceDetail.getTaxAmount());
                        }
                    }
                    if (!taxAmout.equals(BigDecimal.valueOf(0))) {
                        invoice.setTaxAmount(taxAmout);
                    }
                }
            }
        }
        return invoice;
    }

    public static boolean containChineseCharacter(String text) {
        String patter = "[\u4e00-\u9fa5]";
        Pattern p = Pattern.compile(patter);
        Matcher m = p.matcher(text);
        return m.find();
    }

    public static void main(String... args) {
        String text = "电子发票(普通发票)发票号码:25932000000012918560\n" +
                "开票日期:2025年02月19日\n" +
                "共2页第1页\n" +
                "购名称:浙江大学销名称:宁波海曙中创电子产品经营部\n" +
                "买售\n" +
                "方方\n" +
                "信统一社会信用代码/纳税人识别号:12100000470095016Q信统一社会信用代码/纳税人识别号:92330203MA7ETU5T10\n" +
                "息息\n" +
                "项目名称规格型号单位数量单价金额税率/征收率税额\n" +
                "*集成电路*集成电路ADA4522-2ARMZ只30110.89108910891093326.731%33.27\n" +
                "*集成电路*集成电路DAC8812IBPW只15475.24752475247527128.711%71.29\n" +
                "*集成电路*集成电路OPA189IDGKT只1547.5247524752475712.871%7.13\n" +
                "*集成电路*集成电路STM32L052R8H6只5146.5346534653465732.671%7.33\n" +
                "*集成电路*集成电路LT6657AHMS8-2.只5336.63366336633661683.171%16.83\n" +
                "5\n" +
                "*集成电路*集成电路OPA189IDBVT只1547.5247524752475712.871%7.13\n" +
                "*集成电路*集成电路LTC2380IDE-24只301361.386138613861540841.581%408.42\n" +
                "*集成电路*集成电路XC7A35T-1CPG23只51131.18811881188125655.941%56.56\n" +
                "6I\n" +
                "*集成电路*集成电路S25FL064LABNFI只566.8316831683168334.161%3.34\n" +
                "043\n" +
                "*集成电路*集成电路LTC6246HS6#只5222.77227722772281113.861%11.14\n" +
                "TRMPBF\n" +
                "*集成电路*集成电路TPS62040DRCR只526.7326732673267133.661%1.34\n" +
                "*集成电路*集成电路TPS3808G01DBVT只1013.3663366336634133.661%1.34\n" +
                "*集成电路*集成电路ADP2108ACBZ-1.只566.8316831683168334.161%3.34\n" +
                "8-R7\n" +
                "*集成电路*集成电路ADP2108ACBZ-3.只566.8316831683168334.161%3.34\n" +
                "3-R7\n" +
                "*集成电路*集成电路LTC3405AES6只5200.49504950495051002.481%10.02\n" +
                "*集成电路*集成电路AD8421ARMZ只15191.58415841584162873.761%28.74\n" +
                "*集成电路*集成电路LP5912-2.5DRVR只546.7821782178218233.911%2.34\n" +
                "*集成电路*集成电路TPS79101DBVREP只5169.3069306930693846.531%8.47\n" +
                "*集成电路*集成电路TPS63036YFGT只5169.3069306930693846.531%8.47\n" +
                "*集成电路*集成电路ADP7183ACPZN2.只5133.6633663366337668.321%6.68\n" +
                "5-R7\n" +
                "*集成电路*集成电路MAX1697UEUT-T只524.5049504950495122.521%1.23\n" +
                "*集成电路*集成电路SN65HVD33RHLR只589.1089108910891445.541%4.46\n" +
                "*集成电路*集成电路LT8362EDD#WPBF只5200.49504950495051002.481%10.02\n" +
                "*集成电路*集成电路LT8337EV#PBF只5289.60396039603961448.021%14.48\n" +
                "*集成电路*集成电路ADG1421BCPZ只15200.49504950495053007.431%30.07\n" +
                "*集成电路*集成电路ADA4528-2ARMZ只30111.38613861386143341.581%33.42\n" +
                "*集成电路*集成电路Y162550R0000B9只15668.316831683168310024.751%100.25\n" +
                "W-50R\n" +
                "*集成电路*集成电路TMP117AIDRVR只3531.18811881188121091.581%10.92\n" +
                "*集成电路*集成电路TMP117AIYBGR只1553.4653465346535801.981%8.02\n" +
                "*集成电路*集成电路OPA2187IDGKR只1084.6534653465347846.531%8.47\n" +
                "*集成电路*集成电路RN73C1J10KBTG只535.6435643564356178.221%1.78\n" +
                "*集成电路*集成电路RN73C1J20KBTG只553.4653465346535267.331%2.67\n" +
                "*集成电路*集成电路RN73C1J221KB-2只1517.8217821782178267.331%2.67\n" +
                "21K\n" +
                "*集成电路*集成电路VSKY05401006-只54.455445544554522.281%0.22\n" +
                "小计¥92517.30¥925.20\n" +
                "合计¥92517.30¥925.20\n" +
                "开票人:沈丹丹\n";

        String reg = "合计¥?(?<amount>[\\d.,]+)¥?(?<taxAmount>[\\d.,]*)";

        Pattern pattern = Pattern.compile(reg);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            System.out.println("amount: " + matcher.group("amount"));
            System.out.println("taxAmount: " + matcher.group("taxAmount"));
        }
    }

    public static boolean isChinese(String text) {
        for (char c : text.toCharArray()) {
            if (c < 0x4e00 || c > 0x9fa5) {
                return false;
            }
        }
        return true;
    }
}

