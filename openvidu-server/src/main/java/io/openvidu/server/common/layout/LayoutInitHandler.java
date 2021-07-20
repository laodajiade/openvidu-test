package io.openvidu.server.common.layout;

import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.openvidu.server.common.enums.LayoutModeEnum;
import io.openvidu.server.common.enums.LayoutModeTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class LayoutInitHandler {

    private static final ConcurrentHashMap<LayoutModeEnum, JsonArray> normalLayoutMap = new ConcurrentHashMap<>(16);
    private static final ConcurrentHashMap<LayoutModeEnum, JsonArray> rostrumLayoutMap = new ConcurrentHashMap<>(16);
    private static final ConcurrentHashMap<LayoutModeEnum, JsonArray> rostrumT200LayoutMap = new ConcurrentHashMap<>(16);


    @PostConstruct
    public void init() {
        try {
            initLayout(LayoutModeTypeEnum.NORMAL);
            initLayout(LayoutModeTypeEnum.ROSTRUM);
            initLayout(LayoutModeTypeEnum.ROSTRUM_T200);
        } catch (Exception e) {
            log.error("layout init error,exit", e);
            System.exit(1);
        }
        log.info(normalLayoutMap.toString());
        log.info(rostrumLayoutMap.toString());
        log.info(rostrumT200LayoutMap.toString());
    }

    private void initLayout(LayoutModeTypeEnum typeEnum) throws IOException {
        ClassPathResource resource = null;
        ConcurrentHashMap<LayoutModeEnum, JsonArray> layoutMap = null;
        switch (typeEnum) {
            case NORMAL:
                resource = new ClassPathResource("layout.json");
                layoutMap = normalLayoutMap;
                break;
            case ROSTRUM:
                resource = new ClassPathResource("rostrum-layout.json");
                layoutMap = rostrumLayoutMap;
                break;
            case ROSTRUM_T200:
                resource = new ClassPathResource("rostrum-t200-layout.json");
                layoutMap = rostrumT200LayoutMap;
                break;
            default:
                log.error("LayoutModeTypeEnum not found {},exit", typeEnum.name());
                System.exit(1);
        }

        List<String> lines = IOUtils.readLines(resource.getInputStream(), "UTF-8");
        String json = Joiner.on("").join(lines);
        log.info("layout json "+json);
        initLayout(json, layoutMap);
    }

    private void initLayout(String layoutJson, Map<LayoutModeEnum, JsonArray> layoutMap) {
        JsonArray layoutArray = new Gson().fromJson(layoutJson, JsonArray.class);
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
    }

    public static JsonArray getLayoutByMode(LayoutModeEnum layoutModeEnum) {
        return getLayoutByMode(LayoutModeTypeEnum.NORMAL, layoutModeEnum);
    }

    public static JsonArray getLayoutByMode(LayoutModeTypeEnum type, LayoutModeEnum layoutModeEnum) {
        if (type == LayoutModeTypeEnum.NORMAL) {
            return normalLayoutMap.get(layoutModeEnum);
        } else if (type == LayoutModeTypeEnum.ROSTRUM) {
            return rostrumLayoutMap.get(layoutModeEnum);
        } else if (type == LayoutModeTypeEnum.ROSTRUM_T200) {
            return rostrumT200LayoutMap.get(layoutModeEnum);
        }
        return normalLayoutMap.get(layoutModeEnum);
    }

}
