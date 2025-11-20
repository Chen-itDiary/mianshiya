package com.xiaochen.mianshiya.service.impl;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaochen.mianshiya.DeviceUtils.DeviceUtils;
import com.xiaochen.mianshiya.common.ErrorCode;
import com.xiaochen.mianshiya.constant.CommonConstant;
import com.xiaochen.mianshiya.constant.RedisConstant;
import com.xiaochen.mianshiya.exception.BusinessException;
import com.xiaochen.mianshiya.mapper.UserMapper;
import com.xiaochen.mianshiya.model.dto.user.UserQueryRequest;
import com.xiaochen.mianshiya.model.entity.User;
import com.xiaochen.mianshiya.model.enums.UserRoleEnum;
import com.xiaochen.mianshiya.model.vo.LoginUserVO;
import com.xiaochen.mianshiya.model.vo.UserVO;
import com.xiaochen.mianshiya.satoken.TellKicked;
import com.xiaochen.mianshiya.service.UserService;
import com.xiaochen.mianshiya.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.bean.WxOAuth2UserInfo;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBitSet;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

import static com.xiaochen.mianshiya.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户服务实现
 *
 * @author <a href="https://github.com/lixiaochen">程序员鱼皮</a>
 * @from <a href="https://xiaochen.icu">编程导航知识星球</a>
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private TellKicked tellKicked;
    /**
     * 盐值，混淆密码
     */
    public static final String SALT = "xiaochen";

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        // 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        synchronized (userAccount.intern()) {
            // 账户不能重复
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userAccount", userAccount);
            long count = this.baseMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
            }
            // 2. 加密
            String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
            // 3. 插入数据
            User user = new User();
            user.setUserAccount(userAccount);
            user.setUserPassword(encryptPassword);
            boolean saveResult = this.save(user);
            if (!saveResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
            }
            return user.getId();
        }
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 3. 记录用户的登录态
//        request.getSession().setAttribute(USER_LOGIN_STATE, user);

        // sa-token 记录登录态， 指定登陆的设备，完成同端登录互斥
        StpUtil.login(user.getId(), DeviceUtils.getRequestDevice(request));
        StpUtil.getSession().set(USER_LOGIN_STATE, user);
        tellKicked.isConflicted();
        return this.getLoginUserVO(user);
    }

    @Override
    public LoginUserVO userLoginByMpOpen(WxOAuth2UserInfo wxOAuth2UserInfo, HttpServletRequest request) {
        String unionId = wxOAuth2UserInfo.getUnionId();
        String mpOpenId = wxOAuth2UserInfo.getOpenid();
        // 单机锁
        synchronized (unionId.intern()) {
            // 查询用户是否已存在
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("unionId", unionId);
            User user = this.getOne(queryWrapper);
            // 被封号，禁止登录
            if (user != null && UserRoleEnum.BAN.getValue().equals(user.getUserRole())) {
                throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "该用户已被封，禁止登录");
            }
            // 用户不存在则创建
            if (user == null) {
                user = new User();
                user.setUnionId(unionId);
                user.setMpOpenId(mpOpenId);
                user.setUserAvatar(wxOAuth2UserInfo.getHeadImgUrl());
                user.setUserName(wxOAuth2UserInfo.getNickname());
                boolean result = this.save(user);
                if (!result) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "登录失败");
                }
            }
            // 记录用户的登录态
            request.getSession().setAttribute(USER_LOGIN_STATE, user);
            return getLoginUserVO(user);
        }
    }

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 如果已经登录
        Object loginUserId = StpUtil.getLoginIdDefaultNull();
        Object o = StpUtil.getSession().get(USER_LOGIN_STATE);
        if (loginUserId == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "非法请求");
        }
        // 先判断是否已登录
//        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
//        User currentUser = (User) userObj;
//        if (currentUser == null || currentUser.getId() == null) {
//            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
//        }
//        // 从数据库查询（追求性能的话可以注释，直接走;缓存）
//        long userId = currentUser.getId();
        User currentUser = this.getById((String) loginUserId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    /**
     * 获取当前登录用户（允许未登录）
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUserPermitNull(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            return null;
        }
        // 从数据库查询（追求性能的话可以注释，直接走缓存）
        long userId = currentUser.getId();
        return this.getById(userId);
    }

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        // 仅管理员可查询
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        return isAdmin(user);
    }

    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }

    /**
     * 用户注销
     *
     * @param request
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 判断是否已登录
        StpUtil.checkLogin();
        StpUtil.logout();
//        if (request.getSession().getAttribute(USER_LOGIN_STATE) == null) {
//            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
//        }
//        // 移除登录态
//        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVO(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String unionId = userQueryRequest.getUnionId();
        String mpOpenId = userQueryRequest.getMpOpenId();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(StringUtils.isNotBlank(unionId), "unionId", unionId);
        queryWrapper.eq(StringUtils.isNotBlank(mpOpenId), "mpOpenId", mpOpenId);
        queryWrapper.eq(StringUtils.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StringUtils.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.like(StringUtils.isNotBlank(userName), "userName", userName);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    /**
     * 添加用户签到记录
     *
     * @param userId
     * @return 用户是否签到成功
     */
    @Override
    public boolean addUserSignIn(long userId) {
        LocalDate date = LocalDate.now();
        int year = date.getYear();
        String userSignInRedisKey = RedisConstant.getUserSignInRedisKey(year, userId);
        // 获取到BitMap
        RBitSet bitSet = redissonClient.getBitSet(userSignInRedisKey);
        // 查询当天日期在当年的第几天(偏移量)
        int offset = date.getDayOfYear();
        // 查询当天是否签到
        if (!bitSet.get(offset)) {
            bitSet.set(offset, true);
        }
        // 签到成功
        return true;
    }

    /**
     * 获取用户某年的签到记录
     *
     * @param userId
     * @param year
     * @return 某用户一年的签到记录
     */
    @Override
    public List<Integer> getUserSignInRecord(long userId, Integer year) {
        if (year == null) {
            LocalDate date = LocalDate.now();
            year = date.getYear();
        }

        // 获取Key
        String userSignInRedisKey = RedisConstant.getUserSignInRedisKey(year, userId);
        // 获取签到记录
        RBitSet bitSet = redissonClient.getBitSet(userSignInRedisKey);
        // 加载 BitSet 到内存，避免后续读取签到记录时发送多次请求
        BitSet asBitset = bitSet.asBitSet();
        // 从索引 0 开始查找下一个被设置为 1 的位
        int index = asBitset.nextSetBit(0);
        // 构造Map存放签到记录
        List<Integer> map = new ArrayList<>();
        while (index != -1) {
            map.add(index);
            index = asBitset.nextSetBit(index + 1);
        }
        //获取当年总天数
        int totalDays = Year.of(year).length();
        return map;
    }
}
