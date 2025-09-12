package com.ailu.exchange.controller;


import com.ailu.exchange.dao.mapper.source1.AddressTxMonitorMapper;
import com.ailu.exchange.dao.vo.AddressTxMonitorVo;
import com.ailu.exchange.utils.web.WebResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/txMonitor")
@CrossOrigin(origins = "*", maxAge = 3600)
@Slf4j
public class AddressTxMonitorController {

    @Resource
    AddressTxMonitorMapper addressTxMonitorMapper;

    @GetMapping("/getAddressTxMonitorInfo")
    public Object getAddressTxMonitorInfo() {
        try {
            List<AddressTxMonitorVo> dateTotalAmount = addressTxMonitorMapper.getDateTotalAmountWithPagination(50, 1);
            return WebResult.okResult(dateTotalAmount);
        } catch (Exception e) {
            log.error("请求修改getAddressTxMonitorInfo出现异常！！", e);
            return WebResult.failResult("error");
        }
    }

    @GetMapping("/getAddressTxMonitorInfoNew")
    public Object getAddressTxMonitorInfoNew(@RequestParam(value = "page", defaultValue = "1") int page, @RequestParam(value =
            "size", defaultValue = "30") int size) {
        try {
            int offset = (page - 1) * size;
            List<AddressTxMonitorVo> dateTotalAmount = addressTxMonitorMapper.getDateTotalAmountWithPagination(size, offset);
            long totalCount = addressTxMonitorMapper.getTotalCount();
            Long totalPages = (totalCount + size - 1) / size;  // 计算总页数
            Map<String, Object> result = new HashMap<>();
            result.put("list", dateTotalAmount);  // 分页数据
            result.put("total",totalCount);      // 总记录数
            result.put("totalPages",totalPages);      // 总记录数
            result.put("page", page);             // 当前页
            result.put("size", size);             // 每页显示数量
            return WebResult.okResult(result);
        } catch (Exception e) {
            log.error("请求修改getAddressTxMonitorInfo出现异常！！", e);
            return WebResult.failResult("error");
        }
    }
}
