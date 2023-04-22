package com.example.yupao_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;

/**
 * 自定义 swagger 接口文档的配置
 */
@Configuration
@EnableSwagger2WebMvc
//@Profile("prod")
public class SwaggerConfig {

    @Bean(value = "defaultApi2")
    public Docket productApi() {
        return new Docket(DocumentationType.SWAGGER_2)  // swagger 版本选择
                .apiInfo(apiInfo())
                .select() // 要选择 api 了，要找到我们接口代码的位置
                .apis(RequestHandlerSelectors.basePackage("com.example.yupao_backend.controller"))
                .paths(PathSelectors.any()) // 可以筛选那些文件的接口可以生成，支持正则表达式
                .build();
         
    }

    /**
     * api 信息
     * @return
     */
    private ApiInfo apiInfo() {
        // 构造器模式，可以在方法后不断增加配置，通过一个链式调用
        return new ApiInfoBuilder()
                .title("鱼皮用户中心")
                .description("接口文档")
                .version("1.0")
                .build();  // 最后 build 构造出 apiinfo 对象
    }
}