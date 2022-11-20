package xyz.jjcl.signup.api;

import org.springframework.web.bind.annotation.*;

@RequestMapping("signup")
@RestController
public class SignUpController {

    @GetMapping("test/{text}")
    public String test(@PathVariable String text) {
        return text;
    }
}
