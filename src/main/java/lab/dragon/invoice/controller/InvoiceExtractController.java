package lab.dragon.invoice.controller;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import lab.dragon.invoice.VO.InvoiceVO;
import lab.dragon.invoice.entity.Invoice;
import lab.dragon.invoice.service.PdfInvoiceExtractor;
import lab.dragon.invoice.utils.DateUtil;
import lab.dragon.invoice.utils.OFDUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

/**
 * 发票识别 API
 * @author mickey
 */
@RestController
@RequestMapping("invoice")
public class InvoiceExtractController {
    private final Logger log = LoggerFactory.getLogger(InvoiceExtractController.class);

    /**
     * @param file 上传的发票文件,支持pdf和ofd格式
     * @return InvoiceVO
     */
    @PostMapping("extract")
    public InvoiceVO extrat(@RequestParam(value = "file", required = true) MultipartFile file) {
        // 生成一个当前时间的文件名
        String fileName = DateUtil.getDateFormat(DateUtil.FILE_NAME_FORMAT_STRING).format(new Date());
        File dest = null;
        boolean ofd = false;
        if (null != file && !file.isEmpty()) {
            if (file.getOriginalFilename().toLowerCase().endsWith(".ofd")) {
                ofd = true;
                dest = new File(fileName + ".ofd");
            } else {
                dest = new File(fileName + ".pdf");
            }
            if (Files.notExists(Paths.get(dest.getParentFile().getAbsolutePath()))) {
                log.error("文件夹 {} 不存在", dest.getParentFile().getAbsolutePath());
                boolean mkdirsed = dest.getParentFile().mkdirs();
                if (!mkdirsed) {
                    log.error("创建文件夹 {} 失败", dest.getParentFile().getAbsolutePath());
                }
            }

            // 复制文件到一个副本
            try {
                FileUtils.copyInputStreamToFile(file.getInputStream(), dest);
            } catch (IOException e) {
                log.error("[extract]复制文件失败", e);
            }
        }
        Invoice result = null;
        try {
            if (null != dest) {
                if (ofd) {//这里将ofd文件直接转为pdf做抽取
                    log.info("ofd处理...");
                    Path ofdPath = Paths.get(fileName + ".ofd");
                    Path pdfPath = Paths.get(fileName + ".pdf");
                    String pdfFilePath = OFDUtils.ofdtoPdf(ofdPath, pdfPath);
                    result = PdfInvoiceExtractor.extract(new File(pdfFilePath));
                    result.setMsgCode(200);
                    result.setMsg("返回成功！");
                } else {
                    result = PdfInvoiceExtractor.extract(dest);
                    result.setMsgCode(200);
                    result.setMsg("返回成功！");
                }
                //不保存上传的文件，删除
//                if (null != result.getAmount()) {
//                    dest.delete();
//                }
            } else {
                result = new Invoice();
                result.setMsgCode(500);
                result.setMsg("检测输入参数是否正确！");
            }
        } catch (IOException e) {
            e.printStackTrace();
            result = new Invoice();
            result.setMsgCode(500);
            result.setMsg("检测输入参数是否正确！");
        }
        return result.convert2VO();
    }
}
