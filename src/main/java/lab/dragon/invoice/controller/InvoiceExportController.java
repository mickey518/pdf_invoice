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

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Date;

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
    public String[] export(@RequestBody InvoiceVO invoiceVO) {
        // 生成时间戳字符串
        String dateString = DateUtil.getDateFormat(DateUtil.FILE_NAME_FORMAT_STRING).format(new Date());

        // 定义需要的 3 个模板文件名
        String[] templates = new String[] {
            "inventory_template.xlsx", "inventory_template_1.xlsx","inventory_template_2.xlsx"
        };
        // 定义需要的 3 个模板文件类型
        String[] outputTypes = new String[] {
                 "入库单（仓库联）-", "入库单（财务记账联）-", "出库单-"
        };
        String[] results = new String[templates.length];

        for (int i = 0; i < templates.length; i++) {
            try {
                String outputName = Paths.get(dataFolder,  String.format("%s-%s.xlsx", outputTypes[i], dateString)).toString();
                excelService.writeInvoiceToExcel(
                        invoiceVO,
                        Paths.get(templateFolder, templates[i]).toString(),
                        outputName);
                results[i] = exportUrl + outputName;
            } catch (IOException e) {
                log.error("无法写入文件：" + e.getMessage(), e);
                results[i] = "";
            }
        }

        return results;
    }
}
