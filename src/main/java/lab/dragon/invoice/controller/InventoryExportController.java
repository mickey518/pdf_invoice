package lab.dragon.invoice.controller;

import lab.dragon.invoice.VO.InvoiceVO;
import lab.dragon.invoice.utils.DateUtil;
import lab.dragon.invoice.utils.Invoice2ExcelUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Date;

/**
 * @author mickey.wang
 */
@RestController
@RequestMapping("inventory")
public class InventoryExportController {

    private static final Logger log = LoggerFactory.getLogger(InventoryExportController.class);
    @Value("${invoice.folder.templates}")
    private String templateFolder;
    @Value("${invoice.folder.data}")
    private String dataFolder;
    @Value("${invoice.export.url}")
    private String exportUrl;

    @PostMapping("export")
    public String exportInventory(@RequestBody InvoiceVO invoiceVO) {
        String fileName = DateUtil.getDateFormat(DateUtil.FILE_NAME_FORMAT_STRING).format(new Date());
        String templatePath = Paths.get(templateFolder, "inventory_template.xlsx").toString();
        String outputPath = Paths.get(dataFolder, fileName + "-inventory" + ".xlsx").toString();

        try {
            Invoice2ExcelUtil.writeInvoiceToExcel(invoiceVO, templatePath, outputPath);
            return exportUrl + outputPath;
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}
