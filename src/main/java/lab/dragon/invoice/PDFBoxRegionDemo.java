package lab.dragon.invoice;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;

import java.io.File;
import java.io.IOException;

/**
 * @author mickey.wang
 */
public class PDFBoxRegionDemo {

    public static void main(String[] args) {

        try {
            draw();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void draw() throws IOException {
        // 获取PDF文档
        PDDocument doc = Loader.loadPDF(new File("02-28-18-32-540458.pdf"));
        // 获取第一页
        PDPage firstPage = doc.getPage(0);

        // 创建一个图形状态对象
        PDExtendedGraphicsState graphicsState = new PDExtendedGraphicsState();
        graphicsState.setLineWidth(1.0f); // 设置线宽为1.0

        // 创建内容流对象
        try (PDPageContentStream contentStream = new PDPageContentStream(doc, firstPage, PDPageContentStream.AppendMode.APPEND, false)) {
            // 设置线条颜色为黄色 (RGB: 1, 1, 0)
            contentStream.setStrokingColor( 0, 0, 0);  // 黄色
            contentStream.setLineWidth(1.0f); // 设置线宽为1.0

            // 绘制矩形 (x, y, width, height)
            contentStream.addRect(0, 163, 595, 591); // 绘制一个 100x50 的矩形，位于 (10, 10)
            contentStream.stroke(); // 确保调用 stroke() 来渲染矩形边框

            // 如果想填充矩形的话，使用 fill()
            // contentStream.fill(); // 填充矩形，默认填充黑色

        }

        // 保存修改后的 PDF 文档
        doc.save("result2.pdf");
        doc.close();
    }
}
