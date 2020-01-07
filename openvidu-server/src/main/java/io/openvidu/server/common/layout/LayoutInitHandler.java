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

    @Value("${conference.layout}")
    private String conferenceLayouts;

    private static ConcurrentHashMap<LayoutModeEnum, JsonArray> layoutMap = new ConcurrentHashMap<>(20);
    @PostConstruct
    public void init() {
        JsonArray layoutArray = new Gson().fromJson(conferenceLayouts, JsonArray.class);
        int mode, x, y, width, height;
        for (JsonElement element : layoutArray) {
            JsonObject item = element.getAsJsonObject();
            mode = item.get("mode").getAsInt();
            LayoutModeEnum layoutMode = LayoutModeEnum.getLayoutMode(mode);
            JsonArray layouts = new JsonArray();
            JsonArray location = item.get("location").getAsJsonArray();
            for (JsonElement jsonElement : location) {
                JsonObject json = jsonElement.getAsJsonObject();
                JsonObject obj = new JsonObject();
                x = json.get("x").getAsInt();
                y = json.get("y").getAsInt();
                width = json.get("width").getAsInt();
                height = json.get("height").getAsInt();

                obj.addProperty("left", x);
                obj.addProperty("top", y);
                obj.addProperty("width", width);
                obj.addProperty("height", height);
                layouts.add(obj);

            }
            layoutMap.put(layoutMode, layouts);
        }
        log.info("layout init result:{}", layoutMap.toString());
    }

    public static JsonArray getLayoutByMode(LayoutModeEnum layoutModeEnum) {
        return layoutMap.get(layoutModeEnum);
    }

}
