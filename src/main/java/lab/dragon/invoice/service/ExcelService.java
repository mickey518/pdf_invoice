package lab.dragon.invoice.service;

import lab.dragon.invoice.VO.InvoiceDetailVO;
import lab.dragon.invoice.VO.InvoiceVO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Excel 处理服务类，提供将发票数据写入模板和合并多个 Excel 文件的功能。
 *
 * @author mickey
 */
@Service
public final class ExcelService {
    // 定义每列的宽度（单位：1/256字符宽度）
    private static final int[] COLUMN_WIDTHS = {
            256 * 5,   // 编号: 5字符宽度
            256 * 20,  // 品名: 20字符宽度
            256 * 14,  // 型号: 14字符宽度
            256 * 5,   // 单位: 5字符宽度
            256 * 5,  // 数量: 5字符宽度
            256 * 10,  // 金额: 10字符宽度
            256 * 8,  // 税额: 8字符宽度
            256 * 10,  // 小计: 10字符宽度
            256 * 5   // 备注: 5字符宽度
    };
    /**
     * 将发票数据写入 Excel 模板并保存到指定路径。
     *
     * @param invoiceVO    发票数据对象，包含买家信息、日期、明细列表等
     * @param templatePath Excel 模板文件路径
     * @param outputPath   输出文件路径
     * @throws IOException 如果文件操作失败
     */
    public void writeInvoiceToExcel(InvoiceVO invoiceVO, String templatePath, String outputPath) throws IOException {
        try (FileInputStream fileIn = new FileInputStream(templatePath); FileOutputStream fileOut = new FileOutputStream(outputPath); Workbook workbook = new XSSFWorkbook(fileIn)) {

            // 获取第一个工作表
            Sheet sheet = workbook.getSheetAt(0);

            // 创建字体 宋体
            Font headerFont = workbook.createFont();
            headerFont.setFontName("宋体");
            // 设置字体大小（单位：点）
            headerFont.setFontHeightInPoints((short) 10);

            // 创建单元格样式并应用字体
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setWrapText(true);  // 启动自动换行

            // 填充项目名称和日期（第2行，索引为1）
            Row dateRow = sheet.getRow(1);
            Cell projectNameCell = dateRow.createCell(2);   // C2
            projectNameCell.setCellValue(invoiceVO.getBuyerName());
            projectNameCell.setCellStyle(headerStyle);

            // 填充日期
            Cell dateCell = dateRow.createCell(7);  // H2
            dateCell.setCellValue(invoiceVO.getDate());
            dateCell.setCellStyle(headerStyle);

            // 创建带边框的单元格样式用于数据表格
            Font dataFont = workbook.createFont();
            dataFont.setFontName("宋体");
            // 设置字体大小（单位：点）
            dataFont.setFontHeightInPoints((short) 10);
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setFont(dataFont);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setRightBorderColor(IndexedColors.BLACK.getIndex());
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setLeftBorderColor(IndexedColors.BLACK.getIndex());
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());
            dataStyle.setWrapText(true);    // 启动自动换行

            // 数据起始行（第5行，索引为4）
            int currentRow = 4;
            int detailSize = invoiceVO.getDetailList().size();

            // 如果明细超过10行，向下移动合计行
            int detailTemplateSize = 1; // 模板里设置了 1 行还是 10 行，这里做个冗余操作，方便修改
            int templateDetailFinished = detailSize > detailTemplateSize ? currentRow + detailSize : currentRow + detailTemplateSize;
            if (detailSize > detailTemplateSize) {
                sheet.shiftRows(currentRow + detailTemplateSize, templateDetailFinished, detailSize - detailTemplateSize);
            }

            // 填充明细数据
            for (InvoiceDetailVO invoiceDetailVO : invoiceVO.getDetailList()) {
                Row dataRow = sheet.getRow(currentRow);
                if (dataRow == null) {
                    dataRow = sheet.createRow(currentRow);
                    for (int i = 0; i < 9; i++) {
                        dataRow.createCell(i);  // 创建9列
                    }
                }

                // 填充每列数据并应用样式
                dataRow.getCell(0).setCellValue(invoiceDetailVO.getIndex());        // 编号
                dataRow.getCell(0).setCellStyle(dataStyle);
                dataRow.getCell(1).setCellValue(invoiceDetailVO.getName());         // 品名
                dataRow.getCell(1).setCellStyle(dataStyle);
                dataRow.getCell(2).setCellValue(invoiceDetailVO.getModel());        // 型号（规格）
                dataRow.getCell(2).setCellStyle(dataStyle);
                dataRow.getCell(3).setCellValue(invoiceDetailVO.getUnit());         // 单位
                dataRow.getCell(3).setCellStyle(dataStyle);
                dataRow.getCell(4).setCellValue(invoiceDetailVO.getCount());        // 入库数量
                dataRow.getCell(4).setCellStyle(dataStyle);
                dataRow.getCell(5).setCellValue(invoiceDetailVO.getAmount());       // 金额（元）
                dataRow.getCell(5).setCellStyle(dataStyle);
                dataRow.getCell(6).setCellValue(invoiceDetailVO.getTaxAmount());    // 税额（元）
                dataRow.getCell(6).setCellStyle(dataStyle);
                dataRow.getCell(7).setCellValue(invoiceDetailVO.getTotalAmount());  // 金额小计
                dataRow.getCell(7).setCellStyle(dataStyle);
                dataRow.getCell(8).setCellValue(invoiceDetailVO.getRemark());       // 备注
                dataRow.getCell(8).setCellStyle(dataStyle);

                // 自动调整行高
                dataRow.setHeight((short) -1);  // -1 表示自动调整行高
                currentRow++;
            }

            // 设置每列宽度
            for (int i = 0; i < COLUMN_WIDTHS.length; i++) {
                sheet.setColumnWidth(i, COLUMN_WIDTHS[i]);
            }

            // 填充合计行（默认第15行，或数据行后）
            Row totalRow = sheet.getRow(templateDetailFinished);
            if (totalRow == null) {
                totalRow = sheet.createRow(templateDetailFinished);
            }
            Cell totalAmountCell = totalRow.createCell(0);  // A15
            totalAmountCell.setCellValue("金额合计（大写）：" + invoiceVO.getTotalAmountString());
            totalAmountCell.setCellStyle(dataStyle);

            Cell totalValueCell = totalRow.createCell(7);   // H15
            totalValueCell.setCellValue(invoiceVO.getTotalAmount());
            totalValueCell.setCellStyle(dataStyle);

            // 自动调整合计行高度
            totalRow.setHeight((short) -1);

            // 保存到文件
            workbook.write(fileOut);
        }
    }

    /**
     * 将多个 Excel 文件的内容向下拼接，合并为一个新的 Excel 文件，并保留合并单元格。
     *
     * @param mergeName 输出的 Excel 文件名
     * @param fileNames 输入的 Excel 文件名列表（可变参数）
     */
    public void merge(String mergeName, String... fileNames) {
        try (FileOutputStream fileOut = new FileOutputStream(mergeName);
             Workbook mergeExcel = new XSSFWorkbook()) {
            // 创建目标工作表
            Sheet mergeSheet = mergeExcel.createSheet("MergedSheet");
            int currentRowIndex = 0; // 记录当前行号

            // 创建分割线样式
            CellStyle separatorStyle = mergeExcel.createCellStyle();
            separatorStyle.setBorderTop(BorderStyle.THIN);
            separatorStyle.setTopBorderColor(IndexedColors.BLACK.getIndex());

            // 遍历输入的 Excel 文件
            for (int fileIndex = 0; fileIndex < fileNames.length; fileIndex++) {
                String fileName = fileNames[fileIndex];
                try (FileInputStream fileIn = new FileInputStream(fileName); Workbook workbook = new XSSFWorkbook(fileIn)) {
                    // 获取源文件的第一个工作表
                    Sheet sourceSheet = workbook.getSheetAt(0);
                    // 如果不是第一个文件，插入空行和分割线
                    if (fileIndex > 0) {
                        // 插入1行空行
                        mergeSheet.createRow(currentRowIndex++);

                        // 插入1行分割线
                        Row separatorRow = mergeSheet.createRow(currentRowIndex++);
                        for (int j = 0; j < COLUMN_WIDTHS.length; j++) {
                            Cell cell = separatorRow.createCell(j);
                            cell.setCellStyle(separatorStyle);
                        }

                        // 插入1行空行
                        mergeSheet.createRow(currentRowIndex++);
                    }

                    // 复制合并单元格区域
                    for (int i = 0; i < sourceSheet.getNumMergedRegions(); i++) {
                        CellRangeAddress sourceRegion = sourceSheet.getMergedRegion(i);
                        CellRangeAddress newRegion = new CellRangeAddress(sourceRegion.getFirstRow() + currentRowIndex, sourceRegion.getLastRow() + currentRowIndex, sourceRegion.getFirstColumn(), sourceRegion.getLastColumn());
                        // 检查合并区域是否重叠，避免重复
                        boolean isOverlapping = mergeSheet.getMergedRegions().stream().anyMatch(r -> r.isInRange(newRegion.getFirstRow(), newRegion.getFirstColumn()));
                        if (!isOverlapping) {
                            mergeSheet.addMergedRegion(newRegion);
                        }
                    }

                    // 遍历源工作表中的每一行
                    for (int i = 0; i <= sourceSheet.getLastRowNum(); i++) {
                        Row sourceRow = sourceSheet.getRow(i);
                        if (sourceRow == null) continue; // 跳过空行

                        // 在目标工作表中创建新行
                        Row targetRow = mergeSheet.createRow(currentRowIndex++);
                        targetRow.setHeight((short) -1);    // 自动调整行高

                        // 复制每一列的数据
                        for (int j = 0; j < sourceRow.getLastCellNum(); j++) {
                            Cell sourceCell = sourceRow.getCell(j);
                            Cell targetCell = targetRow.createCell(j);

                            if (sourceCell != null) {
                                // 复制单元格值
                                copyCellValue(sourceCell, targetCell);
                                // 复制单元格样式
                                CellStyle targetStyle = copyCellStyle(sourceCell, targetCell, mergeExcel);
                                targetStyle.setWrapText(true); // 启用自动换行
                                targetCell.setCellStyle(targetStyle);
                            }
                        }
                    }
                } catch (FileNotFoundException e) {
                    throw new RuntimeException("文件未找到: " + fileName, e);
                } catch (IOException e) {
                    throw new RuntimeException("读取文件失败: " + fileName, e);
                }
            }

            // 设置每列宽度（如果有数据）
            if (mergeSheet.getLastRowNum() > 0 && mergeSheet.getRow(0) != null) {
                int columnCount = Math.min(COLUMN_WIDTHS.length, mergeSheet.getRow(0).getLastCellNum());
                for (int i = 0; i < columnCount; i++) {
                    mergeSheet.setColumnWidth(i, COLUMN_WIDTHS[i]);
                }
            }

            // 写入合并后的文件
            mergeExcel.write(fileOut);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("无法创建输出文件: " + mergeName, e);
        } catch (IOException e) {
            throw new RuntimeException("写入合并文件失败: " + mergeName, e);
        }
    }

    /**
     * 复制单元格的值，根据单元格类型处理不同数据。
     *
     * @param sourceCell 源单元格
     * @param targetCell 目标单元格
     */
    private void copyCellValue(Cell sourceCell, Cell targetCell) {
        switch (sourceCell.getCellType()) {
            case STRING:
                targetCell.setCellValue(sourceCell.getStringCellValue());
                break;
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(sourceCell)) {
                    targetCell.setCellValue(sourceCell.getDateCellValue());
                } else {
                    targetCell.setCellValue(sourceCell.getNumericCellValue());
                }
                break;
            case BOOLEAN:
                targetCell.setCellValue(sourceCell.getBooleanCellValue());
                break;
            case FORMULA:
                targetCell.setCellFormula(sourceCell.getCellFormula());
                break;
            case BLANK:
                targetCell.setCellValue((String) null);
                break;
            default:
                targetCell.setCellValue(sourceCell.toString());
        }
    }

    /**
     * 复制单元格样式到目标单元格。
     *
     * @param sourceCell 源单元格
     * @param targetCell 目标单元格
     * @param workbook   目标工作簿，用于创建新样式
     * @return 创建的目标单元格样式
     */
    private CellStyle copyCellStyle(Cell sourceCell, Cell targetCell, Workbook workbook) {
        CellStyle sourceStyle = sourceCell.getCellStyle();
        CellStyle targetStyle = workbook.createCellStyle();
        if (sourceStyle != null) {
            targetStyle.cloneStyleFrom(sourceStyle);
        }
        targetCell.setCellStyle(targetStyle);
        return targetStyle;
    }
}
