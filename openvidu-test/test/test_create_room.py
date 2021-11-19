import sys
import threading
import time

import unittest2
from loguru import logger

import test
from test.service.services import ApptService

try:
    import thread
except ImportError:
    import _thread as thread


class TestCreateRoom(test.MyTestCase):
    """ 创建会议相关用例 """

    def test_create_personal(self):
        """ 创建个人会议，不入会,1秒后关闭会议 """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        user = self.users[0]
        client = self.loginAndAccessIn(user['phone'], user['pwd'])
        re = self.createPersonalRoom(client)
        time.sleep(0.5)
        re = client.close_room(self.room_id)
        self.assertEqual(re[0], 0, msg=re[1])

    def test_create_personal_twice(self):
        """ 创建个人会议室2次 """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        user = self.users[0]
        client = self.loginAndAccessIn(user['phone'], user['pwd'])
        self.createPersonalRoom(client)
        time.sleep(0.5)
        self.createPersonalRoom(client)
        re = client.close_room(self.room_id)
        self.assertEqual(re[0], 0, msg=re[1])

    def test_create_random(self):
        """ 创建个人会议，不入会,1秒后关闭会议 """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        user = self.users[0]
        client = self.loginAndAccessIn(user['phone'], user['pwd'])
        self.moderatorClient = client
        self.room_id = ''
        re = client.createRoom(self.room_id, client.uuid + '的随机会议', 'random')
        self.assertEqual(re[0], 0, msg=re[1])
        self.room_id = re[1]['roomId']
        time.sleep(0.5)
        re = client.close_room(self.room_id)
        self.assertEqual(re[0], 0, msg=re[1])

    def a_test_create_same_room_current(self):
        """ 并发创建相同的会议
        测试目的：并发创建相同的会议
        测试过程: 1、登录多个客户端，
        2、同时对一个固定会议室创建会议
        结果期望：并发创建相同的会议，最终只有一个能成功。
        """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        client1 = self.loginAndAccessIn2(self.users[0])
        client2 = self.loginAndAccessIn2(self.users[1])
        client3 = self.loginAndAccessIn2(self.users[2])
        client4 = self.loginAndAccessIn2(self.users[3])
        time.sleep(0.5)
        room_id = self.fixed_rooms[0]['roomId']
        t1 = threading.Thread(target=client1.createRoom, args=(room_id, client1.uuid + '并发创建的会议', 'fixed'))
        t2 = threading.Thread(target=client2.createRoom, args=(room_id, client2.uuid + '并发创建的会议', 'fixed'))
        t3 = threading.Thread(target=client3.createRoom, args=(room_id, client3.uuid + '并发创建的会议', 'fixed'))
        t4 = threading.Thread(target=client4.createRoom, args=(room_id, client4.uuid + '并发创建的会议', 'fixed'))
        t1.start()
        t2.start()
        t3.start()
        t4.start()
        time.sleep(5)

    def a_test_create_different_room_current(self):
        """ 并发创建不同的会议
        测试目的：并发创建不同的会议
        测试过程: 1、登录多个客户端，
        2、创建随机会议
        结果期望：各自不受到全局锁的影响
        """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        client1 = self.loginAndAccessIn2(self.users[0])
        client2 = self.loginAndAccessIn2(self.users[1])
        client3 = self.loginAndAccessIn2(self.users[2])
        client4 = self.loginAndAccessIn2(self.users[3])
        time.sleep(0.5)
        t1 = threading.Thread(target=client1.createRoom, args=(client1.uuid, client1.uuid + '并发创建的会议', 'fixed'))
        t2 = threading.Thread(target=client2.createRoom, args=(client2.uuid, client2.uuid + '并发创建的会议', 'fixed'))
        t3 = threading.Thread(target=client3.createRoom, args=(client3.uuid, client3.uuid + '并发创建的会议', 'fixed'))
        t4 = threading.Thread(target=client4.createRoom, args=(client4.uuid, client4.uuid + '并发创建的会议', 'fixed'))
        t1.start()
        t2.start()
        t3.start()
        t4.start()
        time.sleep(5)
        client1.close_room(client1.uuid)
        client2.close_room(client2.uuid)
        client3.close_room(client3.uuid)
        client4.close_room(client4.uuid)

    def test_create_fixed(self):
        """ 创建固定会议，不入会,1秒后关闭会议 """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        user = self.users[0]
        client = self.loginAndAccessIn(user['phone'], user['pwd'])
        fix_room = self.fixed_rooms[0]

        self.moderatorClient = client
        self.room_id = fix_room['roomId']
        re = client.createRoom(self.room_id, client.uuid + '的固定会议', 'fixed')
        self.assertEqual(re[0], 0, msg=re[1])
        self.room_id = re[1]['roomId']
        time.sleep(0.5)
        re = client.close_room(self.room_id)
        self.assertEqual(re[0], 0, msg=re[1])

    def test_create_fixed_2(self):
        """ 创建固定会议，然后第二个人创建相同的固定会议室 """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        user = self.users[0]
        client = self.loginAndAccessIn(user['phone'], user['pwd'])
        fix_room = self.fixed_rooms[0]
        # 第一个人
        self.moderatorClient = client
        room_id = fix_room['roomId']
        re = client.createRoom(room_id, client.uuid + '的固定会议', 'fixed')
        self.assertEqual(re[0], 0, msg=re[1])

        # 第二个人
        time.sleep(1)
        user2 = self.users[1]
        client2 = self.loginAndAccessIn(user2['phone'], user2['pwd'])
        re = client2.createRoom(room_id, client2.uuid + '的第二个固定会议', 'fixed')
        self.assertEqual(re[0], 13077, msg=re[1])  # {"code":13077,"message":"该会议号已有正在进行的会议"}
        time.sleep(0.5)

        client.close_room(room_id)

    def test_create_appt(self):
        """ 创建一个不存在的ruid的会议 """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        user = self.users[0]
        client = self.loginAndAccessIn(user['phone'], user['pwd'])
        self.moderatorClient = client
        self.room_id = user['uuid']
        re = client.createRoom(self.room_id, client.uuid + '的会议', 'fixed', ruid='appt-abcdefghfe')
        self.assertEqual(re[0], 13054, msg=re[1])  # {"code":13054,"message":"预约会议不存在"}
        re = client.createRoom(self.room_id, client.uuid + '的会议', 'fixed', ruid='abcdefghfe')
        self.assertEqual(re[0], 13001, msg=re[1])  # {"code":13001,"message":"会议不存在"}

    def test_create_appt_person(self):
        """ 创建一个预约会议，然后提前开始 """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        user = self.users[0]
        client = self.loginAndAccessIn(user['phone'], user['pwd'])
        apptService = ApptService(client)
        re = apptService.create_person_appt()
        self.assertEqual(re[0], 0, msg=re[1])
        ruid = re[1]['ruid']  # 获取到创建的ruid
        room_id = re[1]['roomId']  # 获取到创建的roomId
        time.sleep(0.5)
        re = client.createRoom(room_id, client.uuid + '的会议', 'personal', ruid=ruid)
        self.assertEqual(re[0], 0, msg=re[1])

    def test_create_1(self):
        """ 更多设置,入会静音模式为智能静音
        模块名称	 会议
        测试项目	 创建会议
        测试点	更多设置,入会静音模式为智能静音
        预期结果  进入会议后，墙上参会者（参会者前6位）默认为非静音状态，墙下参会者为默认的静音状态。
        """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        moderator = self.users[0]
        moderator_client = self.loginAndAccessIn(moderator['phone'], moderator['pwd'])
        room_id = moderator['uuid']
        re = moderator_client.createRoom(room_id, room_id + '的入会静音会议', 'personal', quietStatusInRoom='smart')
        self.assertEqual(re[0], 0, ' createRoom error ' + str(re[1]))
        re = self.joinRoom(moderator_client, room_id)
        self.assertEqual(re[1]['roomInfo']['quietStatusInRoom'], 'smart', '会议属性 quietStatusInRoom 不对')

        for i in range(1, 10):
            part = self.users[i]
            part_client = self.loginAndAccessIn(part['phone'], part['pwd'])
            re = self.joinRoom(part_client, room_id)
            part_size = i + 1
            if part_size <= self.smart_mic_on_threshold:
                self.assertEqual(re[1]['roomInfo']['micStatusInRoom'], 'on', str(i) + '麦克风状态错误')
            else:
                self.assertEqual(re[1]['roomInfo']['micStatusInRoom'], 'off', str(i) + '麦克风状态错误')

    def test_create_2(self):
        """更多设置，入会静音模式为全部静音
        模块名称	 会议
        测试项目	 创建会议
        测试点	更多设置，入会静音模式为全部静音
        预期结果  除主持人不受影响外，其他参会者进入会议后都是默认的静音状态。
        """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        moderator = self.users[0]
        moderator_client = self.loginAndAccessIn(moderator['phone'], moderator['pwd'])
        room_id = moderator['uuid']
        re = moderator_client.createRoom(room_id, room_id + '的入会静音会议', 'personal', quietStatusInRoom='off')
        self.assertEqual(re[0], 0, ' createRoom error ' + str(re[1]))
        self.joinRoom(moderator_client, room_id)

        part = self.users[1]
        part_client = self.loginAndAccessIn(part['phone'], part['pwd'])
        re = self.joinRoom(part_client, room_id)
        self.assertEqual(re[1]['roomInfo']['micStatusInRoom'], 'off')

    def test_create_3(self):
        """更多设置，ID入会对象仅为主持人
        模块名称	 会议
        测试项目	 创建会议
        测试点	更多设置，ID入会对象仅为主持人
        预期结果  除主持人外，其他参会者无法通过ID加入此会议。
        """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        moderator = self.users[0]
        moderator_client = self.loginAndAccessIn(moderator['phone'], moderator['pwd'])
        room_id = moderator['uuid']
        re = moderator_client.createRoom(room_id, room_id + '的入会静音会议', 'personal', useIdInRoom='onlyModerator')
        self.assertEqual(re[0], 0, ' createRoom error ' + str(re[1]))
        self.joinRoom(moderator_client, room_id)

        part = self.users[1]
        part_client = self.loginAndAccessIn(part['phone'], part['pwd'])
        re = part_client.joinRoom(room_id)
        self.assertEqual(re[0], 13001, ' error 除主持人外，其他参会者无法通过ID加入此会议。')  # 会议不存在

    @unittest2.skipIf(sys.modules.get('fast_test'), '跳过耗时超过60s的用例')
    def test_moderate_disconnected(self):
        """测试主持人掉线会议存在被关闭情况
        描述：测试主持人掉线
        测试目的：测试主持人掉线会议存在被关闭情况
        测试过程: 1、创建会议，主持人入会，与会者入会，
               2、主持人掉线，等待130秒
        结果期望： 主持人掉线后，会议中如果有人不应关闭会议
        """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        if self.fast:
            logger.info('跳过耗时用例')
            return
        moderator_client, room_id = self.loginAndAccessInAndCreateAndJoin(self.users[10])
        part_client, re = self.loginAndAccessInAndJoin(self.users[1], room_id)
        moderator_client.close_ping_pong()
        time.sleep(130)
        logger.info('等待130s后')
        part_client, re = self.loginAndAccessInAndJoin(self.users[2], room_id)
        self.assertEqual(re[0], 0, '会议被关闭了')


if __name__ == '__main__':
    unittest2.main()
