package io.openvidu.server.common.layout;


import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.openvidu.server.common.enums.LayoutModeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@Component
public class LayoutInitHandler {

    private static final int MAX_WIDTH = 1920;
    private static final int MAX_HEIGHT = 1080;

    @Value("${conference.layout}")
    private String conferenceLayouts;

    private static ConcurrentHashMap<LayoutModeEnum, JsonArray> layoutMap = new ConcurrentHashMap<>(6);

    @PostConstruct
    public void init() {
        JsonArray layoutArray = new Gson().fromJson(conferenceLayouts, JsonArray.class);
        int mode, horizontalDivCount, verticalDivCount, space, widthDiv, heightDiv, width, height;
        for (JsonElement element : layoutArray) {
            JsonObject item = element.getAsJsonObject();
            mode = item.get("mode").getAsInt();
            LayoutModeEnum layoutMode = LayoutModeEnum.getLayoutMode(mode);
            horizontalDivCount = item.get("horizontalDivCount").getAsInt();
            verticalDivCount = item.get("verticalDivCount").getAsInt();
            space = item.get("space").getAsInt();

            widthDiv = MAX_WIDTH / (horizontalDivCount + 1);
            heightDiv = MAX_HEIGHT / (verticalDivCount + 1);
            width = widthDiv - space * 2;
            height = heightDiv - space * 2;

            int x, y;
            JsonArray layouts = new JsonArray(mode);
            verticalDivCount++;
            horizontalDivCount++;
            for (int k = 0; k < verticalDivCount; k++) {
                y = +heightDiv * k + space;
                for (int j = 0; j < horizontalDivCount; j++) {
                    x = +widthDiv * j + space;

                    JsonObject obj = new JsonObject();
                    obj.addProperty("left", x);
                    obj.addProperty("top", y);
                    obj.addProperty("width", width);
                    obj.addProperty("height", height);
                    layouts.add(obj);
                }
            }

            layoutMap.put(layoutMode, layouts);
        }

        log.info("layout init result:{}" ,layoutMap.toString());
    }

    public static JsonArray getLayoutByMode(LayoutModeEnum layoutModeEnum) {
        return layoutMap.get(layoutModeEnum);
    }


}
