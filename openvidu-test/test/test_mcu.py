import sys
import time
from datetime import datetime

import unittest2
from loguru import logger

import test
from test.service.services import MeetingService


class TestMCU(test.MyTestCase):
    """ MCU用例 """

    def test_mcu_simple(self):
        """ 创建会议, 主持人入会，强制开启MCU，第二人入会
            期望：第二个人入会将使用mcu模式
        """
        moderator_client, room_id = self.loginAndAccessInAndCreateAndJoin(self.users[0])
        logger.info('强制开启MCU')
        self.set_mcu_mode(moderator_client)
        time.sleep(3)

        part_client, re = self.loginAndAccessInAndJoin(self.users[1], room_id)
        mix_stream_id = re[1]['roomInfo']['mixFlows'][0]['streamId']
        self.assertIsNotNone(mix_stream_id, '混流id不存在')

        logger.info('与会者拉mcu画面')
        re = part_client.subscribe_mcu(mix_stream_id)
        self.assertIsNotNone(re[1]['sdpAnswer'], '没有sdpAnswer，拉流失败')
        subscribeId = re[1]['subscribeId']
        time.sleep(2)
        logger.info('与会者停止拉mcu画面')
        re = part_client.unsubscribe_video(subscribeId)

    def test_mcu_normal(self):
        """ 创建会议, 15人入会,墙上推流，
         期望：后面入会的人看到会议变MCU模式
         """
        # 主持人入会
        moderator_client, room_id = self.loginAndAccessInAndCreateAndJoin(self.users[0])
        moderator_client.ms = MeetingService(moderator_client, room_id)
        # moderator_client.collecting_notify()

        # 主持人推流
        major_stream_id = self.publish_video(moderator_client, 'MAJOR')
        time.sleep(1)
        clients = []
        # 墙上入会并推流
        for i in range(1, 9):
            user = self.users[i]
            client = self.loginAndAccessIn(user['phone'], user['pwd'])
            client.joinRoom(room_id)
            self.publish_video(client, 'MAJOR')
            clients.append(client)

        # 墙下入会
        for i in range(9, 15):
            user = self.users[i]
            client = self.loginAndAccessIn(user['phone'], user['pwd'])
            client.joinRoom(room_id)
            clients.append(client)
            time.sleep(0.1)
        self.set_mcu_mode(moderator_client)
        # 墙上离会，墙下顶上
        time.sleep(3)
        moderator_client.collecting_notify()
        moderator_client.clear_notify()
        logger.info("墙上离开会议")
        clients[3].leave_room(room_id)
        time.sleep(2)

        notify = moderator_client.search_notify_list('partOrderOrRoleChangeNotify')[0]
        leave_uuid = clients[3].uuid  # 离会的uuid
        up_uuid = notify['params']['roleChange'][0]['uuid']  # 获取上墙的uuid
        logger.info('上墙', up_uuid)
        notify = moderator_client.search_notify_list('conferenceLayoutChanged')[0]
        coordinates = notify['params']['layoutInfo']['linkedCoordinates']

        up_exist = False
        for c in coordinates:
            if c['uuid'] == leave_uuid:
                self.assertTrue(False, '离会的uuid还在布局中 ' + str(notify))
            if c['uuid'] == up_uuid:
                # self.assertFalse(c['streaming'], ' 流状态应是false' + str(notify))
                up_exist = True
        self.assertTrue(up_exist, '上墙的uuid不在布局中 ' + str(notify))

        time.sleep(2)
        logger.info("刚上墙的准备推流")
        moderator_client.clear_notify()
        self.search_client(clients, up_uuid).publish_video('MAJOR')
        time.sleep(1)
        notifys = moderator_client.search_notify_list('conferenceLayoutChanged')
        self.assertEqual(len(notifys), 0, "不会推布局变化的通知")

    def test_mcu_share(self):
        """ 测试分享布局，普通的分享布局
        步骤：1、创建会议, 15人入会,第二位分享
        """
        # 主持人入会
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        moderator = self.users[0]
        moderator_client = self.loginAndAccessIn(moderator['phone'], moderator['pwd'])
        re = self.createRandomRoom(moderator_client)
        room_id = re[1]['roomId']
        self.joinRoom(moderator_client, room_id)
        moderator_client.ms = MeetingService(moderator_client, room_id)

        # 主持人推流
        self.publish_video(moderator_client, 'MAJOR')
        clients = []
        # 墙上入会并推流
        for i in range(1, 9):
            user = self.users[i]
            client = self.loginAndAccessIn(user['phone'], user['pwd'])
            client.joinRoom(room_id)
            self.publish_video(client, 'MAJOR')
            clients.append(client)

        time.sleep(1)
        self.set_mcu_mode(moderator_client)

        # 墙下入会
        for i in range(9, 15):
            user = self.users[i]
            client = self.loginAndAccessIn(user['phone'], user['pwd'])
            client.joinRoom(room_id)
            clients.append(client)
            time.sleep(0.1)
        self.set_mcu_mode(moderator_client)

        # 墙上分享分享
        time.sleep(3)
        moderator_client.collecting_notify()
        moderator_client.clear_notify()
        logger.info("墙上开始分享")
        share_client = clients[0]
        re = share_client.request("applyShare", {"targetId": share_client.uuid})
        self.assertEqual(re[0], 0, ' 申请分享失败')
        time.sleep(2)
        notify = moderator_client.search_notify_list('conferenceLayoutChanged')[0]
        coordinates = notify['params']['layoutInfo']['linkedCoordinates']
        # self.assertFalse(coordinates[0]['streaming'], ' 还没开始推流，流状态应是false' + str(notify))
        self.assertEqual(coordinates[0]['streamType'], 'SHARING', ' 还没开始推流，流状态应是false' + str(notify))
        self.assertEqual(coordinates[0]['top'], 2, ' 布局不正确' + str(notify))
        self.assertEqual(coordinates[0]['left'], 323, ' 布局不正确' + str(notify))
        self.assertEqual(coordinates[0]['width'], 1274, ' 布局不正确' + str(notify))
        self.assertEqual(coordinates[0]['height'], 717, ' 布局不正确' + str(notify))
        self.assertEqual(len(coordinates), 4, '1+3布局不正确' + str(notify))

        logger.info('分享者开始推分享流,只停止推流不影响布局变化')
        moderator_client.clear_notify()
        share_client.publish_video('SHARING')

        logger.info('直接结束分享')
        moderator_client.clear_notify()
        re = share_client.request("endShare", {"targetId": share_client.uuid})
        self.assertEqual(re[0], 0, '结束分享失败')
        time.sleep(2)
        notify = moderator_client.search_notify_list('conferenceLayoutChanged')[0]
        coordinates = notify['params']['layoutInfo']['linkedCoordinates']
        # self.assertTrue(coordinates[0]['streaming'], ' 还没开始推流，流状态应是false' + str(notify))
        self.assertEqual(len(coordinates), 9, '布局应是9等分布局')

    def test_mcu_speak(self):
        """测试普通的发言布局
        步骤：1、创建会议, 15人入会,第二位分享
        """
        # 主持人入会
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        moderator = self.users[0]
        moderator_client = self.loginAndAccessIn(moderator['phone'], moderator['pwd'])
        re = self.createRandomRoom(moderator_client)
        room_id = re[1]['roomId']
        self.joinRoom(moderator_client, room_id)
        moderator_client.ms = MeetingService(moderator_client, room_id)

        # 主持人推流
        self.publish_video(moderator_client, 'MAJOR')
        clients = []
        # 墙上入会并推流
        for i in range(1, 9):
            user = self.users[i]
            client = self.loginAndAccessIn(user['phone'], user['pwd'])
            client.joinRoom(room_id)
            self.publish_video(client, 'MAJOR')
            clients.append(client)

        self.set_mcu_mode(moderator_client)
        # 墙下入会
        for i in range(9, 15):
            user = self.users[i]
            client = self.loginAndAccessIn(user['phone'], user['pwd'])
            client.joinRoom(room_id)
            clients.append(client)
            time.sleep(0.1)
        self.set_mcu_mode(moderator_client)

        logger.info("主持人让1号位发言")
        time.sleep(3)
        moderator_client.collecting_notify()
        moderator_client.clear_notify()
        speaker_client = clients[0]
        re = moderator_client.request("setRollCall", {"roomId": room_id, "originator": moderator_client.uuid,
                                                      "targetId": speaker_client.uuid})
        self.assertEqual(re[0], 0, ' 点名发言失败')
        time.sleep(2)
        notify = moderator_client.search_notify_list('conferenceLayoutChanged')[0]
        coordinates = notify['params']['layoutInfo']['linkedCoordinates']
        self.assertEqual(coordinates[0]['uuid'], speaker_client.uuid, ' uuid不对' + str(notify))
        # self.assertTrue(coordinates[0]['streaming'], ' 应是开始推流' + str(notify))
        self.assertEqual(coordinates[0]['streamType'], 'MAJOR', ' 还没开始推流，流状态应是false' + str(notify))
        self.assertEqual(coordinates[0]['top'], 2, ' 布局不正确' + str(notify))
        self.assertEqual(coordinates[0]['left'], 323, ' 布局不正确' + str(notify))
        self.assertEqual(coordinates[0]['width'], 1274, ' 布局不正确' + str(notify))
        self.assertEqual(coordinates[0]['height'], 717, ' 布局不正确' + str(notify))
        self.assertEqual(len(coordinates), 4, '1+3布局')

        logger.info('主持人让1号位结束发言')
        moderator_client.clear_notify()
        re = moderator_client.request("endRollCall", {"roomId": room_id, "originator": moderator_client.uuid,
                                                      "targetId": speaker_client.uuid})
        self.assertEqual(re[0], 0, '结束发言失败')
        time.sleep(2)
        notify = moderator_client.search_notify_list('conferenceLayoutChanged')[0]
        coordinates = notify['params']['layoutInfo']['linkedCoordinates']
        # self.assertTrue(coordinates[0]['streaming'], ' 还没开始推流，流状态应是false' + str(notify))
        self.assertEqual(len(coordinates), 9, '布局应是等分布局')

    def test_mcu_speak_and_share(self):
        """测试发言+分享的布局
        测试目的：测试发言+分享的布局
        测试过程: 1、创建会议，主持人入会，与会者入会，
                2、order2 分享
                3、主持人让order3 发言
                4、order2 停止分享
        结果期望： step2:布局大画面分享
                 step3:布局大画面分享，小画面1是发言
                 step4:布局发言
        """
        # 主持人入会
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        logger.info("step 1")
        moderator = self.users[0]
        moderator_client = self.loginAndAccessIn(moderator['phone'], moderator['pwd'])
        re = self.createRandomRoom(moderator_client)
        room_id = re[1]['roomId']
        self.joinRoom(moderator_client, room_id)
        moderator_client.ms = MeetingService(moderator_client, room_id)

        # 主持人推流
        self.publish_video(moderator_client, 'MAJOR')
        clients = []
        # 墙上入会并推流
        for i in range(1, 9):
            user = self.users[i]
            client = self.loginAndAccessIn(user['phone'], user['pwd'])
            client.joinRoom(room_id)
            self.publish_video(client, 'MAJOR')
            clients.append(client)

        # 墙下入会
        logger.info("墙下入会，凑人数，触发从SFU转MCU")
        for i in range(9, 15):
            user = self.users[i]
            client = self.loginAndAccessIn(user['phone'], user['pwd'])
            client.joinRoom(room_id)
            clients.append(client)
            time.sleep(0.1)
        self.set_mcu_mode(moderator_client)

        # 墙上分享
        logger.info("step 2")
        time.sleep(3)
        logger.info('1号位开始分享并推流')
        moderator_client.collecting_notify()
        moderator_client.clear_notify()
        logger.info("墙上开始分享")
        share_client = clients[0]
        re = share_client.request("applyShare", {"targetId": share_client.uuid})
        self.assertEqual(re[0], 0, ' 申请分享失败')
        logger.info('分享者开始推分享流')
        share_client.publish_video('SHARING')
        time.sleep(2)
        notify = moderator_client.search_notify_list('conferenceLayoutChanged')[0]
        coordinates = notify['params']['layoutInfo']['linkedCoordinates']
        self.assertEqual(coordinates[0]['uuid'], share_client.uuid, ' uuid不对' + str(notify))
        self.assertEqual(coordinates[0]['streamType'], 'SHARING', ' 还没开始推流，流状态应是false' + str(notify))
        self.assertEqual(coordinates[0]['top'], 2, ' 布局不正确' + str(notify))
        self.assertEqual(coordinates[0]['left'], 323, ' 布局不正确' + str(notify))
        self.assertEqual(coordinates[0]['width'], 1274, ' 布局不正确' + str(notify))
        self.assertEqual(coordinates[0]['height'], 717, ' 布局不正确' + str(notify))
        self.assertEqual(len(coordinates), 4, '1+3布局不正确' + str(notify))

        logger.info("step 3")
        time.sleep(3)
        logger.info("主持人让2号位发言")
        moderator_client.clear_notify()
        speaker_client = clients[1]
        re = moderator_client.request("setRollCall", {"roomId": room_id, "originator": moderator_client.uuid,
                                                      "targetId": speaker_client.uuid})
        self.assertEqual(re[0], 0, ' 点名发言失败')
        time.sleep(2)
        # 大画面布局是分享流
        notify = moderator_client.search_notify_list('conferenceLayoutChanged')[0]
        coordinates = notify['params']['layoutInfo']['linkedCoordinates']
        self.assertEqual(coordinates[0]['uuid'], share_client.uuid, ' uuid不对' + str(notify))
        self.assertEqual(coordinates[0]['streamType'], 'SHARING', '布局大画面是分享流' + str(notify))
        self.assertEqual(coordinates[0]['top'], 2, ' 布局不正确' + str(notify))
        self.assertEqual(coordinates[0]['left'], 323, ' 布局不正确' + str(notify))
        self.assertEqual(coordinates[0]['width'], 1274, ' 布局不正确' + str(notify))
        self.assertEqual(coordinates[0]['height'], 717, ' 布局不正确' + str(notify))
        self.assertEqual(len(coordinates), 4, '1+3布局')
        # 小画面第一位是发言者
        self.assertEqual(coordinates[1]['uuid'], speaker_client.uuid, ' uuid不对' + str(notify))
        self.assertEqual(coordinates[1]['streamType'], 'MAJOR', '小画面第一位是发言者' + str(notify))

        logger.info('主持人让2号位结束发言，布局变会分享的1+3')
        moderator_client.clear_notify()
        re = share_client.request('endShare', {'targetId': share_client.uuid})
        self.assertEqual(re[0], 0, '结束共享失败')
        time.sleep(2)

        notify = moderator_client.search_notify_list('conferenceLayoutChanged')[0]
        coordinates = notify['params']['layoutInfo']['linkedCoordinates']
        self.assertEqual(coordinates[0]['uuid'], speaker_client.uuid, ' uuid不对' + str(notify))
        self.assertEqual(coordinates[0]['streamType'], 'MAJOR', ' 大画面是发言者' + str(notify))
        self.assertEqual(coordinates[0]['top'], 2, ' 布局不正确' + str(notify))
        self.assertEqual(coordinates[0]['left'], 323, ' 布局不正确' + str(notify))
        self.assertEqual(coordinates[0]['width'], 1274, ' 布局不正确' + str(notify))
        self.assertEqual(coordinates[0]['height'], 717, ' 布局不正确' + str(notify))
        self.assertEqual(len(coordinates), 4, '1+3布局不正确' + str(notify))

    def test_t200_mcu_speak_and_share(self):
        """ 测试主持人是t200的发言布局 """
        # 主持人入会
        moderator = self.users[0]
        moderator_client = self.loginAndAccessIn(moderator['phone'], moderator['pwd'], deviceModel='T200')
        re = self.createRandomRoom(moderator_client)
        room_id = re[1]['roomId']
        self.joinRoom(moderator_client, room_id)
        moderator_client.ms = MeetingService(moderator_client, room_id)

        # 主持人推流
        self.publish_video(moderator_client, 'MAJOR')
        clients = []
        # 墙上入会并推流
        for i in range(1, 9):
            user = self.users[i]
            client = self.loginAndAccessIn(user['phone'], user['pwd'])
            client.joinRoom(room_id)
            self.publish_video(client, 'MAJOR')
            clients.append(client)

        # 墙下入会
        logger.info("墙下入会，凑人数，触发从SFU转MCU")
        for i in range(9, 15):
            user = self.users[i]
            client = self.loginAndAccessIn(user['phone'], user['pwd'])
            client.joinRoom(room_id)
            clients.append(client)
            time.sleep(0.1)
        self.set_mcu_mode(moderator_client)

        # 墙上分享分享
        logger.info('1号位开始分享并推流')
        time.sleep(3)
        moderator_client.collecting_notify()
        moderator_client.clear_notify()
        logger.info("墙上开始分享")
        share_client = clients[0]
        re = share_client.request("applyShare", {"targetId": share_client.uuid})
        self.assertEqual(re[0], 0, ' 申请分享失败')
        logger.info('分享者开始推分享流')
        moderator_client.clear_notify()
        share_client.publish_video('SHARING')
        time.sleep(2)
        notify = moderator_client.search_notify_list('conferenceLayoutChanged')[0]
        coordinates = notify['params']['layoutInfo']['linkedCoordinates']
        self.assertEqual(coordinates[0]['uuid'], share_client.uuid, ' uuid不对' + str(notify))
        self.assertEqual(coordinates[0]['streamType'], 'SHARING', ' 流类型不正确' + str(notify))
        self.assertEqual(coordinates[0]['top'], 2, ' 布局不正确' + str(notify))
        self.assertEqual(coordinates[0]['left'], 196, ' 布局不正确' + str(notify))
        self.assertEqual(coordinates[0]['width'], 1529, ' 布局不正确' + str(notify))
        self.assertEqual(coordinates[0]['height'], 860, ' 布局不正确' + str(notify))
        self.assertEqual(len(coordinates), 6, ' 应是1+5布局' + str(notify))

        time.sleep(3)
        logger.info("主持人让2号位发言，布局变为2+5")
        moderator_client.clear_notify()
        speaker_client = clients[1]
        re = moderator_client.request("setRollCall", {"roomId": room_id, "originator": moderator_client.uuid,
                                                      "targetId": speaker_client.uuid})
        self.assertEqual(re[0], 0, ' 点名发言失败')
        time.sleep(2)
        notify = moderator_client.search_notify_list('conferenceLayoutChanged')[0]
        coordinates = notify['params']['layoutInfo']['linkedCoordinates']
        # 0是共享位
        self.assertEqual(coordinates[0]['uuid'], share_client.uuid, ' uuid不对' + str(notify))
        self.assertEqual(coordinates[0]['streamType'], 'SHARING', ' 流类型不正确' + str(notify))
        self.assertEqual(coordinates[0]['top'], 162, ' 布局不正确' + str(notify))
        self.assertEqual(coordinates[0]['left'], 2, ' 布局不正确' + str(notify))
        self.assertEqual(coordinates[0]['width'], 957, ' 布局不正确' + str(notify))
        self.assertEqual(coordinates[0]['height'], 538, ' 布局不正确' + str(notify))
        self.assertEqual(len(coordinates), 7, ' 应是2+5布局' + str(notify))
        # 1是发言位
        self.assertEqual(coordinates[1]['uuid'], speaker_client.uuid, ' uuid不对' + str(notify))
        self.assertEqual(coordinates[1]['streamType'], 'MAJOR', ' 还没开始推流，流状态应是false' + str(notify))
        self.assertEqual(coordinates[1]['top'], 162, ' 布局不正确' + str(notify))
        self.assertEqual(coordinates[1]['left'], 961, ' 布局不正确' + str(notify))
        self.assertEqual(coordinates[1]['width'], 957, ' 布局不正确' + str(notify))
        self.assertEqual(coordinates[1]['height'], 538, ' 布局不正确' + str(notify))
        self.assertEqual(len(coordinates), 7, '2+5布局')

        logger.info('主持人让1号位结束共享，布局变会分享的1+5')
        moderator_client.clear_notify()
        re = share_client.request("endShare", {"targetId": share_client.uuid})
        self.assertEqual(re[0], 0, '结束共享失败')
        time.sleep(2)
        notify = moderator_client.search_notify_list('conferenceLayoutChanged')[0]
        coordinates = notify['params']['layoutInfo']['linkedCoordinates']
        self.assertEqual(coordinates[0]['uuid'], speaker_client.uuid, ' uuid不对' + str(notify))
        self.assertEqual(coordinates[0]['streamType'], 'MAJOR', ' 还没开始推流，流状态应是false' + str(notify))
        self.assertEqual(coordinates[0]['top'], 2, ' 布局不正确' + str(notify))
        self.assertEqual(coordinates[0]['left'], 196, ' 布局不正确' + str(notify))
        self.assertEqual(coordinates[0]['width'], 1529, ' 布局不正确' + str(notify))
        self.assertEqual(coordinates[0]['height'], 860, ' 布局不正确' + str(notify))
        self.assertEqual(len(coordinates), 6, '1+5布局不正确' + str(notify))

    def test_mcu_subscribe(self):
        """测试拉MCU的流
        测试目的：如果是发布者是需要connected2个hubPort
        测试过程: 1、创建会议，主持人入会，强制开启MCU模式
                2、第二个人入会，并推流
                3、第二个人拉流
        结果期望：第二人拉流时需要同时connected2个hub port。验证需要看信令日志zao
        """
        # 主持人入会
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        logger.info("step 1")
        moderator_client, room_id = self.loginAndAccessInAndCreateAndJoin(self.users[0])
        self.set_mcu_mode(moderator_client)
        time.sleep(2)

        part_client, re = self.loginAndAccessInAndJoin(self.users[1], room_id)
        mix_stream_id = re[1]['roomInfo']['mixFlows'][0]['streamId']
        self.publish_video(part_client, "MAJOR")
        time.sleep(2)

        self.subscribe_mcu_stream(part_client, mix_stream_id)
        time.sleep(2)

    def test_mcu_switch_voice_mode(self):
        """ 测试MCU模式下切换语音模式
        测试目的：切换语音模式不报错
        测试过程: 1、创建会议，开启MCU，入会与会者
                2、主持人推流，与会者拉流
                3、与会者切换语音模式
                4、与会者切回视频模式
        结果期望： 与会者切换语音模式 不报错
        """
        # 主持人入会
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        logger.info("step 1")
        moderator_client, room_id = self.loginAndAccessInAndCreateAndJoin(self.users[0])
        self.set_mcu_mode(moderator_client)
        time.sleep(2)

        part_client, re = self.loginAndAccessInAndJoin(self.users[1], room_id)
        mix_stream_id = re[1]['roomInfo']['mixFlows'][0]['streamId']
        self.publish_video(part_client, "MAJOR")
        time.sleep(2)

        moderator_client.collecting_notify()
        self.subscribe_mcu_stream(part_client, mix_stream_id)
        re = part_client.request('switchVoiceMode', {"operation": "on"})
        self.assertEqual(re[0], 0)
        notify = moderator_client.find_any_notify('switchVoiceModeNotify')
        self.assertEqual(notify['params']['operation'], 'on')
        self.assertEqual(notify['params']['uuid'], part_client.uuid, 'uuid错误')

        moderator_client.clear_notify()
        re = part_client.request('switchVoiceMode', {"operation": "off"})
        self.assertEqual(re[0], 0, '切换语音模式失败')
        notify = moderator_client.find_any_notify('switchVoiceModeNotify')
        self.assertEqual(notify['params']['operation'], 'off')
        self.assertEqual(notify['params']['uuid'], part_client.uuid, 'uuid错误')

    def test_update_conference_layout(self):
        """ 主持人修改MCU布局
        测试目的：主持人修改MCU布局
        测试过程:1、主持人入会，切换MCU模式
        2、与会者入会
        3、主持人上报2人布局
        4、主持人上报1人布局
        结果期望：回调的布局和主持人上报的一样
        """
        # 主持人入会
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        moderator_client, room_id = self.loginAndAccessInAndCreateAndJoin(self.users[0])
        self.set_mcu_mode(moderator_client)
        time.sleep(2)
        part_client, re = self.loginAndAccessInAndJoin(self.users[1], room_id)

        logger.info('step 3')

        layout = []
        layout.append({'uuid': moderator_client.uuid, 'streamType': 'MAJOR'})
        layout.append({'uuid': part_client.uuid, 'streamType': 'MAJOR'})
        params = {'mode': 2, 'layoutModeType': 'NORMAL', 'roomId': room_id, 'timestamp': datetime.now().timestamp(),
                  'layout': layout}

        re = moderator_client.request('updateConferenceLayout', params)
        self.assertEqual(re[0], 0)
        logger.info('step 4')

    ############################################################

    def subscribe_mcu_stream(self, client, publish_id):
        re = client.subscribe_video(client.uuid, 'MAJOR', publish_id, stream_mode='MIX_MAJOR')
        self.assertEqual(re[0], 0, '拉流错误' + publish_id)
        stream_id = re[1]['subscribeId']  # 获取到stream_id
        self.assertIsNotNone(stream_id, ' 拉流没有subscribeId')
        self.assertIsNotNone(re[1]['sdpAnswer'], '推流没有 sdpAnswer')
        time.sleep(1)
        # self.valid_publish_video(client, stream_id)
        return stream_id

    def valid_publish_video(self, client, stream_id):
        # 检查媒体下发的ice candidate
        ice_candidate_notify = client.search_notify_list('iceCandidate')
        for notify in ice_candidate_notify:
            self.assertEqual(notify['params']['endpointName'], stream_id, 'iceCandidate 错误')

        participant_published = client.search_notify_list('participantPublished')
        self.assertEqual(participant_published[0]['params']['streams'][0]['publishId'], stream_id,
                         'participantPublished 错误')

    def on_ice_candidate(self, client, stream_id):
        re = client.on_ice_candidate(stream_id)
        self.assertEqual(re[0], 0)

    def set_mcu_mode(self, client):
        client.request('setConferenceMode', {'mode': 'mcu', 'pwd': 'sudi123', 'roomId': client.room_id})
        logger.info('强制开启MCU')
        time.sleep(1)

    def publish_and_subscribe(self):
        """ 创建会议, 主持人推主流，与会者1拉主流 """
        result = {}
        # 主持人入会
        moderator = self.users[0]
        moderator_client = self.loginAndAccessIn(moderator['phone'], moderator['pwd'])
        re = self.createRandomRoom(moderator_client)
        room_id = re[1]['roomId']
        self.joinRoom(moderator_client, room_id)
        moderator_client.ms = MeetingService(moderator_client, room_id)
        moderator_client.collecting_notify()

        # 主持人入会
        part = self.users[1]
        part_client = self.loginAndAccessIn(part['phone'], part['pwd'])
        self.joinRoom(part_client, room_id)
        part_client.ms = MeetingService(part_client, room_id)
        part_client.collecting_notify()

        publish_id = self.publish_video(moderator_client, 'MAJOR')  # 主持人推流
        subscribe_id = self.subscribe_video(part_client, moderator_client.uuid, 'MAJOR', publish_id)  # 拉流
        time.sleep(1)

        result['moderator_client'] = moderator_client
        result['moderator'] = moderator
        result['part_client'] = part_client
        result['part'] = part
        result['room_id'] = room_id
        result['publish_id'] = publish_id
        result['subscribe_id'] = subscribe_id
        return result


class TestManualLayout(test.MyTestCase):
    """ 主持人上报布局 """

    def test_layout_1(self):
        """ 主持人只有一人的等分布局
        测试目的：测试 主持人上报布局
        测试过程: 1、创建会议，主持人入会，强制开启mcu
                2、主持人推流，上报自己的布局
                3、主持人收到布局回调
        结果期望： step3:回调和主持人上报的一致
        """
        moderator_client, room_id = self.loginAndAccessInAndCreateAndJoin(self.users[0])
        logger.info('强制开启MCU')
        self.set_mcu_mode(moderator_client)
        time.sleep(3)

        logger.info("step 2")
        self.publish_video(moderator_client, 'MAJOR')

        time.sleep(2)
        moderator_client.ms = MeetingService(moderator_client, room_id)
        moderator_client.collecting_notify()
        layouts = [{'uuid': moderator_client.uuid, 'streamType': 'MAJOR'}]
        re = moderator_client.ms.update_conference_layout(1, 'NORMAL', layouts)
        self.assertEqual(re[0], 0, '上报布局错误')
        notify = moderator_client.find_any_notify('conferenceLayoutChanged')
        self.assertEqual(notify['params']['layoutInfo']['mode'], 1, '回调的手动布局错误')
        self.assertEqual(notify['params']['layoutInfo']['linkedCoordinates'][0]['uuid'], moderator_client.uuid,
                         '回调的手动布局错误')

    def test_layout_2(self):
        """ 主持人上报2等分布局
        测试目的：测试 主持人上报布局
        测试过程: 1、创建会议，主持人入会，强制开启mcu
                2、主持人推流，上报自己的布局
                3、主持人收到布局回调
        结果期望： step3:回调和主持人上报的一致
        """
        moderator_client, room_id = self.loginAndAccessInAndCreateAndJoin(self.users[0])
        logger.info('强制开启MCU')
        self.set_mcu_mode(moderator_client)

        logger.info("step 2")
        self.publish_video(moderator_client, 'MAJOR')

        part_client, re = self.loginAndAccessInAndJoin(self.users[1], room_id)
        self.publish_video(part_client, 'MAJOR')

        time.sleep(2)
        moderator_client.ms = MeetingService(moderator_client, room_id)
        moderator_client.collecting_notify()
        layouts = [{'uuid': moderator_client.uuid, 'streamType': 'MAJOR'},
                   {'uuid': part_client.uuid, 'streamType': 'MAJOR'}]
        re = moderator_client.ms.update_conference_layout(1, 'NORMAL', layouts)
        self.assertEqual(re[0], 0, '上报布局错误')
        notify = moderator_client.find_any_notify('conferenceLayoutChanged')
        self.assertEqual(notify['params']['layoutInfo']['mode'], 2, '回调的手动布局错误')
        self.assertEqual(notify['params']['layoutInfo']['linkedCoordinates'][0]['uuid'], moderator_client.uuid,
                         '回调的手动布局错误')
        self.assertEqual(notify['params']['layoutInfo']['linkedCoordinates'][1]['uuid'], part_client.uuid,
                         '回调的手动布局错误')

    def test_layout_part_not_found(self):
        """ 主持人上报不存在的uuid
        测试目的：测试 主持人上报布局
        测试过程: 1、创建会议，主持人入会，强制开启mcu
                2、主持人推流，上报自己的布局
        结果期望： step2: 接口应该报错13017
        """
        moderator_client, room_id = self.loginAndAccessInAndCreateAndJoin(self.users[0])
        logger.info('强制开启MCU')
        self.set_mcu_mode(moderator_client)

        logger.info("step 2")
        self.publish_video(moderator_client, 'MAJOR')

        part_client, re = self.loginAndAccessInAndJoin(self.users[1], room_id)
        self.publish_video(part_client, 'MAJOR')

        time.sleep(2)
        moderator_client.ms = MeetingService(moderator_client, room_id)
        moderator_client.collecting_notify()
        layouts = [{'uuid': moderator_client.uuid, 'streamType': 'MAJOR'},
                   {'uuid': self.users[5]['uuid'], 'streamType': 'MAJOR'}]
        re = moderator_client.ms.update_conference_layout(1, 'NORMAL', layouts)
        self.assertEqual(re[0], 13017, ' 返回值不对 ')


    ###################################################################################

    def subscribe_mcu_stream(self, client, publish_id):
        re = client.subscribe_video(client.uuid, 'MAJOR', publish_id, stream_mode='MIX_MAJOR')
        self.assertEqual(re[0], 0, '拉流错误' + publish_id)
        stream_id = re[1]['subscribeId']  # 获取到stream_id
        self.assertIsNotNone(stream_id, ' 拉流没有subscribeId')
        self.assertIsNotNone(re[1]['sdpAnswer'], '推流没有 sdpAnswer')
        time.sleep(1)
        # self.valid_publish_video(client, stream_id)
        return stream_id

    def valid_publish_video(self, client, stream_id):
        # 检查媒体下发的ice candidate
        ice_candidate_notify = client.search_notify_list('iceCandidate')
        for notify in ice_candidate_notify:
            self.assertEqual(notify['params']['endpointName'], stream_id, 'iceCandidate 错误')

        participant_published = client.search_notify_list('participantPublished')
        self.assertEqual(participant_published[0]['params']['streams'][0]['publishId'], stream_id,
                         'participantPublished 错误')

    def on_ice_candidate(self, client, stream_id):
        re = client.on_ice_candidate(stream_id)
        self.assertEqual(re[0], 0)

    def set_mcu_mode(self, client):
        client.request('setConferenceMode', {'mode': 'mcu', 'pwd': 'sudi123', 'roomId': client.room_id})
        logger.info('强制开启MCU')
        time.sleep(1)


if __name__ == '__main__':
    unittest2.main()
