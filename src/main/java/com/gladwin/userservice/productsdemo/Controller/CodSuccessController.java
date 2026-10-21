package com.gladwin.userservice.productsdemo.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CodSuccessController {

    @GetMapping("/cod-success")
    public String codSuccess() {
        return "cod-success";
    }
}