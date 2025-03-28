package lab.dragon.invoice.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author mickey.wang
 */
public class PDFExtractKeyword {

    private static final Logger log = LoggerFactory.getLogger(PDFExtractKeyword.class);

    public static void extractKeyword(PDDocument document) throws IOException {
        PDFKeyWordPosition kwp = new PDFKeyWordPosition();
        Map<String, List<Position>> positionListMap = kwp
                .getCoordinate(Arrays.asList("电子发票", "普通发票"), document);

        Position taxRatePos = positionListMap.containsKey("电子发票") && !positionListMap.get("电子发票").isEmpty()
                ? positionListMap.get("电子发票").get(0) : null;

        log.info("电子发票位置：{}", taxRatePos);

        taxRatePos = positionListMap.containsKey("普通发票") && !positionListMap.get("普通发票").isEmpty()
                ? positionListMap.get("普通发票").get(0) : null;

        log.info("普通发票位置：{}", taxRatePos);
    }
}
