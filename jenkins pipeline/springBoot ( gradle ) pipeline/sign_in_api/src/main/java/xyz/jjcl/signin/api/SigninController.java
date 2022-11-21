package xyz.jjcl.signin.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("signin")
@RestController
public class SigninController {

    @GetMapping("test/{text}")
    public String test(@PathVariable String text) {
        return text;
    }
}
