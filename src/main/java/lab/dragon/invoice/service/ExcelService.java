package lab.dragon.invoice.service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import lab.dragon.invoice.VO.InvoiceDetailVO;
import lab.dragon.invoice.VO.InvoiceVO;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

/**
 * Excel 处理服务类.
 *
 * @author mickey
 */
@Service
public final class ExcelService {

    public void writeInvoiceToExcel(InvoiceVO invoiceVO, String templatePath, String outputPath) throws IOException {
        // 加载模板
        try (FileInputStream fileIn = new FileInputStream(templatePath);
             FileOutputStream fileOut = new FileOutputStream(outputPath);
             Workbook workbook = new XSSFWorkbook(fileIn)) {

            // 获取第一个工作表
            Sheet sheet = workbook.getSheetAt(0);

            // 创建字体 宋体
            Font font = workbook.createFont();
            font.setFontName("宋体");
            // 设置字体大小（单位：点）
            font.setFontHeightInPoints((short) 12);

            CellStyle cellStyle = workbook.createCellStyle();
            cellStyle.setFont(font);

            // 填充项目名称及日期
            Row dateRow = sheet.getRow(1);
            Cell projectNameCell = dateRow.createCell(3);
            projectNameCell.setCellValue(invoiceVO.getBuyerName());  // 根据实际需求设置项目名称
            projectNameCell.setCellStyle(cellStyle);

            // 填充日期
            Cell dateCell = dateRow.createCell(7);
            dateCell.setCellValue(invoiceVO.getDate());
            dateCell.setCellStyle(cellStyle);

            cellStyle = workbook.createCellStyle();
            cellStyle.setFont(font);

            // 填充表格数据
            // 数据起始行
            int currentRow = 4;

            // 判断数据行数，如果超过 10 行，要先将下面的数据向下移动 size - 10 行
            int size = invoiceVO.getDetailList().size();
            if (size > 10) {
                sheet.shiftRows(14, 14 + size - 10, size - 10);
            }

            // 添加边框样式
            cellStyle.setBorderRight(BorderStyle.MEDIUM);
            cellStyle.setRightBorderColor(IndexedColors.BLACK.getIndex());
            cellStyle.setBorderLeft(BorderStyle.MEDIUM);
            cellStyle.setLeftBorderColor(IndexedColors.BLACK.getIndex());
            cellStyle.setBorderBottom(BorderStyle.MEDIUM);
            cellStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());

            for (InvoiceDetailVO invoiceDetailVO : invoiceVO.getDetailList()) {

                Row dataRow = sheet.getRow(currentRow);
                if (dataRow == null) {
                    dataRow = sheet.createRow(currentRow);
                    for (int i = 0; i < 9; i++) {
                        dataRow.createCell(i);
                    }
                }
                // 编号
                dataRow.getCell(0).setCellValue(invoiceDetailVO.getIndex());
                dataRow.getCell(0).setCellStyle(cellStyle);
                // 品名
                dataRow.getCell(1).setCellValue(invoiceDetailVO.getName());
                dataRow.getCell(1).setCellStyle(cellStyle);
                // 型号（规格）
                dataRow.getCell(2).setCellValue(invoiceDetailVO.getModel());
                dataRow.getCell(2).setCellStyle(cellStyle);
                // 单位
                dataRow.getCell(3).setCellValue(invoiceDetailVO.getUnit());
                dataRow.getCell(3).setCellStyle(cellStyle);
                // 入库数量
                dataRow.getCell(4).setCellValue(invoiceDetailVO.getCount());
                dataRow.getCell(4).setCellStyle(cellStyle);
                // 金额（元）
                dataRow.getCell(5).setCellValue(invoiceDetailVO.getAmount());
                dataRow.getCell(5).setCellStyle(cellStyle);
                // 税额（元）
                dataRow.getCell(6).setCellValue(invoiceDetailVO.getTaxAmount());
                dataRow.getCell(6).setCellStyle(cellStyle);
                // 金额小计
                dataRow.getCell(7).setCellValue(invoiceDetailVO.getTotalAmount());
                dataRow.getCell(7).setCellStyle(cellStyle);
                // 备注
                dataRow.getCell(8).setCellValue("");
                dataRow.getCell(8).setCellStyle(cellStyle);

                currentRow++;
            }

            // 填充合计
            Row totalRow = sheet.getRow(Math.max(14, currentRow));
            Cell totalAmountCell = totalRow.createCell(0);
            totalAmountCell.setCellValue("金额合计（大写）：" + invoiceVO.getTotalAmountString());
            totalAmountCell.setCellStyle(cellStyle);

            Cell totalValueCell = totalRow.createCell(7);
            totalValueCell.setCellValue(invoiceVO.getTotalAmount());
            totalValueCell.setCellStyle(cellStyle);

            // 保存到文件
            workbook.write(fileOut);
        }
    }
}
