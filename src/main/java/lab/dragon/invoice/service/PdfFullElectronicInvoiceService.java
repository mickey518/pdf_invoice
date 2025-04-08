package lab.dragon.invoice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
     * 匹配一个项目子项，字段解释：                             <br />
     * 分组	含义	匹配内容                                    <br />
     * 1	文本内容	任意字符直到金额前                       <br />
     * 2	金额	整数或小数(包括负数)                         <br />
     * 3	税率（限制 0~100%）	如 0%、17.5%、100%         <br />
     * 4	税额	整数或小数(包括负数)                         <br />
     * 示例匹配行：                                           <br />
     * *纺织产品*无尘布 4009超细(10* 包 2 51.2871 1% 1.03     <br />
     * 结果：                                              <br />
     * 文本：*纺织产品*无尘布 4009超细(10* 包 2              <br />
     * 金额：51.2871                                   <br />
     * 税率：1%                                        <br />
     * 税额：1.03                                      <br />
     * 不会匹配：                                        <br />
     * 输入	不匹配原因                                   <br />
     * 产品 102.50 101% 1.03	税率超过 100%                  <br />
     * 产品 100.00 123456789% 20.00	税率格式非法          <br />
     * 产品102.5 1%1.3	空格缺失                        <br />
     * 产品 102.5 % 1.3	缺少数字                        <br />
     */
    private static final String invoiceDetailReg = "^(.*?)\\s+(-?\\d+(?:\\.\\d+)?)\\s+([1-9]?\\d(?:\\.\\d{1,2})?%|100%)\\s+(-?\\d+(?:\\.\\d+)?)$";
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
        int index = 0;
        if (StringUtils.isBlank(texts[0]) || !StringUtils.contains(texts[0], "开票日期")) {
            index++;
        }
        Invoice invoice = extractFirstPage("电子发票" + texts[index], fullText, doc, doc.getPage(0));
//        index++;
//        for (int i = 1; i < pages; i++) {
//            Invoice invoiceTmp = extractFirstPage("电子发票" + texts[index], fullText, doc, doc.getPage(i));
//            log.warn("invoice tmp: {}", new ObjectMapper().writeValueAsString(invoiceTmp));
//        }

        doc.close();
        return invoice;
    }

    private static Invoice extractFirstPage(String allText, String fullText, PDDocument doc, PDPage firstPage) throws IOException {
        log.warn(allText);
        Invoice invoice = new Invoice();
        {
            Pattern invoiceNumberPattern = Pattern.compile("发票号码[:：]?(?<number>\\d{20})");
            Pattern invoiceDatePattern = Pattern.compile("开票日期[:：]?(?<date>\\d{4}年\\d{2}月\\d{2}日)");
            Pattern buyerSellerPattern = Pattern.compile("[购买]名称[:：]?(?<buyer>[\\u4e00-\\u9fa5]+)[销售]名称[:：]?(?<seller>[\\u4e00-\\u9fa5]+)");

            Matcher m1 = invoiceNumberPattern.matcher(allText);
            if (m1.find()) {
                String number = m1.group("number");
                log.debug("发票号码：{}", number);
                invoice.setNumber(number);
            }
            Matcher m2 = invoiceDatePattern.matcher(allText);
            if (m2.find()) {
                String date = m2.group("date");
                log.debug("开票日期：{}", date);
                invoice.setDate(date);
            }
            Matcher m3 = buyerSellerPattern.matcher(allText);
            if (m3.find()) {
                String buyer = m3.group("buyer");
                log.debug("购买方名称：{}", buyer);
                invoice.setBuyerName(buyer);
                String seller = m3.group("seller");
                log.debug("销售方名称：{}", seller);
                invoice.setSellerName(seller);
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
                .getCoordinate(Arrays.asList("项目名称", "规格型号", "单位", "机器编号", "税率", "单价", "价税合计", "小计", "合计", "开票人", "开票日期",  "车牌号", "开户行及账号", "密", "码", "区"), doc);

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
                return invoice;
            }

            // 以《单位》字段作为识别位置标识
            Position unitPos = null;
            if (positionListMap.containsKey("单位") && !positionListMap.get("单位").isEmpty()) {
                unitPos = positionListMap.get("单位").get(0);
            } else if (positionListMap.containsKey("车牌号") && !positionListMap.get("车牌号").isEmpty()) {
                unitPos = positionListMap.get("车牌号").get(0);
                unitPos.setX(unitPos.getX() - 15);
            } else if (positionListMap.containsKey("单价") && !positionListMap.get("单价").isEmpty()) {
                unitPos = positionListMap.get("单价").get(0);
                unitPos.setX(unitPos.getX() - 50);
            }
            Position modelPos = null;
            if (positionListMap.containsKey("规格型号") && !positionListMap.get("规格型号").isEmpty()) {
                modelPos = positionListMap.get("规格型号").get(0);
            }


            int detailHeight;
            if (positionListMap.containsKey("小计") && !positionListMap.get("小计").isEmpty()) {
                Position posTmp = positionListMap.get("小计").get(0);
                detailHeight = (int) Math.max(0, posTmp.getY() - taxRatePos.getY() - 25);
            } else if (positionListMap.containsKey("合计") && !positionListMap.get("合计").isEmpty()) {
                Position posTmp = positionListMap.get("合计").get(0);
                detailHeight = (int) Math.max(0, posTmp.getY() - taxRatePos.getY() - 25);
            } else if (positionListMap.containsKey("开票人") && !positionListMap.get("开票人").isEmpty()) {
                Position posTmp = positionListMap.get("开票人").get(0);
                detailHeight = (int) Math.max(0, posTmp.getY() - taxRatePos.getY() - 70);
            } else {
                // 如果都找不到，设置默认高度或其他处理方式
                detailHeight = 50;
            }

            int y = (int) taxRatePos.getY() + 5;

            log.debug("model position: {}, unit position: {}, detail height: {}", modelPos, unitPos, detailHeight);

            detailStripper.addRegion("detail", new Rectangle(0, y, (int) firstPage.getCropBox().getWidth(), detailHeight));
            stripper.addRegion("detailName", new Rectangle(0, y, (int)modelPos.getX(), detailHeight));
            stripper.addRegion("detailModel", new Rectangle((int) modelPos.getX(), y, (int)unitPos.getX()-10, detailHeight));
            stripper.addRegion("detailPrice", new Rectangle((int)unitPos.getX()-10, y, (int) firstPage.getCropBox().getWidth() - (int)unitPos.getX(), detailHeight));
        }
        stripper.extractRegions(firstPage);
        detailStripper.extractRegions(firstPage);

        {
            List<String> skipList = CollectionUtil.newArrayList();
            List<InvoiceDetail> invoiceDetailList = CollectionUtil.newArrayList();

            String[] detailPriceStringArray = stripper.getTextForRegion("detailPrice").replaceAll("　", " ").replaceAll(" ", " ")
                    .replaceAll("\r", "").split("\\n");
            log.warn("detailPriceStringArray: {}", new ObjectMapper().writeValueAsString(detailPriceStringArray));

            for (String detailString : detailPriceStringArray) {
                if (StringUtils.containsAny(detailString, "数 ：", "数：", "数:", "数: ", "数 :")) { //过滤发票旁边“下载次数：”信息
                    continue;
                } else if (StringUtils.contains(detailString, "等")) {
                    continue;
                }
                InvoiceDetail invoiceDetail = new InvoiceDetail();
                invoiceDetail.setName("");
                String[] itemArray = StringUtils.split(detailString, " ");
                log.debug("item array: {}, {}", itemArray.length, new ObjectMapper().writeValueAsString(itemArray));

                if (2 >= itemArray.length) {
                    if (detailString.matches("^(-?\\d+)(\\.\\d+)?$")) {
                        log.error("这里要处理下，这种发票没有设置处理逻辑，detailString： {}", detailString);
                        continue;
                    }
                } else if (2 < itemArray.length) {
                    log.debug("识别到金额：{}", itemArray[itemArray.length - 3]);
                    invoiceDetail.setAmount(new BigDecimal(itemArray[itemArray.length - 3]));
                    String taxRate = itemArray[itemArray.length - 2];
                    if (taxRate.indexOf("免税") > 0 || taxRate.indexOf("不征税") > 0 || taxRate.indexOf("出口零税率") > 0
                            || taxRate.indexOf("普通零税率") > 0 || taxRate.indexOf("%") < 0) {
                        invoiceDetail.setTaxRate(new BigDecimal(0));
                        invoiceDetail.setTaxAmount(new BigDecimal(0));
                    } else {
                        BigDecimal rate = new BigDecimal(Integer.parseInt(taxRate.replaceAll("%", "")));
                        invoiceDetail.setTaxRate(rate.divide(new BigDecimal(100)));
                        log.debug("识别到税额：{}", itemArray[itemArray.length - 1]);
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
                                j++;
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

            String[] detailModelStringArray = stripper.getTextForRegion("detailModel").replaceAll("　", " ").replaceAll(" ", " ")
                    .replaceAll("\r", "").split("\\n");
            String[] detailNameStringArray = stripper.getTextForRegion("detailName").replaceAll("　", " ").replaceAll(" ", " ")
                    .replaceAll("\r", "").split("\\n");
            String[] detailStringArray = detailStripper.getTextForRegion("detail").replaceAll("　", " ").replaceAll(" ", " ")
                    .replaceAll("\r", "").split("\\n");

            int i = 0, j = 0, h = 0, m = 0, n=0;
            InvoiceDetail lastInvoiceDetail = null;

            log.warn("detailModelStringArray: {}", new ObjectMapper().writeValueAsString(detailModelStringArray));
            log.warn("detailNameStringArray: {}", new ObjectMapper().writeValueAsString(detailNameStringArray));
            log.warn("detailStringArray: {}", new ObjectMapper().writeValueAsString(detailStringArray));

            for (String detailString : detailStringArray) {
                if (m < detailModelStringArray.length || n < detailNameStringArray.length) {
                    if (detailString.matches(invoiceDetailReg)) {
                        if (j < invoiceDetailList.size()) {
                            lastInvoiceDetail = invoiceDetailList.get(j);
                            if (detailModelStringArray.length > m && StringUtils.isNotBlank(detailModelStringArray[m]) && detailString.contains(detailModelStringArray[m])) {
                                lastInvoiceDetail.setModel(detailModelStringArray[m]);
                                m++;
                            }
                            if (detailNameStringArray.length > n && StringUtils.isNotBlank(detailNameStringArray[n]) && detailString.contains(detailNameStringArray[n])) {
                                lastInvoiceDetail.setName(detailNameStringArray[n]);
                                n++;
                            }
                        }
                        j++;
                    } else if (null != lastInvoiceDetail) {
                        if (detailModelStringArray.length > m && StringUtils.isNotBlank(detailModelStringArray[m]) && detailString.contains(detailModelStringArray[m])) {
                            if (skipList.size() > h) {
                                String skip = skipList.get(h);
                                if (detailString.endsWith(skip)) {
                                    if (detailString.equals(skip)) {
                                        m--;
                                    } else {
                                        lastInvoiceDetail.setModel(lastInvoiceDetail.getModel() + detailModelStringArray[m]);
                                    }
                                    lastInvoiceDetail.setModel(lastInvoiceDetail.getModel() + skip);
                                    h++;
                                } else {
                                    lastInvoiceDetail.setModel(lastInvoiceDetail.getModel() + detailModelStringArray[m]);
                                }
                            } else {
                                lastInvoiceDetail.setModel(lastInvoiceDetail.getModel() + detailModelStringArray[m]);
                            }
                            m++;
                        }
                        if (detailNameStringArray.length > n && StringUtils.isNotBlank(detailNameStringArray[n]) && detailString.contains(detailNameStringArray[n])) {
                            if (skipList.size() > h) {
                                String skip = skipList.get(h);
                                if (detailString.endsWith(skip)) {
                                    if (detailString.equals(skip)) {
                                        n--;
                                    } else {
                                        lastInvoiceDetail.setName(lastInvoiceDetail.getName() + detailNameStringArray[n]);
                                    }
                                    h++;
                                } else {
                                    lastInvoiceDetail.setName(lastInvoiceDetail.getName() + detailNameStringArray[n]);
                                }
                            } else {
                                lastInvoiceDetail.setName(lastInvoiceDetail.getName() + detailNameStringArray[n]);
                            }
                            n++;
                        }
                    }
                }
                i++;
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

    public static boolean isChinese(String text) {
        for (char c : text.toCharArray()) {
            if (c < 0x4e00 || c > 0x9fa5) {
                return false;
            }
        }
        return true;
    }
}

