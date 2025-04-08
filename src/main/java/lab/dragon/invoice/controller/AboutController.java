package lab.dragon.invoice.controller;

import org.springframework.web.bind.annotation.*;

/**
 * @author mickey.wang
 */
@RestController
@RequestMapping("about")
public class AboutController {

    @GetMapping("version")
    public String version() {
        return "1.0.1.rc2-2025-04-08";
    }
}
