package io.openvidu.server.common.enums;

import org.apache.commons.lang3.StringUtils;

public enum RoomIdTypeEnums {

    unknown,
    personal,
    random,
    fixed;


    public static RoomIdTypeEnums calculationRoomType(String roomId) {
        if (StringUtils.isBlank(roomId)) {
            return unknown;
        }

        switch (roomId.length()) {
            case 8:
                return RoomIdTypeEnums.fixed;
            case 9:
                return RoomIdTypeEnums.random;
            case 11:
                return personal;
            default:
                return unknown;
        }
    }

    public static boolean isPersonRoom(String roomId) {
        return calculationRoomType(roomId) == personal;
    }

    public static boolean isRandomRoom(String roomId) {
        return calculationRoomType(roomId) == random;
    }

    public static boolean isFixed(String roomId) {
        return calculationRoomType(roomId) == fixed;
    }

    public static RoomIdTypeEnums parse(String roomIdType) {
        for (RoomIdTypeEnums value : RoomIdTypeEnums.values()) {
            if (value.name().equals(roomIdType)) {
                return value;
            }
        }
        throw new IllegalArgumentException("roomIdType error " + roomIdType);
    }
}
