package com.project.Blog_Management_System.Config;

import com.project.Blog_Management_System.Utils.AllureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class AllureMockMvcConfig {

    @Bean
    public MockMvcBuilderCustomizer allureMockMvcBuilderCustomizer() {
        return builder -> builder.alwaysDo(AllureMockMvc.attach());
    }
}