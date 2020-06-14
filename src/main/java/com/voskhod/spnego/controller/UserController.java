package com.voskhod.spnego.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;


@Controller
public class UserController {

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String login(Model model, String error, String logout) {
        if (error != null) {
            model.addAttribute("error", "Не совпадает имя пользователя или пароль.");
        }
        if (logout != null) {
            model.addAttribute("message", "Выход осуществлен успешно.");
        }
        model.addAttribute("path", System.getProperty("catalina.base"));
        return "login";
    }

    @RequestMapping(value = {"/welcome"}, method = RequestMethod.POST)
    public String welcome() {
        return "welcome";
    }

    @RequestMapping(value = "/error", method = RequestMethod.GET)
    public String error() {
        return "error";
    }

}