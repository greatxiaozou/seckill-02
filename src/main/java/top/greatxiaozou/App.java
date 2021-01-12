package top.greatxiaozou;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;
import top.greatxiaozou.dao.UserDoMapper;
import top.greatxiaozou.dataobject.UserDo;

/**
 * Hello world!
 *
 */
@SpringBootApplication(scanBasePackages = {"top.greatxiaozou"})
@MapperScan("top.greatxiaozou.dao")
@RestController
public class App {

    public static void main( String[] args ){
        System.out.println( "Hello World!" );
        SpringApplication.run(App.class,args);
    }
}
