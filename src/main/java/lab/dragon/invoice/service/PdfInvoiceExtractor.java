package lab.dragon.invoice.service;

import lab.dragon.invoice.entity.Invoice;
import lab.dragon.invoice.utils.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * 专用于处理电子发票识别的类
 *
 */
public class PdfInvoiceExtractor {

    private static final Logger log = LoggerFactory.getLogger(PdfInvoiceExtractor.class);

    /**
     * <p>
     *  Electronic Invoice
     * </p>
     *
     * @param file PDF
     * @return Invoice 发票对象
     * @throws IOException
     */
    public static Invoice extract(File file) throws IOException {
        // 获取PDF文档
        PDDocument doc = PDDocument.load(file);
        // 获取第一页
        PDPage firstPage = doc.getPage(0);
        // 获取第一页的宽度
        int pageWidth = Math.round(firstPage.getCropBox().getWidth());
        // 获取PDF文本
        PDFTextStripper textStripper = new PDFTextStripper();
        // 设置排序
        textStripper.setSortByPosition(true);
        String fullText = textStripper.getText(doc);
        // 如果是纵向，那么获取高度
        if (firstPage.getRotation() != 0) {
            pageWidth = Math.round(firstPage.getCropBox().getHeight());
        }
        String allText = StringUtils.replace(fullText).replaceAll("（", "(").replaceAll("）", ")").replaceAll("￥", "¥");
        allText = allText.trim();

        if(allText.contains("电子发票") || allText.contains("电⼦发票")){
            log.info("全电票处理...");
            // 全票
          return PdfFullElectronicInvoiceService.getFullElectronicInvoice(fullText, allText, pageWidth, doc, firstPage);
        }else {
            log.info("常规发票处理...");
           return PdfRegularInvoiceService.getRegularInvoice(fullText, allText, pageWidth, doc, firstPage);
        }
    }
}