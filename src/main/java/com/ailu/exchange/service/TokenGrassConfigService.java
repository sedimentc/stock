package com.ailu.exchange.service;

import com.ailu.exchange.dao.domaim.TokenGrassConfig;
import com.ailu.exchange.dao.mapper.source1.TokenGrassConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@Slf4j
public class TokenGrassConfigService {

    @Resource
    TokenGrassConfigMapper tokenGrassConfigMapper;


    public List<TokenGrassConfig> getTokenGrassConfig() {
        return tokenGrassConfigMapper.getTokenGrassConfigList();
    }

    public void addTokenGrassConfig(List<TokenGrassConfig> tokenGrassConfigs) {
        tokenGrassConfigMapper.batchInsertOrUpdate(tokenGrassConfigs);
    }

}
