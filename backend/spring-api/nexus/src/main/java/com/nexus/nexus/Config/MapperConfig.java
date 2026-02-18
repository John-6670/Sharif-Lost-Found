package com.nexus.nexus.Config;

import com.nexus.nexus.Mapper.ProductMapper;
import org.mapstruct.factory.Mappers;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MapperConfig {

    @Bean
    @ConditionalOnMissingBean(ProductMapper.class)
    public ProductMapper productMapper() {
        return Mappers.getMapper(ProductMapper.class);
    }
}
