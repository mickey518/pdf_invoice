package lab.dragon.invoice;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author mickey.wang
 */
public class PDFBoxRegionDemo {

    private static final Logger log = LoggerFactory.getLogger(PDFBoxRegionDemo.class);

    public static void main(String[] args) {
        // "032002300811_33196780_浙江大学.pdf",
//        "鹏睿康_浙江大学_20250214-1.pdf",
//        "海富睿_浙江大学_20250218-2.pdf",
//                "dzfp_25932000000012918560_浙江大学_20250219093026.pdf"
        //_浙江大学_20250224190505.pdf

        String[] filePaths = new String[]{
                "./test-data/_浙江大学_20250224190505.pdf"
        };

        try {
            for (String filePath : filePaths) {
                read(filePath);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void read(String pdfPath) throws IOException {

        PDDocument document = PDDocument.load(new File(pdfPath));

        for (PDPage page : document.getPages()) {
            // 获取页面 MediaBox（有效区域）
            float[] mediaBox = page.getMediaBox().getCOSArray().toFloatArray();
            float pageWidth = mediaBox[2] - mediaBox[0];  // 右边界 - 左边界
            float pageHeight = mediaBox[3] - mediaBox[1]; // 上边界 - 下边界

            RectangleExtractor extractor = new RectangleExtractor(page);
            extractor.processPage(page);

            log.info("页面总大小：{}x{}", pageWidth, pageHeight);
            List<Rectangle2D> rectangles = extractor.getRectangles();
            for (int i = 0; i < rectangles.size(); i++) {
                log.debug("rect [i]: {}, rect: {}", i, rectangles.get(i));
            }

            Rectangle2D rect = extractor.getOuterRectangle();

            log.debug("rect: {}", rect);

            Rectangle2D rectangle2D = new Rectangle2D.Float((float) rect.getX(), pageHeight - (float) rect.getY() - (float) rect.getHeight(), (float) rect.getWidth(), (float) rect.getHeight());

            log.debug("rectangle2D: {}", rectangle2D);

            PDFTextStripperByArea stripper = new PDFTextStripperByArea();
            stripper.setSortByPosition(true);

            stripper.addRegion("RECTANGLE_0", rectangle2D);
            stripper.extractRegions(page);

            String[] strings = stripper.getTextForRegion("RECTANGLE_0").replaceAll("　", " ").replaceAll(" ", " ")
                    .replaceAll("\r", "").split("\\n");
            System.out.println("-------------------------------------------------");

            for (int i = 0; i < strings.length; i++) {
                System.out.printf("RECTANGLE_0; i: %s, item: %s \n", i, strings[i]);
            }
            System.out.println("-------------------------------------------------");


        }


        document.close();
    }
}
