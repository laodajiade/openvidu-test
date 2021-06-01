package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

//@RunWith(SpringRunner.class)
//@SpringBootTest
//@WebAppConfiguration
public class UrgedPeopleToEndHandlerTest extends AbstractTestCase {


    //@Autowired
    private UrgedPeopleToEndHandler urgedPeopleToEndHandler =new UrgedPeopleToEndHandler();


    @Test
    public void apptNotExist() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("roomId", "123456");
        jsonObject.addProperty("ruid", "appt-123456");
        //urgedPeopleToEndHandler.doProcess(getEmptyRc(), null, jsonObject);

    }
}