package lab.dragon.invoice.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lab.dragon.invoice.VO.InvoiceVO;
import lab.dragon.invoice.utils.DateUtil;
import lab.dragon.invoice.utils.Invoice2ExcelUtil;

import java.io.IOException;
import java.util.Date;

/**
 * @author mickey.wang
 */
@RestController
@RequestMapping("inventory")
public class InventoryExportController {

    @PostMapping("export")
    public String exportInventory(@RequestBody InvoiceVO invoiceVO) {
        String fileName = DateUtil.getDateFormat(DateUtil.FILE_NAME_FORMAT_STRING).format(new Date());
        String templatePath = "inventory_template.xlsx";
        String outputPath = fileName + "-inventory" + ".xlsx";
        try {
            Invoice2ExcelUtil.writeInvoiceToExcel(invoiceVO, templatePath, outputPath);
            return outputPath;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
