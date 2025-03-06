package lab.dragon.invoice.controller;

import lab.dragon.invoice.VO.InvoiceVO;
import lab.dragon.invoice.utils.DateUtil;
import lab.dragon.invoice.service.ExcelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.nio.file.Paths;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author mickey.wang
 */
@RestController
@RequestMapping("inventory")
public class InvoiceExportController {

    private static final Logger log = LoggerFactory.getLogger(InvoiceExportController.class);
    @Value("${invoice.folder.templates}")
    private String templateFolder;
    @Value("${invoice.folder.data}")
    private String dataFolder;
    @Value("${invoice.export.url}")
    private String exportUrl;

    @Autowired
    private ExcelService excelService;

    @PostMapping("export")
    public String export(@RequestBody InvoiceVO invoiceVO) {
        // 生成时间戳字符串
        String dateString = DateUtil.getDateFormat(DateUtil.FILE_NAME_FORMAT_STRING).format(new Date());

        // 定义需要的 3 个模板文件名
        String[] templates = new String[] {
            "inventory_template.xlsx", "inventory_template_1.xlsx","inventory_template_2.xlsx"
        };
        // 定义需要的 3 个模板文件类型
        String[] outputTypes = new String[] {
                 "入库单（仓库联）", "入库单（财务记账联）", "出库单"
        };
        String[] results = new String[templates.length];

        for (int i = 0; i < templates.length; i++) {
            try {
                String outputName = Paths.get(dataFolder,  String.format("%s-%s.xlsx", dateString, outputTypes[i])).toString();
                excelService.writeInvoiceToExcel(
                        invoiceVO,
                        Paths.get(templateFolder, templates[i]).toString(),
                        outputName);
                results[i] = outputName;
            } catch (IOException e) {
                log.error("无法写入文件：" + e.getMessage(), e);
                results[i] = "";
            }
        }

        String string = zipFiles(results);
        return exportUrl + string;
    }

    private String zipFiles(String[] fileNames) {
        String dateString = DateUtil.getDateFormat(DateUtil.FILE_NAME_FORMAT_STRING).format(new Date());

        String zipFilePath = Paths.get(dataFolder,  dateString + "-发票识别导出.zip").toString();
        try (FileOutputStream fos = new FileOutputStream(zipFilePath);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            for (String fileName : fileNames) {
                File file = new File(fileName);
                if (!file.exists()) {
                    log.error("文件 {} 不存在，跳过压缩。", fileName);
                    continue;
                }

                try (FileInputStream fis = new FileInputStream(file)) {
                    ZipEntry zipEntry = new ZipEntry(file.getName());
                    zos.putNextEntry(zipEntry);

                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        zos.write(buffer, 0, bytesRead);
                    }
                } catch (IOException e) {
                    log.error("读取文件 {} 出错：{}", fileName, e.getMessage(), e);
                }
            }
        } catch (IOException e) {
            log.error("创建或写入 ZIP 文件出错：{}", e.getMessage(), e);
        }
        return zipFilePath;
    }
}
