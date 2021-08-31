import sys
import threading
import time

import unittest2
from loguru import logger

import test
from test.service.services import MeetingService


class TestJoinRoom(test.MyTestCase):
    """ 加入会议相关用例 """

    def test_create_personal(self):
        """ 创建个人会议，不入会,1秒后关闭会议 """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        client = self.loginAndAccessIn2(self.users[0])
        re = self.createPersonalRoom(client)
        time.sleep(0.5)
        re = client.close_room(self.room_id)
        self.assertEqual(re[0], 0, msg=re[1])

    def test_create_random(self):
        """ 创建个人会议，不入会,1秒后关闭会议 """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        client = self.loginAndAccessIn2(self.users[0])
        self.moderatorClient = client
        self.room_id = ''
        re = client.createRoom(self.room_id, client.uuid + '的随机会议', 'random')
        self.assertEqual(re[0], 0, msg=re[1])
        self.room_id = re[1]['roomId']
        time.sleep(0.5)
        re = client.close_room(self.room_id)
        self.assertEqual(re[0], 0, msg=re[1])

    def test_create_and_join(self):
        """ 创建随机会议,主持人入会 """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        client = self.loginAndAccessIn2(self.users[0])
        re = self.createRandomRoom(client)
        self.joinRoom(client, room_id=re[1]['roomId'])

    def test_create_and_join2(self):
        """ 创建随机会议,主持人入会，其他与会者也一起入会 """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        client = self.loginAndAccessIn2(self.users[0])
        re = self.createRandomRoom(client)
        room_id = re[1]['roomId']
        self.joinRoom(client, room_id)
        client.collecting_notify()  # 主动收集通知

        # 第二人入会
        client2 = self.loginAndAccessIn2(self.users[1])
        self.joinRoom(client2, room_id)

        time.sleep(2)
        notifys = client.search_notify_list('participantJoined')
        metadata = notifys[0]['params']['metadata']
        self.assertTrue(client2.uuid in metadata, '第二人加入会议的通知不正确,account不匹配')
        self.assertTrue('PUBLISHER' in metadata, '第二人加入会议的通知不正确,role不匹配')

        notifys = client.search_notify_list('partOrderOrRoleChangeNotify')
        self.assertTrue(len(notifys) > 0, '没有收到 partOrderOrRoleChangeNotify 回调')

    def test_create_join_leave(self):
        """ 创建个人会议,主持人入会，其他与会者也一起入会,然后离会 """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        self.test_create_and_join2()
        self.moderatorClient.clear_notify()
        time.sleep(0.5)

        room_id = self.moderatorClient.room_id
        self.leaveRoom(self.clients[1], room_id)

        self.assertTrue(self.moderatorClient.has_notify('participantLeft'), "没有收到 participantLeft 通知")
        self.leaveRoom(self.moderatorClient, room_id)

    def test_join_parts(self):
        """ 创建个人会议,主持人入会，获取与会者列表 """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        user = self.users[0]
        client = self.loginAndAccessIn(user['phone'], user['pwd'])
        self.createPersonalRoom(client)
        room_id = client.room_id
        # 主持人入会，然后拉列表
        self.joinRoom(client, room_id)
        client.ms = MeetingService(client, room_id)
        re = client.ms.get_participants('all')
        self.assertEqual(re[0], 0, msg=re[1])
        self.assertEqual(re[1]['participantList'][0]['role'], 'MODERATOR', '校验角色错误')
        self.assertEqual(re[1]['participantList'][0]['account'], client.uuid, '校验account错误')
        self.assertEqual(re[1]['participantList'][0]['order'], 0, '校验order错误')

        # 第二个人入会
        user2 = self.users[1]
        client2 = self.loginAndAccessIn(user2['phone'], user2['pwd'])
        self.joinRoom(client2, room_id)

        re = client.ms.get_participants('all')
        self.assertEqual(re[0], 0, msg=re[1])
        self.assertEqual(len(re[1]['participantList']), 2, '人数不对')

    def test_join_order(self):
        """ 创建个人会议,与会者先入会6个，主持人在入会，把最后一人挤下墙，发生角色改变 """
        user = self.users[0]
        client = self.loginAndAccessIn(user['phone'], user['pwd'])
        self.createPersonalRoom(client)
        room_id = client.room_id

        # 与会者入会6个
        for i in range(1, 7):
            user2 = self.users[i]
            client2 = self.loginAndAccessIn(user2['phone'], user2['pwd'])
            re = self.joinRoom(client2, room_id)
            # joinRoom 返回值校验
            self.assertEqual(re[0], 0, msg=re[1])
            room_info = re[1]['roomInfo']
            self.assertEqual(room_info['partSize'], i, '参数人数校验失败')
            self.assertEqual(room_info['order'], i - 1, '校验order错误')
            self.assertIsNotNone(room_info['sfuPublisherThreshold'], 'sfuPublisherThreshold is None')
            self.assertIsNotNone(room_info['mcuThreshold'], 'mcuThreshold is None')
            self.assertIsNotNone(room_info['unMcuThreshold'], 'unMcuThreshold is None')
            # joinRoom 返回值校验

            # 校验 getParticipants 接口
            client2.ms = MeetingService(client2, room_id)
            re = client2.ms.get_participants('all')
            self.assertEqual(re[0], 0, msg=re[1])
            self.assertEqual(len(re[1]['participantList']), i, '人数不对')
            part = self.getParticipant(re[1], user2['uuid'])
            self.assertEqual(part['role'], 'PUBLISHER', '校验角色错误')
            self.assertEqual(part['account'], client2.uuid, '校验account错误')
            self.assertEqual(part['order'], i - 1, '校验order错误')
            client2.collecting_notify()
            time.sleep(0.1)
            # 校验 getParticipants 接口

        # # 主持人入会
        self.joinRoom(client, room_id)
        client.ms = MeetingService(client, room_id)
        re = client.ms.get_participants('all')
        self.assertEqual(re[0], 0, msg=re[1])
        self.assertEqual(len(re[1]['participantList']), 7, '人数不对')
        part = self.getParticipant(re[1], user['uuid'])
        self.assertIsNotNone(part, '没有从与会者列表中找到需要的与会者')
        self.assertEqual(part['role'], 'MODERATOR', '校验角色错误')
        self.assertEqual(part['account'], client.uuid, '校验account错误')
        self.assertEqual(part['order'], 0, '校验order错误')

        # 第一个入会的与会者order会变为1
        # part = self.getParticipant(re[1], user2['uuid'])
        # self.assertEqual(part['order'], 1, '校验order错误')
        #
        # print(client2.search_notify_list('partOrderOrRoleChangeNotify'))
        # time.sleep(2)

    def test_join_not_exist(self):
        """测试加入不存在的会议
        描述：测试加入不存在的会议
        测试目的：测试加入不存在的会议
        测试过程: 1、登录
                2、加入不存在的会议
        结果期望： 返回 13001 会议不存在 错误
        """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        client = self.loginAndAccessIn2(self.users[0])
        re = client.joinRoom('1234567890')
        self.assertEqual(re[0], 13001, '测试加入不存在的会议 错误')
        pass

    def test_join_2(self):
        """ 创建个人会议, 连续入会2次"""
        # todo
        pass

    def test_join_get_participant(self):
        """ 创建个人会议, 测试get_participants接口 """
        # 主持人入会
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        moderator = self.users[0]
        moderator_client = self.loginAndAccessIn(moderator['phone'], moderator['pwd'])
        re = self.createRandomRoom(moderator_client)
        room_id = re[1]['roomId']
        self.joinRoom(moderator_client, room_id)
        moderator_client.ms = MeetingService(moderator_client, room_id)

        # 第二人入会
        part_user = self.users[1]
        part_client = self.loginAndAccessIn(part_user['phone'], part_user['pwd'])
        self.joinRoom(part_client, room_id)

        re = moderator_client.ms.get_participants('publisher')
        for item in re[1]['participantList']:
            self.assertIn(item['role'], {'MODERATOR', 'PUBLISHER'}, ' get_participants publisher 返回的角色不对')

    def test_create_join_20(self):
        """加入会议，不离会，重复20次
        创建会议，加入会议，不离会，重复20次
        期望:每次加入新的会议都会把前一个会议推出
        """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        moderator = self.users[0]
        room_id = None
        moderator_client = None
        for i in range(0, 20):
            moderator_client = self.loginAndAccessIn(moderator['phone'], moderator['pwd'])
            re = self.createRandomRoom(moderator_client)
            room_id = re[1]['roomId']
            self.joinRoom(moderator_client, room_id)
            time.sleep(0.1)
        self.leaveRoom(moderator_client, room_id)

    @unittest2.skipIf(sys.modules.get('fast_test'), '跳过耗时用例')
    def test_reconnect(self):
        """重连入会
       描述：重连测试
       测试目的：重连入会的表现
       测试过程: 1、创建会议，主持人入会，与会者入会，
                2、另启动一个客户端(不同udid)，让与会者重连入会
                3、再启动一个客户端(相同的udid)，让与会者重连入会
       结果期望： 步骤2会被强制端口链接，步骤3不会
       注意点：因为分布式，链接会被随机分配，需要多次测试覆盖多个信令
       """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        moderator_client, room_id = self.loginAndAccessInAndCreateAndJoin(self.users[0])
        part_client_reconnect, re = self.loginAndAccessInAndJoin(self.users[1], room_id)
        last_udid = ''
        for i in range(1, 10):
            part_client_pre = part_client_reconnect
            logger.info(f'{self.users[1]["uuid"]} 不同udid重连')
            part_client_pre.collecting_notify()
            last_udid = 'udid_qweqwe' + str(i)
            part_client_reconnect = self.loginAndAccessIn2(self.users[1], udid=last_udid)
            self.joinRoom(part_client_reconnect, room_id, isReconnected=True)
            time.sleep(2)
            evicted_notify = part_client_pre.search_notify_list('participantEvicted')[0]
            self.assertEqual(evicted_notify['params']['reason'], 'forceDisconnectByServer', '踢人原因不正确')
            self.assertEqual(evicted_notify['params']['uuid'], part_client_pre.uuid, '被踢的uuid不对')
            self.assertTrue(part_client_pre.is_close(), '前一个客户端因被强制关闭链接')

        for i in range(0, 10):
            part_client_pre = part_client_reconnect
            part_client_pre.collecting_notify()
            logger.info(f'{self.users[1]["uuid"]} 相同udid重连')
            part_client_reconnect = self.loginAndAccessIn2(self.users[1], udid=last_udid)
            self.joinRoom(part_client_reconnect, room_id, isReconnected=True)
            time.sleep(2)
            evicted_notify = part_client_pre.search_notify_list('participantEvicted')[0]
            self.assertEqual(evicted_notify['params']['reason'], 'reconnect', '踢人原因不正确')
            self.assertEqual(evicted_notify['params']['uuid'], part_client_pre.uuid, '被踢的uuid不对')
            self.assertFalse(part_client_pre.is_close(), '不强制关闭链接')

    def test_reconnect_moderator(self):
        """测试主持人重连入会
        描述：测试主持人重连入会
        测试目的：测试主持人重连入会
        测试过程: 主持人入会，然后主持人重连入会
        结果期望：主持人2次都正常入会
        """
        logger.info('测试主持人重连入会')
        moderator_client, room_id = self.loginAndAccessInAndCreateAndJoin(self.users[0])
        self.joinRoom(moderator_client, room_id, isReconnected=True)
        pass

    def test_concurrent_join(self):
        """测试并发入会
        描述：测试并发入会
        测试目的：主要为了校验在并发的状态下，信令的角色和order是否正确
        测试过程: 主持人创建一个会议，之后起10-20个客户端。在同一个时间入会。
        结果期望：在并发期间主持人请求到与会者列表需正确，主持人收到的入会回调需正确。验证项目：角色和order的连续性。
        """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        moderator_client, room_id = self.loginAndAccessInAndCreateAndJoin(self.users[0])
        clients = []
        self.barrier = False  # 与会者栅栏，让线程能同一个时间执行。
        self.finish = False  # True后让主持人线程停止
        for i in range(1, 15):
            client = self.loginAndAccessIn2(self.users[i])
            clients.append(client)

        for client in clients:
            t = threading.Thread(target=self.join, args=(client, room_id))
            t.setDaemon(True)
            t.start()

        threading.Thread(target=self.moderator_check_parts, args=(moderator_client, room_id), daemon=True).start()
        time.sleep(0.3)
        logger.info('测试账号已登录，准备并发入会')
        self.barrier = True
        time.sleep(2)
        self.finish = True
        time.sleep(2)

    cond = sys.modules.get('fast_test')

    @unittest2.skipIf(sys.modules.get('fast_test'), '跳过耗时用例')
    def test_force_disconnected(self):
        """测试强制掉线
        描述：测试强制掉线
        测试目的：掉线后，会保持2分钟与会状态，2分钟后被踢出会议，并且释放发言状态和分享位置
        测试过程: 1、主持人创建会议，
                2、第1个人入会，发言+分享。
                3、第2人入会，第1人掉线
                4、等待60秒，第3人入会
                5、继续等待60秒，第4人入会
        结果期望：step3:第2人可以看到发言者和分享，
                step4:第3人可以看到发言者和分享，
                step5:掉线的会被踢出会议，所以第4人入会看不到言者和分享
        """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        if sys.modules.get('fast_test'):
            return self.skip('跳过耗时用例')
        moderator_client, room_id = self.loginAndAccessInAndCreateAndJoin(self.users[0])
        moderator_client.ms = MeetingService(moderator_client, room_id)

        part_client, re = self.loginAndAccessInAndJoin(self.users[1], room_id)
        # step 2
        logger.info("step 2")
        speaker_sharing_uuid = part_client.uuid
        moderator_client.ms.set_roll_call(part_client.uuid)
        part_client.request('applyShare', {'targetId': part_client.uuid})
        logger.info('stop ping pong')
        part_client.close_ping_pong()

        # step 3
        client, re = self.loginAndAccessInAndJoin(self.users[2], room_id)
        self.assertEqual(re[1]['roomInfo']['sharingUuid'], speaker_sharing_uuid, '分享者有问题')
        self.assertEqual(re[1]['roomInfo']['speakerUuid'], speaker_sharing_uuid, '发言者有问题')

        # step 4
        time.sleep(60)
        logger.info('step 4')
        client, re = self.loginAndAccessInAndJoin(self.users[3], room_id)
        self.assertEqual(re[1]['roomInfo']['sharingUuid'], speaker_sharing_uuid, '分享者有问题')
        self.assertEqual(re[1]['roomInfo']['speakerUuid'], speaker_sharing_uuid, '发言者有问题')

        # step 5
        time.sleep(100)
        logger.info("step 5")
        client, re = self.loginAndAccessInAndJoin(self.users[5], room_id)
        self.assertEqual(re[1]['roomInfo']['sharingUuid'], "", '分享者有问题')
        self.assertEqual(re[1]['roomInfo']['speakerUuid'], "", '发言者有问题')

    def join(self, client, room_id):
        """ 与会者并发入会 """
        while (not self.barrier):
            pass
        t1 = time.time()
        self.joinRoom(client, room_id)
        t2 = time.time()
        logger.info(f'{client.uuid} 入会用时 {t2 - t1}')

    def moderator_check_parts(self, client, room_id):
        """主持人线程，循环请求getParticipants，并验证"""
        while (not self.finish):
            client.ms = MeetingService(client, room_id)
            re = client.ms.get_participants("all")
            self.assertEqual(re[0], 0)
            list = re[1]['participantList']
            # 从order 0开始匹配每个part的位置，同时验证角色是否正确
            for i in range(0, len(list)):
                flag = False  # 是否有这个order的part
                for part in list:
                    if part['order'] == i:
                        flag = True
                        if i == 0:
                            self.assertEqual(part['role'], 'MODERATOR', '角色不对' + str(part))
                        elif i < self.sfuLimit:
                            self.assertEqual(part['role'], 'PUBLISHER', '角色不对' + str(part))
                        else:
                            self.assertEqual(part['role'], 'SUBSCRIBER', '角色不对' + str(part))
                self.assertTrue(flag, '没有找到 order ' + str(i) + ' 的与会者')


if __name__ == '__main__':
    unittest2.main()
