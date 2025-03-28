package lab.dragon.invoice;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author mickey.wang
 */
public class RectangleExtractor extends PDFGraphicsStreamEngine {
  private static final Logger log = LoggerFactory.getLogger(RectangleExtractor.class);
  private final List<Rectangle2D> rectangles = new ArrayList<>();
  private final double tolerance = 0.9d;
  List<Point2D[]> horizontalLines = new ArrayList<>();
  List<Point2D[]> verticalLines = new ArrayList<>();
  private Point2D[] currentLine = new Point2D[2];
  private Point2D currentPoint = null; // 跟踪当前点
  private Rectangle2D outerRect = null;
  private float pageWidth;
  private float pageHeight;

  private Point2D leftLow;
  private Point2D leftUp;
  private Point2D rightLow;
  private Point2D rightUp;

  /**
   * Constructor.
   *
   * @param page
   */
  protected RectangleExtractor(PDPage page) {
    super(page);
    // 获取 MediaBox（页面完整区域）
    org.apache.pdfbox.cos.COSArray mediaBox =
        (org.apache.pdfbox.cos.COSArray) page.getCOSObject().getDictionaryObject("MediaBox");
    float[] mediaBoxArray = mediaBox.toFloatArray();

    pageWidth = mediaBoxArray[2] - mediaBoxArray[0];
    pageHeight = mediaBoxArray[3] - mediaBoxArray[1];
    // 左下角定义为右上角
    leftLow = new Point2D.Double(pageWidth, pageHeight);
    // 左上角定义为右下角
    leftUp = new Point2D.Double(pageWidth, 0);
    // 右下角定义为左上角
    rightLow = new Point2D.Double(0, pageHeight);
    // 右上角定义为左下角
    rightUp = new Point2D.Double(0, 0);
  }

  @Override
  public void appendRectangle(Point2D p0, Point2D p1, Point2D p2, Point2D p3) {
    // 路径是错误的，需要手动处理路径，手动识别为矩形
  }

  // 获取所有提取的矩形
  public List<Rectangle2D> getRectangles() {
    log.info("left low: {}", leftLow);
    return new ArrayList<>(rectangles); // 返回副本以保护内部状态
  }

  public Rectangle2D getOuterRectangle() {
    if (rectangles.isEmpty()) return null;

    // 初始化为第一个矩形
    Rectangle2D outer = rectangles.get(0);

    // 遍历所有矩形，找到包含所有矩形的最外侧矩形
    for (Rectangle2D rect : rectangles) {
      outer = outer.createUnion(rect);
    }

    return outer;
  }

  @Override
  public void moveTo(float x, float y) throws IOException {
    if (x <= 0 || y <= 0 || x >= pageHeight || y >= pageWidth) return;
    log.debug("MoveTo: x={}, y={}", x, y);
    currentPoint = new Point2D.Float(x, y);
    // 更靠左边
    if (currentPoint.getX() + tolerance < leftLow.getX()) {
      if (currentPoint.getY() + tolerance < leftLow.getY()) leftLow.setLocation(currentPoint);
      if (currentPoint.getY() + tolerance > leftUp.getY()) leftUp.setLocation(currentPoint);
    }
    // 更靠右边
    if (currentPoint.getX() + tolerance > rightLow.getX()) {
      if (currentPoint.getY() + tolerance < rightLow.getY()) rightLow.setLocation(currentPoint);
      if (currentPoint.getY() + tolerance > rightUp.getY()) rightUp.setLocation(currentPoint);
    }

    currentLine[1] = currentPoint;
    extractLine();
    currentLine[0] = currentPoint;
  }

  @Override
  public void lineTo(float x, float y) throws IOException {
    if (x <= 0 || y <= 0 || x >= pageHeight || y >= pageWidth) return;
    log.debug("LineTo: x={}, y={}", x, y);
    currentPoint = new Point2D.Float(x, y);

    // 更靠左边
    if (currentPoint.getX() + tolerance < leftLow.getX()) {
      if (currentPoint.getY() + tolerance < leftLow.getY()) leftLow.setLocation(currentPoint);
      if (currentPoint.getY() + tolerance > leftUp.getY()) leftUp.setLocation(currentPoint);
    }
    // 更靠右边
    if (currentPoint.getX() + tolerance > rightLow.getX()) {
      if (currentPoint.getY() + tolerance < rightLow.getY()) rightLow.setLocation(currentPoint);
      if (currentPoint.getY() + tolerance > rightUp.getY()) rightUp.setLocation(currentPoint);
    }

    currentLine[1] = currentPoint;
    extractLine();
    currentLine[0] = currentPoint;
  }

  @Override
  public void endPath() throws IOException {
    log.debug("EndPath detected");
    currentPoint = null;
  }

  @Override
  public void closePath() throws IOException {
    log.debug("ClosePath detected, checking for rectangle");
    // 识别路径，绘制成矩形
    findRectangles();
  }

  @Override
  public void drawImage(PDImage pdImage) throws IOException {}

  @Override
  public void clip(int windingRule) throws IOException {}

  @Override
  public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3)
      throws IOException {
    log.debug("暂不处理曲线相关的路径");
  }

  @Override
  public Point2D getCurrentPoint() throws IOException {
    return currentPoint;
  }

  @Override
  public void strokePath() throws IOException {
    extractLine();
    // 识别路径，绘制成矩形
    findRectangles();
  }

  private void extractLine() {
    if (currentLine[0] != null && currentLine[1] != null) {
      // 判断是否为水平线（y 值变化小于容差）
      if (Math.abs(currentLine[0].getY() - currentLine[1].getY()) < tolerance) {
        verticalLines.add(new Point2D[] {currentLine[0], currentLine[1]});
      }
      // 判断是否为垂直线（x 值变化小于容差）
      else if (Math.abs(currentLine[0].getX() - currentLine[1].getX()) < tolerance) {
        horizontalLines.add(new Point2D[] {currentLine[0], currentLine[1]});
      }
    }
    log.debug("水平线：{}个，垂直线：{}个", verticalLines.size(), horizontalLines.size());
  }

  @Override
  public void fillPath(int windingRule) throws IOException {
    // 如果矩形是通过填充路径定义，可能需要在此处捕获
    // 当前仅处理 appendRectangle
    log.debug("Fill path detected.");
    extractLine();
    // 识别路径，绘制成矩形
    findRectangles();
  }

  @Override
  public void fillAndStrokePath(int windingRule) throws IOException {
    log.debug("fillAndStrokePath.");
    findRectangles();
  }

  private List<Rectangle2D> findRectangles() {
    // 清理之前的矩形列表
    rectangles.clear();
    // 如果没有足够线段，无法形成矩形
    if (horizontalLines.size() < 2 || verticalLines.size() < 2) {
      log.warn(
          "Not enough lines to form rectangles. Horizontal: {}, Vertical: {}",
          horizontalLines.size(),
          verticalLines.size());
      return rectangles;
    }
    // 排序：水平线按照 y 坐标（从下到上），可以获取到左下角到右下角的坐标【【【基于最下方是矩形最下方的线段情况】】】
    verticalLines.sort(
        Comparator.comparingDouble(o -> ((Point2D[]) o)[0].getX())
            .thenComparingDouble(o -> ((Point2D[]) o)[0].getY()));
    // 垂直线按照 x 坐标（从左到右）
    horizontalLines.sort(
        Comparator.comparingDouble(o -> ((Point2D[]) o)[0].getX())
            .thenComparingDouble(o -> ((Point2D[]) o)[0].getY()));

    // 发票最外侧的红色矩形框位置如下：
    Point2D leftBottom = verticalLines.get(0)[0];
    Point2D rightBottom = verticalLines.get(0)[1];
    Point2D leftTop = horizontalLines.get(0)[0];

    // 获取最右上角的点位
    Point2D rightTop = new Point2D.Double(rightBottom.getX(), leftTop.getY());

    log.info(
        "left bottom: {}, right bottom: {}, left top: {}, right top: {}",
        leftBottom,
        rightBottom,
        leftTop,
        rightTop);

    // 验证最外侧矩形的水平线
    double bottomY = rightBottom.getY(); // 最下水平线
    double topY = leftTop.getY(); // 最上水平线

    if (outerRect == null
        || leftBottom.getX() < outerRect.getX()
        || leftTop.getY() > outerRect.getY() + outerRect.getHeight()
        || rightBottom.getX() > outerRect.getX() + outerRect.getWidth()) {
      // 确保最外侧矩形有效
      if (Math.abs(leftBottom.getY() - bottomY) > tolerance
          || Math.abs(leftTop.getY() - topY) > tolerance
          || Math.abs(rightBottom.getY() - bottomY) > tolerance
          || Math.abs(rightTop.getY() - topY) > tolerance) {
        log.warn("Outer rectangle validation failed. Expected bottomY={}, topY={}", bottomY, topY);
        return rectangles;
      }

      outerRect =
          new Rectangle2D.Double(
              leftBottom.getX(),
              leftBottom.getY(),
              rightBottom.getX() - leftBottom.getX(),
              leftTop.getY() - leftBottom.getY());
      log.error("设置最外圈矩形大小：{}", outerRect);
    }

    rectangles.add(outerRect);
    // 过滤下不在矩形框内的垂直线，并且按照左下方向上的顺序排序下垂直线
    List<Point2D[]> filterHorizontalLines =
        horizontalLines.stream()
            .filter(o -> (o)[0].getX() > leftBottom.getX() && (o)[0].getX() < rightBottom.getX())
            .sorted(
                Comparator.comparingDouble(o -> ((Point2D[]) o)[0].getY())
                    .thenComparingDouble(o -> ((Point2D[]) o)[0].getX()))
            .collect(Collectors.toList());

    // 设置左下方起始位置
    Point2D startLeftBottom = leftBottom;
    // 遍历识别到的矩形框内的垂直线，将其组合为矩形
    for (int i = 0; i < filterHorizontalLines.size(); i++) {
      Point2D[] ds = filterHorizontalLines.get(i);
      log.info("[{}]坐标： {}, {}", i, ds[0], ds[1]);

      // 判断下左下角起始点和垂直线起始点的差值如果超过垂直线的高度很多的话，证明中间还有块区域需要识别
      if (Math.abs(startLeftBottom.getY() - ds[1].getY()) > tolerance) {
        Rectangle2D rect =
            new Rectangle2D.Double(
                startLeftBottom.getX(),
                startLeftBottom.getY(),
                outerRect.getWidth(),
                ds[1].getY() - startLeftBottom.getY());
        rectangles.add(rect);
        startLeftBottom = new Point2D.Double(outerRect.getX(), ds[1].getY());
      }

      // 以左下角 left bottom 做为起点开始处理
      Rectangle2D rect =
          new Rectangle2D.Double(
              startLeftBottom.getX(),
              startLeftBottom.getY(),
              ds[0].getX() - startLeftBottom.getX(),
              ds[0].getY() - startLeftBottom.getY());
      rectangles.add(rect);
      if (i < filterHorizontalLines.size() - 1 && filterHorizontalLines.get(i + 1)[0].getY() <= ds[0].getY()) {
        // 不是最后一条垂直线
        startLeftBottom = ds[1];
      } else {
        startLeftBottom = ds[1];
        rect =
            new Rectangle2D.Double(
                startLeftBottom.getX(),
                startLeftBottom.getY(),
                outerRect.getWidth() - rect.getWidth(),
                rect.getHeight());
        rectangles.add(rect);
        startLeftBottom = new Point2D.Double(outerRect.getX(), ds[0].getY());
      }
    }

    return rectangles;
  }

  @Override
  public void shadingFill(COSName shadingName) throws IOException {
    log.error("shading fill");
  }
}
