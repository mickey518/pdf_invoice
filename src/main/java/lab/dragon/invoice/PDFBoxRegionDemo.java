package lab.dragon.invoice;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.Point2D;
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

        String[] filePaths = new String[] {
                "032002300811_33196780_浙江大学.pdf"
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
            float height;
            // 获取 MediaBox（页面完整区域）
            org.apache.pdfbox.cos.COSArray mediaBox = (org.apache.pdfbox.cos.COSArray) page.getCOSObject().getDictionaryObject("MediaBox");
            float[] mediaBoxArray = mediaBox.toFloatArray();

            height = mediaBoxArray[3] - mediaBoxArray[1];

            RectangleExtractor extractor = new RectangleExtractor(page);
            extractor.processPage(page);

            List<Rectangle2D> rectangles = extractor.getRectangles();
            for (int i = 0; i < rectangles.size(); i++) {
                log.info("rect [i]: {}, rect: {}", i, rectangles.get(i));
            }

            for (Rectangle2D rect : rectangles) {
                Rectangle2D rectangle2D = new Rectangle2D.Float((float) rect.getX(), height - (float) rect.getY() - (float) rect.getHeight(), (float) rect.getWidth(), (float) rect.getHeight());

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
        }


        document.close();
    }
}
