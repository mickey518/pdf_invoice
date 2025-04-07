package lab.dragon.invoice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 从 PDF 中提取矩形的工具类，基于 PDFBox 的 GraphicsStreamEngine。
 *
 * @author mickey.wang
 */
public class RectangleExtractor extends PDFGraphicsStreamEngine {
    private static final Logger log = LoggerFactory.getLogger(RectangleExtractor.class);
    private static final int MAX_ITERATIONS = 10000; // 最大迭代次数，防止死循环
    private final List<Rectangle2D> rectangles = new ArrayList<>();
    private final double tolerance = 3.0d; // 容差调整为 3.0，适应 PDF 浮点精度
    private final List<Point2D[]> horizontalLines = new ArrayList<>(); // 水平线集合
    private final List<Point2D[]> verticalLines = new ArrayList<>();   // 垂直线集合
    private Point2D[] currentLine = new Point2D[2]; // 当前绘制的线段
    private Point2D currentPoint = null; // 当前绘制点
    private float pageWidth;  // 页面宽度
    private float pageHeight; // 页面高度

    /**
     * 构造函数，初始化页面尺寸。
     *
     * @param page PDF 页面对象
     */
    protected RectangleExtractor(PDPage page) {
        super(page);
        // 获取页面 MediaBox（有效区域）
        float[] mediaBox = page.getMediaBox().getCOSArray().toFloatArray();
        pageWidth = mediaBox[2] - mediaBox[0];  // 右边界 - 左边界
        pageHeight = mediaBox[3] - mediaBox[1]; // 上边界 - 下边界
    }

    /**
     * 获取提取的所有矩形。
     *
     * @return 矩形列表的副本
     */
    public List<Rectangle2D> getRectangles() {
        rectangles.sort(Comparator.comparingDouble(Rectangle2D::getHeight).reversed()
                .thenComparingDouble(Rectangle2D::getWidth)
                .thenComparingDouble(Rectangle2D::getY)
                .thenComparingDouble(Rectangle2D::getX)
        );
        return new ArrayList<>(rectangles);
    }

    /**
     * 获取包含所有矩形的最外侧矩形。
     *
     * @return 最外侧矩形，若无矩形则返回 null
     */
    public Rectangle2D getOuterRectangle() {
        if (rectangles.isEmpty()) return null;
        Rectangle2D outer = rectangles.get(0);
        if (Math.abs(outer.getX()) <= tolerance || Math.abs(outer.getY()) <= tolerance) {
            log.warn("Outer rectangle has x or y as 0, returning null: x={}, y={}", outer.getX(), outer.getY());
            return null;
        }
        return outer;
    }

    @Override
    public void appendRectangle(Point2D p0, Point2D p1, Point2D p2, Point2D p3) throws IOException {
        // 直接处理 PDF 中的矩形定义
        double x = Math.min(p0.getX(), p2.getX());
        double y = Math.min(p0.getY(), p1.getY());
        double width = Math.abs(p1.getX() - p0.getX());
        double height = Math.abs(p2.getY() - p0.getY());
        if (width > tolerance && height > tolerance) {
            if (Math.abs(x) > tolerance && Math.abs(y) > tolerance) {
                Rectangle2D rect = new Rectangle2D.Double(x, y, width, height);
                rectangles.add(rect);
                log.debug("Appended rectangle: x={}, y={}, w={}, h={}", x, y, width, height);
            } else {
                log.debug("Rectangle skipped due to x or y being 0: x={}, y={}, w={}, h={}", x, y, width, height);
            }
        }
    }

    @Override
    public void moveTo(float x, float y) throws IOException {
        // 忽略超出页面边界的点
        if (x < 0 || y < 0 || x > pageWidth || y > pageHeight) return;
        log.debug("MoveTo: x={}, y={}", x, y);
        currentPoint = new Point2D.Float(x, y);
        currentLine[0] = currentPoint; // 线段起点
    }

    @Override
    public void lineTo(float x, float y) throws IOException {
        // 忽略超出页面边界的点
        if (x < 0 || y < 0 || x > pageWidth || y > pageHeight) return;
        log.debug("LineTo: x={}, y={}", x, y);
        currentPoint = new Point2D.Float(x, y);
        currentLine[1] = currentPoint; // 线段终点
        extractLine();
        currentLine[0] = currentPoint; // 更新起点为当前点
    }

    @Override
    public void closePath() throws IOException {
        log.debug("ClosePath detected");
    }

    @Override
    public void strokePath() throws IOException {
        log.debug("StrokePath detected");
    }

    @Override
    public void fillPath(int windingRule) throws IOException {
        log.debug("FillPath detected: {}", windingRule);
    }

    @Override
    public void fillAndStrokePath(int windingRule) throws IOException {
        log.debug("FillAndStrokePath detected: {}", windingRule);
    }

    @Override
    public void endPath() throws IOException {
        log.debug("EndPath detected");
        findRectangles();
        currentPoint = null;
    }

    @Override
    public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) throws IOException {
        log.debug("Ignoring curve: x1={}, y1={}, x2={}, y2={}, x3={}, y3={}", x1, y1, x2, y2, x3, y3);
    }

    @Override
    public void drawImage(PDImage pdImage) throws IOException {
    }

    @Override
    public void clip(int windingRule) throws IOException {
    }

    @Override
    public Point2D getCurrentPoint() throws IOException {
        return currentPoint;
    }

    @Override
    public void shadingFill(COSName shadingName) throws IOException {
        log.debug("Shading fill detected: {}", shadingName.getName());
    }

    /**
     * 提取线段并分类为水平或垂直线。
     */
    private void extractLine() {
        if (currentLine[0] == null || currentLine[1] == null) return;

        Point2D[] line = new Point2D[]{currentLine[0], currentLine[1]};
        double dx = Math.abs(line[0].getX() - line[1].getX());
        double dy = Math.abs(line[0].getY() - line[1].getY());

        // 更严格的水平/垂直线判断
        if (dx < tolerance && dy > tolerance) {
            // 垂直线
            boolean merged = false;
            double x = (line[0].getX() + line[1].getX()) / 2; // 取平均 x 坐标
            double y1 = Math.min(line[0].getY(), line[1].getY());
            double y2 = Math.max(line[0].getY(), line[1].getY());

            // 检查是否可以与已有垂直线合并
            for (Point2D[] existingLine : verticalLines) {
                double existingX = (existingLine[0].getX() + existingLine[1].getX()) / 2;
                double existingY1 = Math.min(existingLine[0].getY(), existingLine[1].getY());
                double existingY2 = Math.max(existingLine[0].getY(), existingLine[1].getY());

                // x 坐标差小于容差，且 y 坐标范围有重叠或接近
                if (Math.abs(x - existingX) < tolerance &&
                        (Math.abs(y1 - existingY2) < tolerance || Math.abs(y2 - existingY1) < tolerance ||
                                (y1 <= existingY2 && y2 >= existingY1))) {
                    // 合并：更新 y 坐标范围
                    double newY1 = Math.min(y1, existingY1);
                    double newY2 = Math.max(y2, existingY2);
                    existingLine[0] = new Point2D.Double(existingX, newY1);
                    existingLine[1] = new Point2D.Double(existingX, newY2);
                    merged = true;
                    log.debug("Merged vertical line: ({}, {}) -> ({}, {})", existingX, newY1, existingX, newY2);
                    break;
                }
            }

            if (!merged) {
                verticalLines.add(line);
                log.debug("Added vertical line: ({}, {}) -> ({}, {})", line[0].getX(), line[0].getY(), line[1].getX(), line[1].getY());
            }
        } else if (dy < tolerance && dx > tolerance) {
            // 水平线
            boolean merged = false;
            double y = (line[0].getY() + line[1].getY()) / 2; // 取平均 y 坐标
            double x1 = Math.min(line[0].getX(), line[1].getX());
            double x2 = Math.max(line[0].getX(), line[1].getX());

            // 检查是否可以与已有水平线合并
            for (Point2D[] existingLine : horizontalLines) {
                double existingY = (existingLine[0].getY() + existingLine[1].getY()) / 2;
                double existingX1 = Math.min(existingLine[0].getX(), existingLine[1].getX());
                double existingX2 = Math.max(existingLine[0].getX(), existingLine[1].getX());

                // y 坐标差小于容差，且 x 坐标范围有重叠或接近
                if (Math.abs(y - existingY) < tolerance &&
                        (Math.abs(x1 - existingX2) < tolerance || Math.abs(x2 - existingX1) < tolerance ||
                                (x1 <= existingX2 && x2 >= existingX1))) {
                    // 合并：更新 x 坐标范围
                    double newX1 = Math.min(x1, existingX1);
                    double newX2 = Math.max(x2, existingX2);
                    existingLine[0] = new Point2D.Double(newX1, existingY);
                    existingLine[1] = new Point2D.Double(newX2, existingY);
                    merged = true;
                    log.debug("Merged horizontal line: ({}, {}) -> ({}, {})", newX1, existingY, newX2, existingY);
                    break;
                }
            }

            if (!merged) {
                horizontalLines.add(line);
                log.debug("Added horizontal line: ({}, {}) -> ({}, {})", line[0].getX(), line[0].getY(), line[1].getX(), line[1].getY());
            }
        }
    }

    // 判断水平线和垂直线是否相交
    private boolean intersects(Point2D[] horizontal, Point2D[] vertical) {
        double hX1 = Math.min(horizontal[0].getX(), horizontal[1].getX());
        double hX2 = Math.max(horizontal[0].getX(), horizontal[1].getX());
        double hY = (horizontal[0].getY() + horizontal[1].getY()) / 2;

        double vY1 = Math.min(vertical[0].getY(), vertical[1].getY());
        double vY2 = Math.max(vertical[0].getY(), vertical[1].getY());
        double vX = (vertical[0].getX() + vertical[1].getX()) / 2;

        // 水平线的 y 坐标必须在垂直线的 y 范围内
        boolean yInRange = hY >= vY1 - tolerance && hY <= vY2 + tolerance;
        // 垂直线的 x 坐标必须在水平线的 x 范围内
        boolean xInRange = vX >= hX1 - tolerance && vX <= hX2 + tolerance;

        return yInRange && xInRange;
    }

    private void findRectangles() {
        if (horizontalLines.size() < 2 || verticalLines.size() < 2) {
            log.warn("Not enough lines to form rectangles. Horizontal: {}, Vertical: {}",
                    horizontalLines.size(), verticalLines.size());
            return;
        }

        // 按坐标排序
        horizontalLines.sort(Comparator.comparingDouble(line -> line[0].getY())); // 按 Y 从下到上
        verticalLines.sort(Comparator.comparingDouble(line -> line[0].getX()));   // 按 X 从左到右

        // 不再筛选固定区域，直接使用所有水平线和垂直线
        List<Point2D[]> targetHorizontalLines = new ArrayList<>(horizontalLines);
        List<Point2D[]> targetVerticalLines = new ArrayList<>(verticalLines);

        try {
            log.info("HorizontalLines: {}", new ObjectMapper().writeValueAsString(targetHorizontalLines));
            log.info("VerticalLines: {}", new ObjectMapper().writeValueAsString(targetVerticalLines));
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (targetHorizontalLines.size() < 2 || targetVerticalLines.size() < 2) {
            log.warn("Not enough lines to form rectangles after filtering. Horizontal: {}, Vertical: {}",
                    targetHorizontalLines.size(), targetVerticalLines.size());
            return;
        }

        // 计算最外侧矩形（基于相交的线段）
        double minX = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE;
        double maxY = Double.MIN_VALUE;

        // 遍历所有水平线和垂直线，找到相交的边界
        for (Point2D[] hLine : targetHorizontalLines) {
            for (Point2D[] vLine : targetVerticalLines) {
                if (intersects(hLine, vLine)) {
                    double hY = (hLine[0].getY() + hLine[1].getY()) / 2;
                    double vX = (vLine[0].getX() + vLine[1].getX()) / 2;
                    minX = Math.min(minX, vX);
                    maxX = Math.max(maxX, vX);
                    minY = Math.min(minY, hY);
                    maxY = Math.max(maxY, hY);
                }
            }
        }

        if (minX == Double.MAX_VALUE || maxX == Double.MIN_VALUE || minY == Double.MAX_VALUE || maxY == Double.MIN_VALUE) {
            log.warn("No intersecting lines found to form an outer rectangle.");
            return;
        }

        Rectangle2D outerRect = new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
        if (Math.abs(outerRect.getX()) > tolerance && Math.abs(outerRect.getY()) > tolerance && Math.abs(outerRect.getWidth()) > tolerance && Math.abs(outerRect.getHeight()) > tolerance) {
            if (!rectangles.contains(outerRect)) {
                rectangles.add(outerRect);
                log.info("Outer rectangle: x={}, y={}, w={}, h={}", outerRect.getX(), outerRect.getY(), outerRect.getWidth(), outerRect.getHeight());
            }
        } else {
            log.debug("Outer rectangle skipped due to x or y being 0: x={}, y={}, w={}, h={}",
                    outerRect.getX(), outerRect.getY(), outerRect.getWidth(), outerRect.getHeight());
        }

        // 计算内部矩形，只考虑相邻的线段
        int iterationCount = 0;
        for (int i = 0; i < targetHorizontalLines.size() - 1; i++) {
            Point2D[] h1 = targetHorizontalLines.get(i);
            Point2D[] h2 = targetHorizontalLines.get(i + 1);
            for (int j = 0; j < targetVerticalLines.size() - 1; j++) {
                Point2D[] v1 = targetVerticalLines.get(j);
                Point2D[] v2 = targetVerticalLines.get(j + 1);

                // 验证矩形的四个顶点是否存在（即水平线和垂直线是否相交）
                boolean topLeft = intersects(h1, v1);
                boolean topRight = intersects(h1, v2);
                boolean bottomLeft = intersects(h2, v1);
                boolean bottomRight = intersects(h2, v2);

                if (topLeft && topRight && bottomLeft && bottomRight) {
                    double x = v1[0].getX();
                    double y = h1[0].getY();
                    double width = v2[0].getX() - x;
                    double height = h2[0].getY() - y;

                    if (width > tolerance && height > tolerance) { // 移除 MAX_RECT_WIDTH 和 MAX_RECT_HEIGHT 限制
                        if (Math.abs(x) > tolerance && Math.abs(y) > tolerance) {
                            Rectangle2D rect = new Rectangle2D.Double(x, y, width, height);
                            if (!rectangles.contains(rect)) {
                                rectangles.add(rect);
                                log.debug("Added inner rectangle: x={}, y={}, w={}, h={}", x, y, width, height);
                            }
                        } else {
                            log.debug("Inner rectangle skipped due to x or y being 0: x={}, y={}, w={}, h={}", x, y, width, height);
                        }
                    }
                } else {
//                    log.debug("Skipped rectangle due to missing intersection points: h1=({}, {}), h2=({}, {}), v1=({}, {}), v2=({}, {})",
//                            h1[0].getY(), h1[1].getY(), h2[0].getY(), h2[1].getY(),
//                            v1[0].getX(), v1[1].getX(), v2[0].getX(), v2[1].getX());
                }

                // 防止死循环
                iterationCount++;
                if (iterationCount > MAX_ITERATIONS) {
                    log.error("Maximum iterations reached, breaking loop to prevent infinite loop");
                    return;
                }
            }
        }
        log.info("Total iterations: {}", iterationCount);
    }
}