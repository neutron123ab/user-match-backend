package com.neutron.usermatchbackend.service.impl;

import java.util.*;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neutron.usermatchbackend.common.ErrorCode;
import com.neutron.usermatchbackend.exception.BusinessException;
import com.neutron.usermatchbackend.model.dto.TmpSecret;
import com.neutron.usermatchbackend.model.dto.UserDTO;
import com.neutron.usermatchbackend.model.entity.User;
import com.neutron.usermatchbackend.model.request.UserLoginRequest;
import com.neutron.usermatchbackend.model.request.UserRegisterRequest;
import com.neutron.usermatchbackend.service.UserService;
import com.neutron.usermatchbackend.mapper.UserMapper;
import com.neutron.usermatchbackend.util.AlgorithmUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicSessionCredentials;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.region.Region;
import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.neutron.usermatchbackend.constant.UserConstant.*;

/**
 * @author zzs
 * @description 针对表【user(用户表)】的数据库操作Service实现
 * @createDate 2023-03-21 15:12:31
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @Value("${cos.regionName}")
    private String regionName;

    @Value("${cos.bucketName}")
    private String bucketName;

    @Value("${cos.filePrefix}")
    private String filePrefix;

    @Resource
    private UserMapper userMapper;

    @Resource
    private TmpSecret tmpSecret;
    private static final String SALT = "neutron";

    private static final String VALID_PATTERN = "^[a-zA-Z0-9]+$";

    @Override
    public long userRegister(UserRegisterRequest userRegisterRequest) {

        //1.接收到的参数是否为空
        if (BeanUtil.hasNullField(userRegisterRequest)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        //2.账号长度 >= 4
        String userAccount = userRegisterRequest.getUserAccount();
        if (userAccount.length() < USER_ACCOUNT_MIN_LEN) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        //3.密码/校验密码长度 >= 8
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        if (userPassword.length() < USER_PASSWORD_MIN_LEN
                || checkPassword.length() < USER_PASSWORD_MIN_LEN) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        //6.账号不能包含特殊字符
        Pattern pattern = Pattern.compile(VALID_PATTERN);
        Matcher matcher = pattern.matcher(userRegisterRequest.getUserAccount());
        if (!matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号不能包含特殊字符");
        }
        //7.判断密码与校验密码是否相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        //8.查数据库，账号不能重复
        Long count = query().eq("user_account", userAccount).count();
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该账户已存在");
        }
        //10.密码加密
        String encodedPassword = SecureUtil.md5(userPassword + SALT);
        //11.插入数据库
        User user = new User();
        user.setUsername("user_" + IdUtil.simpleUUID());
        user.setUserAccount(userAccount);
        user.setUserPassword(encodedPassword);
        boolean save = save(user);
        if (!save) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "插入数据库失败");
        }
        //12.返回新用户id

        return user.getId();
    }

    @Override
    public UserDTO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request) {

        //1.非空
        if (BeanUtil.hasNullField(userLoginRequest)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }
        //2.账号长度不小于4位
        String userAccount = userLoginRequest.getUserAccount();
        if (userAccount.length() < USER_ACCOUNT_MIN_LEN) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        //3.密码长度不小于8位
        String userPassword = userLoginRequest.getUserPassword();
        if (userPassword.length() < USER_PASSWORD_MIN_LEN) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码过短");
        }
        //4.账号不包含特殊字符
        Pattern pattern = Pattern.compile(VALID_PATTERN);
        Matcher matcher = pattern.matcher(userAccount);
        if (!matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能包含特殊字符");
        }
        //5.密码比对
        String encodedPassword = SecureUtil.md5(userPassword + SALT);
        User user = query().eq("user_password", encodedPassword)
                .eq("user_account", userAccount)
                .one();
        if (BeanUtil.isEmpty(user)) {
            log.info("user login failed, userAccount cannot match password");
            throw new BusinessException(ErrorCode.NULL_ERROR, "找不到该用户");
        }
        //6.用户信息脱敏
        UserDTO userDTO = new UserDTO();
        BeanUtil.copyProperties(user, userDTO);
        //7.记录用户登录态（session）
        request.getSession().setAttribute(USER_LOGIN_STATE, userDTO);
        //8.返回脱敏后的用户信息
        return userDTO;
    }

    @Override
    public UserDTO getSafetyUser(User user) {
        UserDTO userDTO = new UserDTO();
        BeanUtil.copyProperties(user, userDTO);
        return userDTO;
    }

    @Override
    public User getSafetyUsers(User user) {
        if (user == null) {
            return null;
        }
        User safetyUser = new User();
        safetyUser.setId(user.getId());
        safetyUser.setUsername(user.getUsername());
        safetyUser.setUserAccount(user.getUserAccount());
        safetyUser.setAvatarUrl(user.getAvatarUrl());
        safetyUser.setGender(user.getGender());
        safetyUser.setPhone(user.getPhone());
        safetyUser.setEmail(user.getEmail());
        safetyUser.setUserStatus(user.getUserStatus());
        safetyUser.setCreateTime(user.getCreateTime());
        safetyUser.setTags(user.getTags());
        safetyUser.setUserRole(user.getUserRole());

        return safetyUser;
    }


    @Override
    public boolean isAdmin(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        UserDTO userInfo = (UserDTO) userObj;
        return userInfo != null && userInfo.getUserRole() == ADMIN_ROLE;
    }

//    @Override
//    public List<UserDTO> searchUsersByTags(List<String> tags) {
//
//        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
//        for(String tag : tags) {
//            queryWrapper.like("tags", tag);
//        }
//        List<User> userList = list(queryWrapper);
//
//        return userList.stream().map(this::getSafetyUser).collect(Collectors.toList());
//    }

    @Override
    public List<UserDTO> searchUsersByTags(List<String> tags) {

        if (CollUtil.isEmpty(tags)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        List<User> userList = list(queryWrapper);
        return userList.stream().filter(user -> {
            Set<String> userTags = user.getTags();
            userTags = Optional.ofNullable(userTags).orElse(new HashSet<>());
            for (String tag : tags) {
                if (!userTags.contains(tag)) {
                    return false;
                }
            }
            return true;
        }).map(this::getSafetyUser).collect(Collectors.toList());
    }

    @Override
    public boolean updateUser(User user, UserDTO loginUser) {
        Long id = user.getId();
        if (loginUser == null || !id.equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "用户未登录");
        }
        User newUser = userMapper.selectById(id);
        if (newUser == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "请求参数为空");
        }
        return updateById(user);
    }

    @Override
    public String uploadAvatar(MultipartFile file) {
        // 1 传入获取到的临时密钥 (tmpSecretId, tmpSecretKey, sessionToken)
        String tmpSecretId = tmpSecret.getSecretId();
        String tmpSecretKey = tmpSecret.getSecretKey();
        String sessionToken = tmpSecret.getSessionToken();
        BasicSessionCredentials cred = new BasicSessionCredentials(tmpSecretId, tmpSecretKey, sessionToken);

        Region region = new Region(regionName);
        ClientConfig clientConfig = new ClientConfig(region);

        COSClient cosClient = new COSClient(cred, clientConfig);

        // 指定要上传的文件
// 指定文件将要存放的存储桶
// 指定文件上传到 COS 上的路径，即对象键。例如对象键为 folder/picture.jpg，则表示将文件 picture.jpg 上传到 folder 路径下
        String key = "user-match/" + IdUtil.simpleUUID() + ".jpg";

        PutObjectRequest putObjectRequest;
        try {
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setSecurityToken(sessionToken);
            putObjectRequest = new PutObjectRequest(bucketName, key, file.getInputStream(), objectMetadata);
            cosClient.putObject(putObjectRequest);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            cosClient.shutdown();
        }

        return filePrefix + key;
    }

    @Override
    public List<UserDTO> matchUsers(UserDTO user, long pageSize, long currentPage) {
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        if (currentPage < 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "查询页不能小于1");
        }
        if(pageSize < 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "页大小不能小于1");
        }
        Set<String> userTagsSet = user.getTags();
        List<String> userTags1 = new ArrayList<>(userTagsSet);
        userTags1 = userTags1.stream().sorted().collect(Collectors.toList());
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "tags");
        List<User> userList = list(queryWrapper);
        //pair中保存Pair<用户id, 相似度>，相似度越小越匹配
        List<Pair<Long, Integer>> list = new ArrayList<>();
        for (User value : userList) {
            List<String> userTags2 = new ArrayList<>(value.getTags());
            if(value.getId().equals(user.getId()) || CollUtil.isEmpty(userTags2)) {
                continue;
            }
            int minDistance = AlgorithmUtil.minDistance(userTags1, userTags2);
            list.add(new Pair<>(value.getId(), minDistance));
        }
        //此处得到的id顺序即要返回的user顺序
        List<Long> userIdList = list.stream()
                .sorted(Comparator.comparingInt(Pair::getValue))
                .skip(pageSize * (currentPage-1))
                .limit(pageSize)
                .map(Pair::getKey)
                .collect(Collectors.toList());
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.in("id", userIdList);
        //此处从数据库中查出了id对应的user，但把顺序打乱了
        Map<Long, List<UserDTO>> userMap = list(userQueryWrapper)
                .stream()
                .map(this::getSafetyUser)
                .collect(Collectors.groupingBy(UserDTO::getId));
        List<UserDTO> resultList = new ArrayList<>();
        //根据上面的userIdList重排序
        for (Long id : userIdList) {
            resultList.add(userMap.get(id).get(0));
        }

        return resultList;
    }


}




