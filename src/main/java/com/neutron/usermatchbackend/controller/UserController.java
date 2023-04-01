package com.neutron.usermatchbackend.controller;
import java.util.Date;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.neutron.usermatchbackend.common.BaseResponse;
import com.neutron.usermatchbackend.common.ErrorCode;
import com.neutron.usermatchbackend.common.ResultUtils;
import com.neutron.usermatchbackend.exception.BusinessException;
import com.neutron.usermatchbackend.model.dto.UserDTO;
import com.neutron.usermatchbackend.model.entity.Tags;
import com.neutron.usermatchbackend.model.entity.User;
import com.neutron.usermatchbackend.model.request.UserLoginRequest;
import com.neutron.usermatchbackend.model.request.UserRegisterRequest;
import com.neutron.usermatchbackend.service.TagsService;
import com.neutron.usermatchbackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.neutron.usermatchbackend.constant.TagsConstant.USER_MATCH_TAGS;
import static com.neutron.usermatchbackend.constant.UserConstant.USER_LOGIN_STATE;

/**
 * @author zzs
 * @date 2023/3/21 15:58
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private TagsService tagsService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @PostMapping("/register")
    public BaseResponse<Long> userRegisterController(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean flag = BeanUtil.hasNullField(userRegisterRequest);
        if (flag) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }
        long userId = userService.userRegister(userRegisterRequest);

        return ResultUtils.success(userId, "注册成功");
    }

    @PostMapping("/login")
    public BaseResponse<UserDTO> userLoginController(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean flag = BeanUtil.hasNullField(userLoginRequest);
        if (flag) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }
        UserDTO userDTO = userService.userLogin(userLoginRequest, request);
        return ResultUtils.success(userDTO, "登录成功");
    }

    @GetMapping("/getUserInfo")
    public BaseResponse<UserDTO> getUserInfoController(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        UserDTO userInfo = (UserDTO) userObj;
        if (userInfo == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        return ResultUtils.success(userInfo);
    }

    @GetMapping("/search")
    public BaseResponse<List<UserDTO>> searchUsers(String username, HttpServletRequest request) {
        if(!userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if(CharSequenceUtil.isNotBlank(username)) {
            queryWrapper.like("username", username);
        }
        List<User> list = userService.list(queryWrapper);
        List<UserDTO> userList = list.stream().map(user -> userService.getSafetyUser(user)).collect(Collectors.toList());

        return ResultUtils.success(userList);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody long id, HttpServletRequest request) {
        if(!userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        if(id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean flag = userService.removeById(id);
        return ResultUtils.success(flag);
    }

    @GetMapping("/search/tags")
    public BaseResponse<List<UserDTO>> searchUsersByTags(@RequestParam(required = false) List<String> tags){
        System.out.println(tags);
        if(CollUtil.isEmpty(tags)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数列表不能为空");
        }
        long start = System.currentTimeMillis();
        List<UserDTO> userList = userService.searchUsersByTags(tags);
        long end = System.currentTimeMillis();
        log.info("time = " + (end - start));
        return ResultUtils.success(userList);
    }

    @PostMapping("/update")
    public BaseResponse<Boolean> updateUser(@RequestBody User user, HttpServletRequest request) {
        UserDTO loginUser = (UserDTO) request.getSession().getAttribute(USER_LOGIN_STATE);
        if(user == null) {
           throw new BusinessException(ErrorCode.NULL_ERROR, "请求参数为空");
        }
        boolean result = userService.updateUser(user, loginUser);
        Set<String> userTags = user.getTags();
        for (String userTag : userTags) {
            Long add = stringRedisTemplate.opsForSet().add(USER_MATCH_TAGS, userTag);
            stringRedisTemplate.expire(USER_MATCH_TAGS, 1, TimeUnit.HOURS);
            //如果当前标签已存在于redis
            if(add != null && add == 0) {
                continue;
            }
            //如果当前标签不存在于redis中，则将其插入到数据库
            Tags tags = new Tags();
            tags.setTagName(userTag);
            boolean save = tagsService.save(tags);
            if(!save) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "标签插入失败");
            }
        }

        return ResultUtils.success(result);
    }

    @PostMapping("/upload/{id}")
    public BaseResponse<String> uploadFile(@RequestParam MultipartFile file, @PathVariable Integer id) {
        if(file == null) {
            return ResultUtils.error(ErrorCode.NULL_ERROR, "file为空");
        }
        String picUrl = userService.uploadAvatar(file);
        boolean isUpdate = userService.update().eq("id", id).set("avatar_url", picUrl).update();
        if(isUpdate) {
            return ResultUtils.success(picUrl);
        }
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "图片上传失败");
    }

    @GetMapping("/getAllUsers")
    public BaseResponse<Page<User>> getAllUsers(long pageSize, long currentPage, HttpServletRequest request) {
        UserDTO user = (UserDTO) request.getSession().getAttribute(USER_LOGIN_STATE);
        String redisKey = String.format("user:match:getAllUsers:%s:%s", user.getId(), currentPage);
        String pageInfoJson = stringRedisTemplate.opsForValue().get(redisKey);
        Gson gson = new Gson();
        //如果查到了缓存
        if(!StrUtil.isEmptyIfStr(pageInfoJson)) {
            Page<User> pageInfo = gson.fromJson(pageInfoJson, new TypeToken<Page<User>>(){}.getType());
            return ResultUtils.success(pageInfo);
        }
        //缓存中没有则去查数据库
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne("id", user.getId());
        Page<User> page = userService.page(new Page<>(currentPage, pageSize), queryWrapper);
        List<User> userList = page.getRecords().stream().map(userService::getSafetyUsers).collect(Collectors.toList());
        Page<User> userPage = page.setRecords(userList);

        stringRedisTemplate.opsForValue().set(redisKey, gson.toJson(userPage), 30, TimeUnit.MINUTES);
        return ResultUtils.success(userPage);
    }

    @GetMapping("/matchUser")
    public BaseResponse<List<UserDTO>> matchUserByTags(long pageSize, long currentPage, HttpServletRequest request){
        UserDTO user = (UserDTO) request.getSession().getAttribute(USER_LOGIN_STATE);
        List<UserDTO> userPage = userService.matchUsers(user, pageSize, currentPage);
        return ResultUtils.success(userPage);
    }
}






















