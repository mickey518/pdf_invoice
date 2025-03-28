package lab.dragon.invoice.controller;

import lab.dragon.invoice.VO.InvoiceDetailVO;
import lab.dragon.invoice.utils.AmountToChinese;
import lab.dragon.invoice.utils.FileChecksum;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 发票识别 API
 * @author mickey
 */
@RestController
@RequestMapping("invoice")
public class InvoiceExtractController {
    private final Logger log = LoggerFactory.getLogger(InvoiceExtractController.class);

    @Value("${invoice.folder.data}")
    private String dataFolder;

    /**
     * @param files 上传的发票文件数组
     * @return InvoiceVO
     */
    @PostMapping("extract")
    public InvoiceVO extract(@RequestParam(value = "files") MultipartFile[] files) {

        InvoiceVO invoiceVO = new InvoiceVO();
        List<InvoiceDetailVO> details = new ArrayList<>();
        BigDecimal totalAmount = new BigDecimal("0.0");

        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            // 生成一个当前时间的文件名
            String fileName = DateUtil.getDateFormat(DateUtil.FILE_NAME_FORMAT_STRING).format(new Date());
            File dest = null;
            boolean ofd = false;
            if (null != file && !file.isEmpty()) {
                if (file.getOriginalFilename().toLowerCase().endsWith(".ofd")) {
                    ofd = true;
                    dest = new File(Paths.get(dataFolder, fileName + ".ofd").toUri());
                } else {
                    dest = new File(Paths.get(dataFolder, fileName + ".pdf").toUri());
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
                        Path ofdPath = Paths.get(dataFolder, fileName + ".ofd");
                        Path pdfPath = Paths.get(dataFolder, fileName + ".pdf");
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

            InvoiceVO convert2VO = result.convert2VO();
            convert2VO.setMsgCode(result.getMsgCode());
            convert2VO.setMsg(result.getMsg());

            details.addAll(convert2VO.getDetailList());

            invoiceVO.setDate(convert2VO.getDate());
            totalAmount = totalAmount.add(result.getTotalAmount());
        }

        for (int i = 0; i < details.size(); i++) {
            details.get(i).setIndex(i + 1);
        }

        invoiceVO.setDetailList(details);
        invoiceVO.setTotalAmount(totalAmount.toString());
        invoiceVO.setTotalAmountString(AmountToChinese.numberToChinese(totalAmount.toString()));

        return invoiceVO;
    }

    /**
     * @param files 上传的发票文件数组
     * @return List<Invoice> invoice 列表
     */
    @PostMapping("extract/files")
    public ResponseEntity<?> extractFiles(@RequestParam(value = "files") MultipartFile[] files) {
        List<Invoice> invoices = new ArrayList<>();

        for (int i = 0; i < files.length; i++) {
            Invoice result = null;
            MultipartFile file = files[i];
            String sha256 = null;
            try {
                sha256 = FileChecksum.sha256(file.getInputStream());
            } catch (IOException | NoSuchAlgorithmException e) {
                return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(e.getMessage());
            }

            // 生成一个当前时间的文件名
            String fileName = DateUtil.getDateFormat(DateUtil.FILE_NAME_FORMAT_STRING).format(new Date());
            File dest = null;
            boolean ofd = false;
            if (null != file && !file.isEmpty()) {
                if (file.getOriginalFilename().toLowerCase().endsWith(".ofd")) {
                    ofd = true;
                    dest = new File(Paths.get(dataFolder, fileName + ".ofd").toUri());
                } else {
                    dest = new File(Paths.get(dataFolder, fileName + ".pdf").toUri());
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

            try {
                if (null != dest) {
                    if (ofd) {//这里将ofd文件直接转为pdf做抽取
                        log.info("ofd处理...");
                        Path ofdPath = Paths.get(dataFolder, fileName + ".ofd");
                        Path pdfPath = Paths.get(dataFolder, fileName + ".pdf");
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
                    result.setMsgCode(500);
                    result.setMsg("检测输入参数是否正确！");
                }
            } catch (IOException e) {
                e.printStackTrace();
                result.setMsgCode(500);
                result.setMsg("检测输入参数是否正确！");
            }

            invoices.add(result);
        }

        return ResponseEntity.ok(invoices);
    }
}
