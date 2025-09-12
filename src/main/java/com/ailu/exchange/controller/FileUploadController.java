package com.ailu.exchange.controller;//package com.ailu.exchange.controller;
//
//
//import com.ailu.exchange.service.FileUploadService;
//import com.ailu.exchange.utils.web.WebResult;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.lang3.StringUtils;
//import org.apache.poi.ss.usermodel.Cell;
//import org.apache.poi.ss.usermodel.Row;
//import org.apache.poi.ss.usermodel.Sheet;
//import org.apache.poi.ss.usermodel.Workbook;
//import org.apache.poi.xssf.usermodel.XSSFWorkbook;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//
//import javax.annotation.Resource;
//import java.io.IOException;
//import java.io.InputStream;
//import java.util.HashMap;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/upload")
//@CrossOrigin(origins = "*", maxAge = 3600)
//@Slf4j
//public class FileUploadController {
//
//
//    @Resource
//    FileUploadService fileUploadService;
//
//    @PostMapping("/excel")
//    public Object handleFileUpload(
//            @RequestParam("file") MultipartFile file,
//            @RequestParam("tokenContractAddress") String tokenContractAddress,
//            @RequestParam("tokenName") String tokenName,
//            @RequestParam("chainShortName") String chainShortName) {
//
//        if (StringUtils.isEmpty(tokenContractAddress) || StringUtils.isEmpty(tokenName) || StringUtils.isEmpty(chainShortName)) {
//
//            return WebResult.failResult("输入的参数不能为空都是必填的");
//        }
//        if (file.isEmpty()) {
//            return WebResult.failResult("上传文件为空");
//        }
//
//        Map<String, String> setMap = new HashMap<>();
//
//        try (InputStream is = file.getInputStream()) {
//            Workbook workbook = new XSSFWorkbook(is);
//            Sheet sheet = workbook.getSheetAt(0);
//            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
//                Row row = sheet.getRow(i);
//                if (row == null) continue;
//                Cell holderAddressCell = row.getCell(0);
//                if (holderAddressCell == null) continue;
//                Cell labelInfo = row.getCell(1);
//                if (labelInfo == null) {
//                    continue;
//                }
//                ;
//                Cell addressAddCell = row.getCell(2);
//                String holderAddress = holderAddressCell.getStringCellValue();
//                if (StringUtils.isEmpty(holderAddress)) {
//                    continue;
//                }
//                String addressAdd = null;
//                if (addressAddCell != null) {
//                    addressAdd = addressAddCell.getStringCellValue();
//                }
//                String label = labelInfo.getStringCellValue();
//                if (StringUtils.isNotEmpty(addressAdd)) {
//                    holderAddress = holderAddress + "," + addressAdd;
//                }
//
//                if (holderAddress.endsWith(",")) {
//                    holderAddress = holderAddress.substring(0, holderAddress.length() - 1);
//                }
//                // 如果map中已经存在这个label，则拼接，否则直接插入
//                setMap.merge(label, holderAddress.trim(), (existingValue, newValue) -> existingValue + "," + newValue);
//            }
//
//            fileUploadService.addTokenInfo(setMap, tokenContractAddress, tokenName, chainShortName);
//
//
//            // 假设文件解析成功后返回成功响应
//            return WebResult.okResult("文件上传并解析成功");
//        } catch (IOException e) {
//            e.printStackTrace();
//            return WebResult.failResult("上传或解析文件时发生错误");
//        }
//    }
//}
