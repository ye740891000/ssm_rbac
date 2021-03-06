package com.etop.controller;

import com.etop.pojo.Msg;
import com.etop.pojo.Role;
import com.etop.pojo.User;
import com.etop.service.IPermissionService;
import com.etop.service.IRoleService;
import com.etop.service.IUserService;
import com.etop.service.IloginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Created by 63574 on 2017/7/13.
 */
@Controller
public class LoginController {

    @Autowired
    IPermissionService permissionService;


    @Autowired
    IloginService loginServiceImpl;

    @Autowired
    IUserService userService;

    @Autowired
    IRoleService roleService;

    @Autowired
    PermissionController permissionController;

    @RequestMapping("/returnWrong")
    public ModelAndView returnWrong(String msg) {
        ModelAndView mav = new ModelAndView("400");
        mav.addObject("msg", msg);
        return mav;
    }

    @ResponseBody
    @RequestMapping("/checkUser")
    public Msg checkUser(@RequestParam(value = "username") String username,
                         @RequestParam(value = "password") String password) {
        User user = new User();
        user.setAccount(username);
        user.setPassword(password);
        User temp = loginServiceImpl.login(user);
        if (temp == null) {
            return Msg.fail();
        } else {
            return Msg.success();
        }
    }

    @RequestMapping("/login")
    public ModelAndView handleEnter() {
        ModelAndView mav = new ModelAndView("index");
        return mav;
    }

    @RequestMapping("/notpermission")
    public ModelAndView handleNotpermission() {
        ModelAndView mav = new ModelAndView("notpermission");
        return mav;
    }


    @RequestMapping("/user/index")
    public ModelAndView handleLogin(User user, HttpServletRequest httpServletRequest) {
        Pattern reg = Pattern.compile("^[a-z0-9_-]{3,16}$");
        Matcher accMat = reg.matcher(user.getAccount());
        boolean accMatRe = accMat.matches();
        Matcher passMat = reg.matcher(user.getPassword());
        boolean passMatRe = passMat.matches();
        if(accMatRe==false||passMatRe==false){
            ModelAndView mav = new ModelAndView("400");
            mav.addObject("msg", "帐号密码必须是3-16位英文和数字组合");
            return mav;
        }
//        if (result.hasErrors()) {
//            List<FieldError> fieldErrors = result.getFieldErrors();
//            StringBuilder stringBuilder = new StringBuilder();
//            for (FieldError fieldError1 : fieldErrors) {
//                stringBuilder.append(fieldError1.getDefaultMessage()+"\n");
//            }
//
//        }
        HttpSession session =
                httpServletRequest.getSession();
        Long id = (Long) session.getAttribute("id");
        User temp = loginServiceImpl.login(user);
        if (user.getAccount() == null || user.getPassword() == null) {
            //从别的界面来
            if (id == null) {
                //没有登陆过
                ModelAndView mav = new ModelAndView("index");
                return mav;
            } else {
                //登录过加载原来的数据
                ModelAndView mav = new ModelAndView("personalHomepage");
                temp = userService.selectByPrimaryKey(id);
                mav.addObject("user", temp);

                return mav;
            }
        } else {
            //从登录界面来
            if (temp == null) {
                //帐号不存在
                ModelAndView mav = new ModelAndView("400");
                mav.addObject("msg","帐号不存在！");
                return mav;
            } else {
                //更新权限
                session.removeAttribute("id");
                session.removeAttribute("userPermission");
                List<User> users = userService.listPermission(temp.getId());
                HashSet<String> userPerssion = new HashSet<String>();
                if (users != null)
                    for (User u : users) {
                        if (u != null && u.getRoleList() != null) {
                            List<Role> roles = roleService.listPermission(u.getRoleList().get(0).getId());
                            if (roles != null) {
                                for (Role role : roles) {
                                    if (role != null) {
                                        String expression = role.getPermissionList().get(0).getExpression();
                                        if (!userPerssion.contains(expression))
                                            userPerssion.add(expression);
                                    }
                                }

                            }
                        }
                    }
                session.setAttribute("id", temp.getId());
                session.setAttribute("userPermission", userPerssion);
                ModelAndView mav = new ModelAndView("personalHomepage");
                mav.addObject("user", temp);
                mav.addObject("userPermission", userPerssion);
                return mav;
            }
        }


    }


}
