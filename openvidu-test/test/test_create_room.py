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
        res = client.close_room(client.room_id)
        self.assertEqual(res[0], 0, msg=re[1])

    def test_create_random2(self):
        """创建随机会议，不入会，1s后关闭会议"""
        logger.info(getattr(self,sys._getframe().f_code.co_name).__doc__)
        user = self.users[0]
        client = self.loginAndAccessIn(user['phone'],user['pwd'])
        re = self.createRandomRoom(client)
        print('创建随机会议的会议链接：',re[1]['inviteUrl'])
        time.sleep(1)
        res = client.close_room(re[1]['roomId'])
        self.assertEqual(res[0], 0, msg='关闭会议失败')

    def test_create_personal_twice(self):
        """ 创建个人会议室2次 """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        user = self.users[0]
        client = self.loginAndAccessIn(user['phone'], user['pwd'])
        self.createPersonalRoom(client)
        time.sleep(0.5)
        self.createPersonalRoom(client)
        re = client.close_room(client.room_id)
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
        re = client.close_room(client.room_id)
        self.assertEqual(re[0], 0, msg=re[1])

    @unittest2.skipIf(sys.modules.get('fast_test'), '跳过耗时用例')
    def test_create_not_join(self):
        """ 创建会议，不加入会议
        测试目的：创建会议，不加入会议，并掉线，看是否会关闭会议
        测试过程:
        结果期望：空会议在1分钟后应该被释放 """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        user = self.users[0]
        client = self.loginAndAccessIn(user['phone'], user['pwd'])
        # self.moderatorClient = client
        result = self.createRandomRoom(client)
        room_id = result[1]['roomId']
        time.sleep(80)
        result = client.joinRoom(room_id)
        self.assertEqual(result[0], 13001, '入会应该失败，因为会议不存在')

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
        re = apptService.create_person_appt() #创建个人预约会议
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


    def test_createRoomOfAll(self):
        """
        创建会议：预设为ID入会未全体参会人员
        模块名称 会议
        测试项目 创建会议
        测试点 会议预设的更多设置，ID入会为所有参会者
        预期结果 除主持人外，其他参会者可通过ID加入此会议

        步骤：
        1、主持人创建会议
        2、其他参会者根据会议ID可加入会议成功
        3、主持人关闭会议
        """
        logger.info(getattr(self,sys._getframe().f_code.co_name).__doc__)
        moderator = self.users[0]
        moderator_client = self.loginAndAccessIn(moderator['phone'],moderator['pwd'])
        room_id = moderator['uuid']
        re = moderator_client.createRoom(room_id,room_id + '的个人会议（其他人可通过ID入会）', 'personal')
        self.assertEqual(re[0],0,' createRoom error ' + str(re[1]))
        re_moderator = moderator_client.joinRoom(room_id) #主持人加入会议
        self.assertEqual(re_moderator[0],0,' moderator join room fail ' + str(re_moderator[1]))

        part = self.users[1] #参会者身份
        part_client = self.loginAndAccessIn(part['phone'],part['pwd'])
        re_part = part_client.joinRoom(room_id)  #参会者入会，可入会成功
        self.assertEqual(re_part[0],0,' parter join room fail ' + str(re_part[1]))
        moderator_client.close_room(room_id) #主持人关闭会议




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
        moderator_client, room_id = self.loginAndAccessInAndCreateAndJoin(self.users[10])
        part_client, re = self.loginAndAccessInAndJoin(self.users[1], room_id)
        moderator_client.close_ping_pong()
        time.sleep(130)
        logger.info('等待130s后')
        part_client, re = self.loginAndAccessInAndJoin(self.users[2], room_id)
        self.assertEqual(re[0], 0, '会议被关闭了，但是不应该被关闭')

    @unittest2.skipIf(sys.modules.get('fast_test'), '跳过耗时超过60s的用例')
    def test_moderate_disconnected_2(self):
        """ 测试会议中仅主持人的情况下，主持人掉线，应关闭会议。
        描述：测试会议中仅主持人的情况下，主持人掉线，应关闭会议。
        测试目的：测试主持人掉线会议存在被关闭情况
        测试过程: 1、创建会议，主持人入会
               2、主持人掉线，等待130秒
        结果期望： 主持人掉线后，应关闭会议
        """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        moderator_client, room_id = self.loginAndAccessInAndCreateAndJoin(self.users[10])
        moderator_client.close_ping_pong()
        moderator_client.request('accessDevInf', {'pwd': 'sudi123456', 'method': 'setConnectedExpired'})
        time.sleep(130)
        logger.info('等待130s后')
        part_client = self.loginAndAccessIn2(self.users[2])
        result = part_client.joinRoom(room_id)
        self.assertNotEqual(result[0], 0, '会议被关闭了')

    def test_create_room_by_emoji(self):
        """ 创建会议使用emoji表情
        描述：创建会议使用emoji表情，数据库写入失败后，内存中产生脏数据
        测试目的：在创建会议失败后，及时释放资源、清理数据
        测试过程: 1、创建会议，desc使用emoji表情。
               2、创建会议应失败，
               3、重新创建会议，不使用emoji
        结果期望：第三步应成功
        """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        moderator_client = self.loginAndAccessIn2(self.users[0])
        try:
            result = moderator_client.createRoom(moderator_client.uuid, '失败的会议😀', room_id_type='personal')
            self.assertNotEqual(result[0], 0, '创建会议应该失败')
            result = moderator_client.createRoom(moderator_client.uuid, '成功会议', room_id_type='personal')
            self.assertEqual(result[0], 0, '创建会议应该失败')
        finally:
            moderator_client.close_room(moderator_client.uuid)

    def test_getMemberDetails_img(self):
        """
        ----------zyx个人练习-请忽略-------------
        登录
        根据uuid获取个人（80103600010）的头像并打印
        头像地址字段：userIcon
        """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        user = self.users[0]
        user_client = self.loginAndAccessIn(user['phone'],user['pwd'])
        uuid = user_client.uuid
        re = user_client.getMemberDetails(uuid)
        print('------------------------',re[1]['userIcon'])
        self.assertEqual(re[0],0,'获取图像链接失败')


    def test_getUploadToken(self):
        """
        ----------zyx个人练习-请忽略-------------
        获取头像上传地址
        """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        user = self.users[0]
        user_client = self.loginAndAccessIn(user['phone'],user['pwd'])
        type = 'UserIcon'
        re = user_client.getUploadToken(type)
        print('------------------------',re[1]['uploadUrl'])
        self.assertEqual(re[0],0,'获取图像上传链接失败')



if __name__ == '__main__':
    unittest2.main()
