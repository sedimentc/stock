//package com.ailu.exchange;
//
//
//import com.ailu.exchange.dao.domaim.AddressTxMonitor;
//import com.ailu.exchange.dao.domaim.TokenInfo;
//import com.ailu.exchange.dao.domaim.TokenMainInfo;
//import com.ailu.exchange.dao.mapper.source1.TokenMainInfoMapper;
//import com.ailu.exchange.dao.vo.AddressInfoVo;
//import com.ailu.exchange.dao.vo.ContractInfoVo;
//import com.ailu.exchange.dao.vo.DailyVo;
//import com.ailu.exchange.dao.vo.SpxVo;
//import com.ailu.exchange.rest.api.OkLinkApi;
//import com.alibaba.fastjson.JSON;
//import com.alibaba.fastjson.JSONArray;
//import com.alibaba.fastjson.JSONObject;
//import lombok.Data;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.lang3.StringUtils;
//import org.apache.poi.ss.usermodel.*;
//import org.apache.poi.xssf.usermodel.XSSFWorkbook;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.junit4.SpringRunner;
//
//import javax.annotation.Resource;
//import java.io.FileInputStream;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.math.BigDecimal;
//import java.time.Instant;
//import java.time.LocalDateTime;
//import java.time.ZoneId;
//import java.time.format.DateTimeFormatter;
//import java.time.temporal.ChronoUnit;
//import java.util.*;
//import java.util.stream.Collectors;
//
//@RunWith(SpringRunner.class)
//@SpringBootTest
//@Slf4j
//public class ExchangeApplicationTests {
//    @Resource
//    OkLinkApi okLinkApi;
//    @Value("${oklink.apikey}")
//    private String apikey;
//
//    @Resource
//    TokenMainInfoMapper tokenMainInfoMapper;
//
//    private static final Map<String, String> MAP = new HashMap<>();
//
//    static {
//        MAP.put("eth", "0xE0f63A424a4439cBE457D80E4f4b51aD25b2c56C");
//        MAP.put("base", "0x50dA645f148798F68EF2d7dB7C1CB22A6819bb2C");
//    }
//
//    //    @Test
//    public void Test2() {
//        // Excel 文件路径
//        String filePath = "C:\\Users\\bicoin\\Desktop\\virtual.xlsx";
//        List<TokenInfo> tokenInfoList = getHolderAddress(filePath);
//
//// 合约地址集合
//        Set<String> tokenInfosContract = new HashSet<>();
//// 交易所地址集合
//        Set<String> tokenInfosExchange = new HashSet<>();
//// 其他备注地址集合
//        Set<String> tokenInfosOther = new HashSet<>();
//
//// 将列表转换为 Map
//        Map<String, TokenInfo> tokenInfoMap =
//                tokenInfoList.stream().filter(tokenInfo -> StringUtils.isNotEmpty(tokenInfo.getAddress())) // 确保地址非空
//                        .collect(Collectors.toMap(TokenInfo::getAddress, tokenInfo -> tokenInfo,
//                                (existing, replacement) -> existing)); // 处理重复键
//
//        tokenInfoMap.values().forEach(tokenInfo -> {
//            String address = tokenInfo.getAddress();
//            String type = tokenInfo.getType();
//            if (StringUtils.isNotEmpty(type)) {
//                if ("合约".equalsIgnoreCase(type)) {
//                    tokenInfosContract.add(address);
//                } else if ("bybit".equalsIgnoreCase(type) || "mx".equalsIgnoreCase(type) || "gate".equalsIgnoreCase(type) ||
//                        "交易所".equalsIgnoreCase(type)) {
//                    tokenInfosExchange.add(address);
//                } else {
//                    tokenInfosOther.add(address);
//                }
//            }
//        });
//
//        List<TokenInfo> tokenInfos = new ArrayList<>();
//
//        for (Map.Entry<String, TokenInfo> stringTokenInfoEntry : tokenInfoMap.entrySet()) {
//            TokenInfo value = stringTokenInfoEntry.getValue();
//
//            String address = value.getAddress();
//            String type = value.getType();
//            if (StringUtils.isNotEmpty(type) && "合约".equalsIgnoreCase(type)) {
//                continue;
//            }
//            if (StringUtils.isNotEmpty(type) && "bybit".equalsIgnoreCase(type) || "mx".equalsIgnoreCase(type) || "gate".equalsIgnoreCase(type) || "交易所".equalsIgnoreCase(type)) {
//                continue;
//            }
//            if (StringUtils.isNotEmpty(type)) {
//                continue;
//            }
//            String fromNow = value.getFrom();
//            // 判断关联地址是否为跨链地址
//            if (StringUtils.isNotEmpty(fromNow) && "0x0000000000000000000000000000000000000000".equals(fromNow)) {
//                value.setType("跨链");
//            }
//            // 查询首次入账地址的的最上级
//            if (StringUtils.isNotEmpty(fromNow) && !"0x0000000000000000000000000000000000000000".equals(fromNow) && !tokenInfosContract.contains(fromNow)
//                    && !tokenInfosExchange.contains(fromNow) && !tokenInfosOther.contains(fromNow)) {
//                // 如果地址不是由合约转入的币，则进行查询
//                TokenTransaction base = getTokenTransactionList("base", fromNow);
//                String addFrom = "";
//                // 迭代查询 base，直到为 null 或查询到合约地址
//                while (base != null) {
//                    String from = base.getFrom();
//                    addFrom = from;
//                    if (base.getAddress().equals("疑似合约")) {
//                        break; // 如果是合约地址，停止继续查询
//                    }
//                    if (tokenInfosContract.contains(from) || tokenInfosExchange.contains(from) || tokenInfosOther.contains(from) || "0x0000000000000000000000000000000000000000".equals(from)) {
//                        break; // 如果是合约地址，停止继续查询
//                    }
//                    base = getTokenTransactionList("base", from); // 查询下一个 base
//                }
//                if (StringUtils.isNotEmpty(addFrom) && tokenInfosContract.contains(addFrom)) {
//                    value.setTopLevelAddress(addFrom + ",(合约)");
//                } else if (StringUtils.isNotEmpty(addFrom) && tokenInfosExchange.contains(addFrom)) {
//                    value.setTopLevelAddress(addFrom + ",(交易所)");
//                } else if (StringUtils.isNotEmpty(addFrom) && tokenInfosOther.contains(addFrom)) {
//                    value.setTopLevelAddress(addFrom + ",(其他)");
//                } else if (StringUtils.isNotEmpty(addFrom) && "0x0000000000000000000000000000000000000000".equals(addFrom)) {
//                    value.setType("跨链");
//                } else {
//                    value.setTopLevelAddress(addFrom);
//                }
//            }
//            BigDecimal exchangeToAmount = getTokenTransactionListNew("base", address, tokenInfosExchange);
//            if (exchangeToAmount.abs().compareTo(new BigDecimal(100000)) > 0) {
//                value.setType("转交易所地址");
//            }
//            //交易数量
//            long transactionCount = Long.parseLong(value.getTransactionCount());
//            Long transactionTime = value.getTransactionTime();
//            BigDecimal otherAmount = new BigDecimal(value.getOtherAmount());
//
//            //交易数量小于 10 的，且vir金额来源在 100天外，账户其他代币金额小于 1000 的，记0 分。
//            if (transactionCount < 10L && !isWithin100Days(transactionTime) && otherAmount.compareTo(new BigDecimal(1000)) < 0) {
//                value.setScore(0);
//                //交易数量大于 10，且vir金额来源在 100天外，账户其他代币金额小于 1000 的，记 3 分。
//            } else if (transactionCount > 10 && !isWithin100Days(transactionTime) && otherAmount.compareTo(new BigDecimal(1000)) < 0) {
//                value.setScore(3);
//                //交易数量大于 10，vir金额来源在 100天内，记5 分
//            } else if (transactionCount > 10 && isWithin100Days(transactionTime)) {
//                value.setScore(5);
//                //vir金额来源在 100天内，账户除 vir其他代币金额大于 20000 的，记 6 分。
//            } else if (isWithin100Days(transactionTime) && otherAmount.compareTo(new BigDecimal(20000)) > 0) {
//                value.setScore(5);
//                //交易数量大于 50 的，记 7 分 。
//            } else if (transactionCount > 50) {
//                value.setScore(7);
//            } else {
//                log.info("其他情况:{}", JSON.toJSONString(value));
//                value.setScore(0);
//            }
//            if (StringUtils.isNotEmpty(fromNow) && tokenInfosExchange.contains(fromNow) && value.getScore() <= 5) {
//                value.setType("可疑地址");
//            }
//            String topLevelAddress = value.getTopLevelAddress();
//            if (StringUtils.isNotEmpty(topLevelAddress) && tokenInfosExchange.contains(topLevelAddress) && value.getScore() <= 5) {
//                value.setType("可疑地址");
//            }
//            tokenInfos.add(value);
//        }
//        Map<String, TokenInfo> tokenInfoMapNew =
//                tokenInfos.stream().filter(tokenInfo -> StringUtils.isNotEmpty(tokenInfo.getAddress())) // 确保地址非空
//                        .collect(Collectors.toMap(TokenInfo::getAddress, tokenInfo -> tokenInfo,
//                                (existing, replacement) -> existing)); // 处理重复键
//        xieRuData(filePath, tokenInfoMapNew);
//    }
//
//
//    public boolean isWithin100Days(Long transactionTime) {
//        if (transactionTime == null) {
//            return false; // 如果时间为空，默认返回 false
//        }
//        // 当前时间
//        LocalDateTime now = LocalDateTime.now();
//        // 将 transactionTime 转为 LocalDateTime
//        LocalDateTime transactionDateTime = Instant.ofEpochMilli(transactionTime)
//                .atZone(ZoneId.systemDefault())
//                .toLocalDateTime();
//        // 计算两个时间之间的天数差
//        long daysDifference = ChronoUnit.DAYS.between(transactionDateTime, now);
//        // 判断是否在 100 天以内
//        return daysDifference <= 100;
//    }
//
//
//    /**
//     * 获取到现有的地址信息
//     *
//     * @param filePath
//     * @return
//     */
//    public List<TokenInfo> getHolderAddress(String filePath) {
//        List<TokenInfo> tokenInfos = new ArrayList<>();
//        // 使用 DataFormatter 格式化单元格内容
//        DataFormatter formatter = new DataFormatter();
//        try (FileInputStream fis = new FileInputStream(filePath); Workbook workbook = new XSSFWorkbook(fis)) {
//            // 获取第一个工作表
//            Sheet sheet = workbook.getSheetAt(0);
//            // 从第二行开始遍历数据
//            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
//                Row row = sheet.getRow(i);
//                if (row == null) continue;
//                // 读取每一列的值，确保单元格不为空
//                String holderAddress = getCellValue(row, 0, formatter);
//                String tokenAmount = getCellValue(row, 1, formatter);
//                String otherAmount = getCellValue(row, 2, formatter);
//                String isContract = getCellValue(row, 3, formatter);
//                String transactionCount = getCellValue(row, 4, formatter);
//
//                String transactionTimeStr = getCellValue(row, 5, formatter);
//                long transactionTime = parseTimestamp(transactionTimeStr); // 转换为时间戳
//
//                String amount = getCellValue(row, 6, formatter);
//                String from = getCellValue(row, 7, formatter);
//                String suspectedAssociatedAddress = getCellValue(row, 8, formatter);
//                // 优化 type 字段逻辑：遍历检查第9到15列的值
//                String type = "";
//                for (int j = 9; j <= 15; j++) {
//                    String cellValue = getCellValue(row, j, formatter);
//                    if (StringUtils.isNotEmpty(cellValue)) {
//                        type = cellValue;
//                        break;
//                    }
//                }
//                // 添加到列表中
//                tokenInfos.add(new TokenInfo(holderAddress, tokenAmount, otherAmount, isContract, transactionCount,
//                        transactionTime, amount, from, suspectedAssociatedAddress, type.trim(), 0));
//            }
//        } catch (IOException e) {
//            log.error("出现异常！！", e);
//        }
//        return tokenInfos;
//    }
//
//    /**
//     * 获取单元格的值，并处理空值情况
//     *
//     * @param row       当前行
//     * @param cellIndex 单元格索引
//     * @param formatter DataFormatter 格式化器
//     * @return 单元格内容的字符串值（若为空返回空字符串）
//     */
//    private String getCellValue(Row row, int cellIndex, DataFormatter formatter) {
//        Cell cell = row.getCell(cellIndex);
//        return cell == null ? "" : formatter.formatCellValue(cell);
//    }
//
//
//    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss"; // 日期时间格式
//    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);
//
//    /**
//     * 将日期时间字符串转换为时间戳
//     *
//     * @param dateTimeStr 日期时间字符串，格式为 "yyyy-MM-dd HH:mm:ss"
//     * @return 时间戳（毫秒值）；如果解析失败，则返回 0
//     */
//    private long parseTimestamp(String dateTimeStr) {
//        if (StringUtils.isEmpty(dateTimeStr)) {
//            return 0L; // 空字符串返回 0
//        }
//        try {
//            // 使用现代 API 解析日期时间
//            LocalDateTime localDateTime = LocalDateTime.parse(dateTimeStr, DATE_TIME_FORMATTER);
//            return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
//        } catch (Exception e) {
//            System.err.println("Error parsing date time: " + dateTimeStr);
//            return 0L; // 解析失败返回 0
//        }
//    }
//
//
//    public void xieRuData(String filePath, Map<String, TokenInfo> tokenTransactionMap) {
//
//        try (FileInputStream fis = new FileInputStream(filePath); Workbook workbook = new XSSFWorkbook(fis)) {
//            // 获取第一个工作表
//            Sheet sheet = workbook.getSheetAt(0);
//            // 获取表头行（假设是第一行）
//            Row headerRow = sheet.getRow(0);
//            int lastColumnIndex = headerRow.getLastCellNum();
//            // 在表头后新增两个表头：余额 和 是否合约
//            Cell balanceHeader = headerRow.createCell(lastColumnIndex + 1);
//            balanceHeader.setCellValue("评分");
//            Cell otherAmountHeader = headerRow.createCell(lastColumnIndex + 2);
//            otherAmountHeader.setCellValue("备注");
//            Cell topLevelAddress = headerRow.createCell(lastColumnIndex + 3);
//            topLevelAddress.setCellValue("最上层地址");
//            // 从第二行开始遍历数据，填充新列
//            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
//                Row row = sheet.getRow(i);
//                if (row == null) continue;
//                // 获取 HolderAddress
//                Cell holderAddressCell = row.getCell(0);
//                if (holderAddressCell == null) continue;
//                String holderAddress = holderAddressCell.getStringCellValue();
//                TokenInfo tokenTransaction = tokenTransactionMap.get(holderAddress);
//                String topLevelAddressNew = "";
//                String score = "";
//                String type = "";
//                if (tokenTransaction != null) {
//                    type = tokenTransaction.getType();
//                    score = String.valueOf(tokenTransaction.getScore());
//                    topLevelAddressNew = tokenTransaction.getTopLevelAddress();
//                }
//                // 填充余额
//                row.createCell(lastColumnIndex + 1).setCellValue(type);
//                row.createCell(lastColumnIndex + 2).setCellValue(score);
//                row.createCell(lastColumnIndex + 3).setCellValue(topLevelAddressNew);
//            }
//            // 输出到新文件
//            try (FileOutputStream fos = new FileOutputStream(filePath)) {
//                workbook.write(fos);
//            }
//            System.out.println("Excel 文件处理完成！");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public static String convertTimestampToDate(Long timestamp) {
//        if (timestamp == null) {
//            return null; // 如果时间戳为空，返回 null
//        }
//
//        // 转换为 LocalDateTime
//        LocalDateTime dateTime = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime();
//        // 格式化日期
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//        return dateTime.format(formatter);
//    }
//
//
//    public void xieRuDataNew(String filePath, String tokenContractAddress) {
//
//        try (FileInputStream fis = new FileInputStream(filePath);
//             Workbook workbook = new XSSFWorkbook(fis)) {
//            // 获取第一个工作表
//            Sheet sheet = workbook.getSheetAt(0);
//            // 获取表头行（假设是第一行）
//            Row headerRow = sheet.getRow(0);
//            int lastColumnIndex = headerRow.getLastCellNum();
//            // 在表头后新增两个表头：余额 和 是否合约
//            Cell balanceHeaderNew = headerRow.createCell(1);
//            balanceHeaderNew.setCellValue("最新余额");
//            Cell balanceHeader = headerRow.createCell(lastColumnIndex + 1);
//            balanceHeader.setCellValue("余额");
////
//            Cell otherAmountHeader = headerRow.createCell(lastColumnIndex + 2);
//            otherAmountHeader.setCellValue("其他余额");
//
//            Cell contractHeader = headerRow.createCell(lastColumnIndex + 3);
//            contractHeader.setCellValue("是否合约");
//
//            Cell transactionCountHeader = headerRow.createCell(lastColumnIndex + 4);
//            transactionCountHeader.setCellValue("交易数量");
//            // 从第二行开始遍历数据，填充新列
//            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
//                Row row = sheet.getRow(i);
//                if (row == null) continue;
//                // 获取 HolderAddress
//                Cell holderAddressCell = row.getCell(0);
//                if (holderAddressCell == null) continue;
//                String holderAddress = holderAddressCell.getStringCellValue();
//
//                Map<String, String> tokenBalance = getTokenBalance("base", holderAddress, tokenContractAddress, "VIRTUAL");
//                String virtualQuantity = "0";
//                String otherAmount = "0";
//                if (tokenBalance != null) {
//                    virtualQuantity = tokenBalance.get("virtualQuantity");
//                    otherAmount = tokenBalance.get("otherAmount");
//                }
//                log.info("holderAddress:{},tokenBalance:{}", holderAddress, JSON.toJSONString(tokenBalance));
//                Map<String, String> informationEvm = getInformationEvm("base", holderAddress);
//                String contractAddress = "";
//                String transactionCount = "";
//                if (informationEvm != null) {
//                    contractAddress = informationEvm.get("contractAddress");
//                    transactionCount = informationEvm.get("transactionCount");
//                }
//                // 填充余额
//                row.createCell(1).setCellValue(virtualQuantity);
//                row.createCell(lastColumnIndex + 2).setCellValue(otherAmount);
//                row.createCell(lastColumnIndex + 3).setCellValue(contractAddress);
//                row.createCell(lastColumnIndex + 4).setCellValue(transactionCount);
//            }
//            // 输出到新文件
//            try (FileOutputStream fos = new FileOutputStream(filePath)) {
//                workbook.write(fos);
//            }
//            System.out.println("Excel 文件处理完成！");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//
//    //    @Test
//    public void Test3() {
//        String filePath = "C:\\Users\\bicoin\\Desktop\\virtual.xlsx";
//        String tokenContractAddress = "";
//        xieRuDataNew(filePath, tokenContractAddress);
//
//    }
//
//    @Test
//    public void Test() {
//        for (Map.Entry<String, String> stringStringEntry : MAP.entrySet()) {
//            String value = stringStringEntry.getValue();
//            String key = stringStringEntry.getKey();
//            List<AddressInfoVo> addressInfoVoAll = getAddressInfoVoAll(value, key);
//            List<ContractInfoVo> contractInfoVoList = new ArrayList<>();
//            for (AddressInfoVo addressInfoVo : addressInfoVoAll) {
//                String holderAddress = addressInfoVo.getHolderAddress();
//                String amount = addressInfoVo.getAmount();
//
////                Map<String, String> tokenBalance = getTokenBalance(key, holderAddress, value, "VIRTUAL");
//                String virtualQuantity = "0";
//                String virtualAmountUsd = "0";
//                String otherAmount = "0";
////                if (tokenBalance != null) {
////                    virtualQuantity = tokenBalance.get("virtualQuantity");
////                    virtualAmountUsd = tokenBalance.get("virtualAmountUsd");
////                    otherAmount = tokenBalance.get("otherAmount");
////                }
//                Map<String, String> informationEvm = getInformationEvm(key, holderAddress);
//                String contractAddress = "";
//                String transactionCount = "";
//                if (informationEvm != null) {
//                    contractAddress = informationEvm.get("contractAddress");
//                    transactionCount = informationEvm.get("transactionCount");
//                }
//                contractInfoVoList.add(new ContractInfoVo(holderAddress, amount, virtualAmountUsd, otherAmount,
//                        transactionCount, contractAddress, null, null, null, null));
//            }
//            log.info("获取到JSon数据:{}", JSON.toJSONString(contractInfoVoList));
//        }
//    }
//
//
//    /**
//     * 获取到转入超过多少的地址
//     *
//     * @param chainName
//     * @param address
//     * @return
//     */
//    private TokenTransaction getTokenTransactionList(String chainName, String address) {
//        List<TokenTransaction> tokenTransactions = new ArrayList<>();
//        try {
//            int currentPage = 1; // 初始页数
//            int pageSize = 100; // 每页条数
//            boolean shouldContinue = true; // 控制循环的标志
//            while (shouldContinue) {
//                // 调用 API 获取当前页的数据
//                Thread.sleep(1000L); // 避免请求过快
//                JSONObject body = okLinkApi.getTokenTransactionList(apikey, chainName, address, "token_20",
//                        "0x0b3e328455c4059eeb9e3f84b5543f74e24e7e1b", "to", String.valueOf(currentPage),
//                        String.valueOf(pageSize)).execute().body();
//                if (body != null && body.getInteger("code").equals(0)) {
//                    JSONObject jsonObject = body.getJSONArray("data").getJSONObject(0);
//                    Integer totalPage = jsonObject.getInteger("totalPage");
//                    JSONArray tokenList = jsonObject.getJSONArray("transactionList");
//                    log.info("地址: {}, 当前页码: {}, 返回条数: {}", address, currentPage, tokenList.size());
//                    if (totalPage != null && totalPage.equals(100)) {
//                        log.info("疑似合约地址跳过");
//                        break;
//                    }
//                    for (Object o : tokenList) {
//                        JSONObject jsonObject1 = JSONObject.parseObject(JSON.toJSONString(o));
//                        String from = jsonObject1.getString("from");
//                        Long transactionTime = jsonObject1.getLong("transactionTime");
//                        BigDecimal amount = jsonObject1.getBigDecimal("amount");
//                        tokenTransactions.add(new TokenTransaction(from, address, amount, transactionTime));
//                    }
//                    // 判断是否继续
//                    if (tokenList.size() < pageSize) {
//                        // 当前返回条数小于 100，跳出循环
//                        shouldContinue = false;
//                    } else {
//                        currentPage++; // 获取下一页
//                    }
//                } else {
//                    log.warn("未知错误 address: {}, Page: {}", address, currentPage);
//                    shouldContinue = false; // 停止查询
//                }
//            }
//        } catch (Exception e) {
//            log.error("获取划转记录出现异常！！", e);
//        }
//        return tokenTransactions.stream().filter(tx -> tx.getAmount().compareTo(new BigDecimal("100000")) > 0) // 筛选 amount >
//                .min(Comparator.comparingLong(TokenTransaction::getTransactionTime)) // 找到时间最早的一条
//                .orElse(null); // 如果没有符合条件的，返回 null
//    }
//
//    private BigDecimal getTokenTransactionListNew(String chainName, String address, Set<String> tokenInfosExchange) {
//
//        //转入交易所数量
//        BigDecimal exchangeToAmount = new BigDecimal(0);
//        try {
//            int currentPage = 1; // 初始页数
//            int pageSize = 100; // 每页条数
//            boolean shouldContinue = true; // 控制循环的标志
//            while (shouldContinue) {
//                // 调用 API 获取当前页的数据
//                Thread.sleep(1000L); // 避免请求过快
//                JSONObject body = okLinkApi.getTokenTransactionList(apikey, chainName, address, "token_20",
//                        "0x0b3e328455c4059eeb9e3f84b5543f74e24e7e1b", "from", String.valueOf(currentPage),
//                        String.valueOf(pageSize)).execute().body();
//
//                if (body != null && body.getInteger("code").equals(0)) {
//                    JSONObject jsonObject = body.getJSONArray("data").getJSONObject(0);
//                    Integer totalPage = jsonObject.getInteger("totalPage");
//                    JSONArray tokenList = jsonObject.getJSONArray("transactionList");
//                    log.info("地址: {}, 当前页码: {}, 返回条数: {}", address, currentPage, tokenList.size());
//                    if (totalPage != null && totalPage.equals(100)) {
//                        log.info("疑似合约地址跳过");
//                        break;
//                    }
//                    // 遍历当前页的交易记录
//                    for (Object o : tokenList) {
//                        JSONObject transaction = JSONObject.parseObject(JSON.toJSONString(o));
//                        String to = transaction.getString("to");
//                        BigDecimal amount = transaction.getBigDecimal("amount"); // 转出金额
//                        if (tokenInfosExchange.contains(to)) {
//                            exchangeToAmount = exchangeToAmount.add(amount);
//                        }
//                    }
//                    // 判断是否继续
//                    if (tokenList.size() < pageSize) {
//                        // 当前返回条数小于 100，跳出循环
//                        shouldContinue = false;
//                    } else {
//                        currentPage++; // 获取下一页
//                    }
//                } else {
//                    log.warn("未知错误 address: {}, Page: {}", address, currentPage);
//                    shouldContinue = false; // 停止查询
//                }
//            }
//        } catch (Exception e) {
//            log.error(" 循环出现异常！！ address: {}", address, e);
//        }
//        return exchangeToAmount;
//    }
//
//
//    @Data
//    class TokenTransaction {
//        private String from;
//        private String address;
//        private BigDecimal amount;
//        private Long transactionTime;
//
//        public TokenTransaction(String from, String address, BigDecimal amount, Long transactionTime) {
//            this.from = from;
//            this.address = address;
//            this.amount = amount;
//            this.transactionTime = transactionTime;
//        }
//
//        public TokenTransaction(String from, String address) {
//            this.from = from;
//            this.address = address;
//        }
//
//        public TokenTransaction() {
//        }
//    }
//
//    private Map<String, String> getTokenBalance(String chainName, String address, String tokenContractAddress, String symbolNow) {
//        Map<String, String> map = new HashMap<>();
//        try {
//            Thread.sleep(1000L);
//            JSONObject body =
//                    okLinkApi.getTokenBalance(apikey, chainName, address, "token_20", "50", tokenContractAddress).execute().body();
//            BigDecimal virtualQuantity = BigDecimal.ZERO;
//            BigDecimal virtualAmountUsd = BigDecimal.ZERO;
//            BigDecimal otherAmount = BigDecimal.ZERO;
//            //获取
//            if (body != null && body.getInteger("code").equals(0)) {
//                JSONArray jsonArray = body.getJSONArray("data").getJSONObject(0).getJSONArray("tokenList");
//                for (Object o : jsonArray) {
//                    JSONObject jsonObject = JSONObject.parseObject(JSON.toJSONString(o));
//                    String symbol = jsonObject.getString("symbol");
//                    if (StringUtils.isEmpty(symbol)) {
//                        log.info("该币种为空！！{}", jsonObject);
//                        continue;
//                    }
//                    //金额
//                    String valueUsd = jsonObject.getString("valueUsd");
//                    if (StringUtils.isEmpty(valueUsd)) {
//                        valueUsd = "0";
//                    }
//                    //代币数量
//                    String holdingAmount = jsonObject.getString("holdingAmount");
//                    if (StringUtils.isEmpty(holdingAmount)) {
//                        holdingAmount = "0";
//                    }
//                    if (symbol.equalsIgnoreCase(symbolNow)) {
//                        virtualQuantity = virtualQuantity.add(new BigDecimal(holdingAmount));
//                        virtualAmountUsd = virtualAmountUsd.add(new BigDecimal(valueUsd));
//                    } else {
//                        otherAmount = otherAmount.add(new BigDecimal(valueUsd));
//                    }
//                }
//            } else {
//                log.error("getTokenBalance1异常信息:{}", body);
//            }
////            if (virtualAmountUsd.compareTo(BigDecimal.ZERO) == 0) {
////                JSONObject body2 = okLinkApi.getTokenBalance(apikey, chainName, address, "token_20", "50",
////                        "0x0b3e328455c4059eeb9e3f84b5543f74e24e7e1b").execute().body();
////                if (body2 != null && body2.getInteger("code").equals(0)) {
////                    JSONArray jsonArray = body2.getJSONArray("data").getJSONObject(0).getJSONArray("tokenList");
////                    if (jsonArray.size() > 0) {
////                        for (Object o : jsonArray) {
////                            JSONObject jsonObject = JSONObject.parseObject(JSON.toJSONString(o));
////                            //金额
////                            String valueUsd = jsonObject.getString("valueUsd");
////                            if (StringUtils.isEmpty(valueUsd)) {
////                                valueUsd = "0";
////                            }
////                            //代币数量
////                            String holdingAmount = jsonObject.getString("holdingAmount");
////                            if (StringUtils.isEmpty(holdingAmount)) {
////                                holdingAmount = "0";
////                            }
////                            virtualQuantity = virtualQuantity.add(new BigDecimal(holdingAmount));
////                            virtualAmountUsd = virtualAmountUsd.add(new BigDecimal(valueUsd));
////                        }
////                    }
////                } else {
////                    log.error("getTokenBalance2异常信息:{}", body);
////                }
////            }
//            map.put("virtualQuantity", virtualQuantity.stripTrailingZeros().toPlainString());
//            map.put("virtualAmountUsd", virtualAmountUsd.stripTrailingZeros().toPlainString());
//            map.put("otherAmount", otherAmount.stripTrailingZeros().toPlainString());
//            return map;
//        } catch (Exception e) {
//            log.error("获取账号数据出现异常！！异常地址:{}", address, e);
//        }
//        return null;
//    }
//
//    private List<AddressInfoVo> getAddressInfoVoAll(String tokenContractAddress, String chainName) {
//        List<AddressInfoVo> addressInfoVos = new ArrayList<>();
//        for (int i = 1; i < 3; i++) {
//            try {
//                JSONObject body = okLinkApi.getPositionList(apikey, chainName, tokenContractAddress, i, "100").execute().body();
//                if (body != null && body.getInteger("code").equals(0)) {
//                    JSONArray jsonArray = body.getJSONArray("data").getJSONObject(0).getJSONArray("positionList");
//                    for (Object o : jsonArray) {
//                        JSONObject jsonObject = JSONObject.parseObject(JSON.toJSONString(o));
//                        String holderAddress = jsonObject.getString("holderAddress");
//                        String amount = jsonObject.getString("amount");
//                        addressInfoVos.add(new AddressInfoVo(holderAddress, amount));
//                    }
//                } else {
//                    log.error("getAddressInfoVoAll异常信息:{}", body);
//                }
//            } catch (IOException e) {
//                log.error("获取持有人前两百数据出现异常！！链名:{},地址:{}", chainName, tokenContractAddress, e);
//            }
//        }
//        return addressInfoVos;
//    }
//
//    private Map<String, String> getEntityLabel(Set<String> addressSet, String chainName) {
//        Map<String, String> map = new HashMap<>();
//        // 将 Set 转换为 List，方便分割
//        List<String> addressList = new ArrayList<>(addressSet);
//
//        // 分批次处理，每批最多包含 20 个地址
//        int batchSize = 20;
//        int totalAddresses = addressList.size();
//        for (int i = 0; i < totalAddresses; i += batchSize) {
//            // 计算当前批次的地址范围
//            int end = Math.min(i + batchSize, totalAddresses);
//            List<String> batchAddresses = addressList.subList(i, end);
//            // 将当前批次的地址拼接成以逗号分隔的字符串
//            String addressBatch = String.join(",", batchAddresses);
//            try {
//                // 调用 API 获取当前批次的标签信息
//                JSONObject body = okLinkApi.getEntityLabel(apikey, chainName, addressBatch).execute().body();
//                // 处理 API 返回的结果
//                if (body != null && body.getInteger("code").equals(0)) {
//                    JSONArray jsonArray = body.getJSONArray("data");
//                    for (Object o : jsonArray) {
//                        JSONObject jsonObject = JSONObject.parseObject(JSON.toJSONString(o));
//                        String label = jsonObject.getString("label");
//                        String address = jsonObject.getString("address");
//                        map.put(address, label); // 将地址和标签存入 map
//                    }
//                } else {
//                    log.error("getEntityLabel API 返回异常: {}", body);
//                }
//            } catch (IOException e) {
//                log.error("获取地址标签信息失败！链名：{}, 地址批次：{}", chainName, addressBatch, e);
//            }
//        }
//        return map;
//    }
//
//
//    private Map<String, String> getInformationEvm(String chainName, String address) {
//        Map<String, String> map = new HashMap<>();
//        try {
//            Thread.sleep(1000);
//            JSONObject body = okLinkApi.getInformationEvm(apikey, chainName, address).execute().body();
//            if (body != null && body.getInteger("code").equals(0)) {
//                JSONArray jsonArray = body.getJSONArray("data");
//                for (Object o : jsonArray) {
//                    JSONObject jsonObject = JSONObject.parseObject(JSON.toJSONString(o));
//                    //是否为合约
//                    Boolean contractAddress = jsonObject.getBoolean("contractAddress");
//                    String transactionCount = jsonObject.getString("transactionCount");
//                    map.put("contractAddress", contractAddress ? "合约" : "不是合约");
//                    map.put("transactionCount", transactionCount);
//                }
//            } else {
//                log.error("getInformationEvm异常信息:{}", body);
//            }
//            return map;
//        } catch (Exception e) {
//            log.error("获取持有人前两百数据出现异常！！链名:{},地址:{}", chainName, address, e);
//        }
//        return null;
//    }
//
//    @Test
//    public void Test_4() {
//        // Excel 文件路径
//        String filePath = "C:\\Users\\bicoin\\Downloads\\virtual.xlsx";
//        String filePath2 = "C:\\Users\\bicoin\\Desktop\\前两百地址\\1.8_spx_sol_前100.xlsx";
////        String filePath2 = "C:\\Users\\bicoin\\Desktop\\data\\virtual_base前100地址.xlsx";
//        List<TokenTransaction> tokenTransactions = new ArrayList<>();
//
////        Map<String, String> holderAddress_v3 = getHolderAddress_v3(filePath2);
//
////        System.out.println(JSON.toJSONString(holderAddress_v3));
//
//
//        List<TokenMainInfo> tokenMainInfoByAddressList = tokenMainInfoMapper.getTokenMainInfoByAddressList();
//
//
//        Map<String, TokenMainInfo> groupedByTradeDate =
//                tokenMainInfoByAddressList.stream().collect(Collectors.toMap(TokenMainInfo::getContractId, dailyVo -> dailyVo,
//                        (existing, replacement) -> existing));
//
//
//        List<AddressInfoVo> addressInfoVoAllEth = new ArrayList<>();
//
//        List<AddressInfoVo> addressInfoVoAllBase = new ArrayList<>();
//
//        for (Map.Entry<String, String> stringStringEntry : MAP.entrySet()) {
//
//            String tokenContractAddress = stringStringEntry.getValue();
//
//            String chainName = stringStringEntry.getKey();
//
//            List<AddressInfoVo> addressInfoVoAll = getAddressInfoVoAll(tokenContractAddress, chainName);
//            if (chainName.equalsIgnoreCase("eth")) {
//                addressInfoVoAllEth.addAll(addressInfoVoAll);
//            } else {
//                addressInfoVoAllBase.addAll(addressInfoVoAll);
//            }
//
//        }
//
//        Map<String, AddressInfoVo> addressInfoVoAllEthMap =
//                addressInfoVoAllEth.stream().collect(Collectors.toMap(AddressInfoVo::getHolderAddress, dailyVo -> dailyVo,
//                        (existing, replacement) -> existing));
//
//        Map<String, AddressInfoVo> addressInfoVoAllBaseMap =
//                addressInfoVoAllBase.stream().collect(Collectors.toMap(AddressInfoVo::getHolderAddress, dailyVo -> dailyVo,
//                        (existing, replacement) -> existing));
//
//
//        for (Map.Entry<String, String> stringStringEntry : MAP.entrySet()) {
//
//            String tokenContractAddress = stringStringEntry.getValue();
//
//            String chainName = stringStringEntry.getKey();
//
//            TokenMainInfo tokenMainInfo = groupedByTradeDate.get(tokenContractAddress);
//
//            String[] split = tokenMainInfo.getAddressList().split(",");
//
//            List<SpxVo> spxVos = new ArrayList<>();
//            for (String address : split) {
//                String currentTotal;
//                AddressInfoVo addressInfoVo;
//                if (chainName.equalsIgnoreCase("eth")) {
//                    addressInfoVo = addressInfoVoAllEthMap.get(address);
//                } else {
//                    addressInfoVo = addressInfoVoAllBaseMap.get(address);
//                }
//                if (addressInfoVo == null) {
//                    currentTotal = "0";
//                } else {
//                    currentTotal = addressInfoVo.getAmount();
//                }
//                SpxVo tokenTransactionListNew2 = getTokenTransactionListNew2(chainName, address, tokenContractAddress,
//                        currentTotal);
//                if (tokenTransactionListNew2 != null) {
//                    spxVos.add(tokenTransactionListNew2);
//                } else {
//                    log.info("等于空:{}", address);
//                }
//            }
//            log.info("{}", JSON.toJSONString(spxVos));
//        }
//
//
////        Set<String> holderAddress = getHolderAddress_v2(filePath);
////        Map<String, String> entityLabel = getEntityLabel(holderAddress, "eth");
////
////        for (String address : holderAddress) {
////            TokenTransaction tokenTransactionList = getTokenTransactionList("base", address);
////            if (tokenTransactionList != null) {
////                log.info("地址:{},添加信息:{}", address, JSON.toJSONString(tokenTransactionList));
////                tokenTransactions.add(tokenTransactionList);
////            }
////        }
////        Map<String, TokenTransaction> collect =
////                tokenTransactions.stream().collect(Collectors.toMap(TokenTransaction::getAddress, v -> v,
////                        (existing, replacement) -> replacement));
////
////        xieRuData_v2(filePath, collect, holderAddress, entityLabel);
//    }
//
//
//    /**
//     * 获取到现有的地址信息
//     *
//     * @param filePath
//     * @return
//     */
//    public Set<String> getHolderAddress_v2(String filePath) {
//        Set<String> holderAddressSet = new HashSet<>();
//        try (FileInputStream fis = new FileInputStream(filePath);
//             Workbook workbook = new XSSFWorkbook(fis)) {
//            // 获取第一个工作表
//            Sheet sheet = workbook.getSheetAt(0);
//            // 从第二行开始遍历数据，填充新列
//            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
//                Row row = sheet.getRow(i);
//                if (row == null) continue;
//                // 获取 HolderAddress
//                Cell holderAddressCell = row.getCell(0);
//                if (holderAddressCell == null) continue;
//                String holderAddress = holderAddressCell.getStringCellValue();
//                holderAddressSet.add(holderAddress);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return holderAddressSet;
//    }
//
//    public Map<String, String> getHolderAddress_v3(String filePath) {
//        Map<String, String> setMap = new HashMap<>();
//        try (FileInputStream fis = new FileInputStream(filePath);
//             Workbook workbook = new XSSFWorkbook(fis)) {
//            // 获取第一个工作表
//            Sheet sheet = workbook.getSheetAt(0);
//            // 从第二行开始遍历数据，填充新列
//            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
//                Row row = sheet.getRow(i);
//                if (row == null) continue;
//                // 获取 HolderAddress
//                Cell holderAddressCell = row.getCell(0);
//                if (holderAddressCell == null) continue;
//                Cell labelInfo = row.getCell(1);
//                Cell addressAddCell = row.getCell(2);
//                String holderAddress = holderAddressCell.getStringCellValue();
//                String addressAdd = addressAddCell.getStringCellValue();
//                holderAddress = holderAddress + "," + addressAdd;
//
//                String label = labelInfo.getStringCellValue();
//                // 如果map中已经存在这个label，则拼接，否则直接插入
//                setMap.merge(label, holderAddress.trim(), (existingValue, newValue) -> existingValue + "," + newValue);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return setMap;
//    }
//
//
//    private SpxVo getTokenTransactionListNew2(String chainName, String address, String tokenContractAddress,
//                                              String currentTotal) {
//        BigDecimal totalAmountIn = BigDecimal.ZERO; // 累计转入金额
//        BigDecimal totalAmountOut = BigDecimal.ZERO; // 累计转出金额
//        boolean hasChange = false; // 是否有变动标记
//        long tenMonthsAgo = System.currentTimeMillis() - (10L * 30L * 24L * 60L * 60L * 1000L); // 获取当前时间10个月前的时间戳
//        try {
//            int currentPage = 1; // 初始页数
//            int pageSize = 100; // 每页条数
//            boolean shouldContinue = true; // 控制循环的标志
//            while (shouldContinue) {
//                Thread.sleep(1000L); // 避免请求过快
//                JSONObject body = okLinkApi.getTokenTransactionList(apikey, chainName, address, "token_20",
//                        tokenContractAddress, null,
//                        String.valueOf(currentPage),
//                        String.valueOf(pageSize)).execute().body();
//
//                if (body != null && body.getInteger("code").equals(0)) {
//                    JSONObject jsonObject = body.getJSONArray("data").getJSONObject(0);
//                    JSONArray tokenList = jsonObject.getJSONArray("transactionList");
//                    // 遍历当前页的交易记录
//                    for (Object o : tokenList) {
//                        JSONObject transaction = JSONObject.parseObject(JSON.toJSONString(o));
//                        String to = transaction.getString("to");
//                        BigDecimal amount = transaction.getBigDecimal("amount");
//                        Date transactionTime = transaction.getDate("transactionTime");
//
//                        // 过滤最近10个月的交易记录
//                        if (transactionTime != null && transactionTime.getTime() < tenMonthsAgo) {
//                            continue; // 跳过10个月之前的交易
//                        }
//
//                        // 统计转入转出金额
//                        if (!to.equalsIgnoreCase(address)) {
//                            // 转出：金额为负
//                            totalAmountOut = totalAmountOut.add(amount);
//                        } else {
//                            // 转入
//                            totalAmountIn = totalAmountIn.add(amount);
//                        }
//                    }
//                    // 判断是否继续
//                    if (tokenList.size() < pageSize) {
//                        // 当前返回条数小于 100，跳出循环
//                        shouldContinue = false;
//                    } else {
//                        currentPage++; // 获取下一页
//                    }
//                } else {
//                    log.warn("未知错误 address: {}, Page: {},{}", address, currentPage, body);
//                    shouldContinue = false; // 停止查询
//                }
//            }
//            // 判断是否有变动
//            BigDecimal totalAmount = totalAmountIn.add(totalAmountOut); // 将转入和转出金额相加
//            if (totalAmount.abs().compareTo(BigDecimal.valueOf(100000)) > 0) {
//                hasChange = true; // 累计金额超过10万，标记为有变动
//            }
//
//            return new SpxVo(address, totalAmountIn, totalAmountOut, totalAmount, hasChange ? "有变动" : "没有变动", currentTotal);
//
//        } catch (Exception e) {
//            log.error("循环出现异常！！ address: {}", address, e);
//        }
//
//        return null;
//    }
//
//
//    public void xieRuData_v2(String filePath, Map<String, TokenTransaction> tokenTransactionMap, Set<String> holderAddresSet,
//                             Map<String, String> entityLabel) {
//
//        try (FileInputStream fis = new FileInputStream(filePath);
//             Workbook workbook = new XSSFWorkbook(fis)) {
//            // 获取第一个工作表
//            Sheet sheet = workbook.getSheetAt(0);
//            // 获取表头行（假设是第一行）
//            Row headerRow = sheet.getRow(0);
//            int lastColumnIndex = headerRow.getLastCellNum();
//            // 在表头后新增两个表头：余额 和 是否合约
//            Cell balanceHeader = headerRow.createCell(lastColumnIndex + 1);
//            balanceHeader.setCellValue("最早入账时间");
//
//            Cell otherAmountHeader = headerRow.createCell(lastColumnIndex + 2);
//            otherAmountHeader.setCellValue("入账转账数量");
//
//            Cell contractHeader = headerRow.createCell(lastColumnIndex + 3);
//            contractHeader.setCellValue("转入地址");
//
//            Cell transactionCountHeader = headerRow.createCell(lastColumnIndex + 4);
//            transactionCountHeader.setCellValue("疑似存在关联地址");
//            // 从第二行开始遍历数据，填充新列
//            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
//                Row row = sheet.getRow(i);
//                if (row == null) continue;
//                // 获取 HolderAddress
//                Cell holderAddressCell = row.getCell(0);
//                if (holderAddressCell == null) continue;
//                String holderAddress = holderAddressCell.getStringCellValue();
//                TokenTransaction tokenTransaction = tokenTransactionMap.get(holderAddress);
//                String transactionTimeStr = "";
//                String suspectedAssociatedAddress = "";
//                String from = "";
//                String amount = "";
//                if (tokenTransaction != null) {
//                    from = tokenTransaction.getFrom();
//                    amount = tokenTransaction.getAmount().toString();
//                    Long transactionTime = tokenTransaction.getTransactionTime();
//                    transactionTimeStr = convertTimestampToDate(transactionTime);
//                    if (holderAddresSet.contains(from)) {
//                        suspectedAssociatedAddress = from;
//                    }
//                }
//                // 填充余额
//                row.createCell(lastColumnIndex + 1).setCellValue(transactionTimeStr);
//                row.createCell(lastColumnIndex + 2).setCellValue(amount);
//                row.createCell(lastColumnIndex + 3).setCellValue(from);
//                row.createCell(lastColumnIndex + 4).setCellValue(suspectedAssociatedAddress);
//            }
//            // 输出到新文件
//            try (FileOutputStream fos = new FileOutputStream(filePath)) {
//                workbook.write(fos);
//            }
//            System.out.println("Excel 文件处理完成！");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//}
