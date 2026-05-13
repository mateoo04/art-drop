package hr.tvz.artdrop.artdropapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    @GetMapping({
            "/",
            "/signup",
            "/login",
            "/challenges",
            "/challenges/{id}",
            "/collections",
            "/search",
            "/details/{id}",
            "/edit/{id}",
            "/drop",
            "/account",
            "/checkout/{artworkId}",
            "/orders",
            "/orders/{id}",
            "/sales",
            "/u/{slug}",
            "/admin",
            "/admin/users",
            "/admin/challenges",
            "/admin/challenges/new",
            "/admin/challenges/{id}/edit"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
