package io.openvidu.server.utils;

import io.openvidu.server.common.pojo.SimpleUserInfo;
import io.openvidu.server.common.pojo.User;

import java.util.List;
import java.util.stream.Collectors;

public class SimpleUserInfoHelper {


    public static SimpleUserInfo coverFromUser(User user) {
        SimpleUserInfo info = new SimpleUserInfo();
        info.setPhone(user.getPhone());
        info.setUserIcon(user.getIcon());
        info.setUuid(user.getUuid());
        info.setUserName(user.getUsername());
        return info;
    }

    public static List<SimpleUserInfo> coverFromUser(List<User> users) {
        return users.stream().map(SimpleUserInfoHelper::coverFromUser).collect(Collectors.toList());
    }
}
