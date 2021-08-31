import time

import test
import unittest2
from loguru import logger

from test.service.services import MeetingService


class TestLeaveRoom(test.MyTestCase):
    """ 离会相关用例 """

    def test_simple_leave(self):
        """ 创建个人会议，入会，然后离会 """
        client = self.loginAndAccessIn(self.users[0]['phone'], self.users[0]['pwd'])
        re = self.createPersonalRoom(client)
        room_id = re[1]['roomId']
        self.joinRoom(client, room_id)
        time.sleep(0.3)

        client.ms = MeetingService(client, room_id)

        # 离会前 检查 getNotFinishedRoom 返回值
        re = client.ms.get_not_finished_room()
        self.assertEqual(re[0], 0)
        self.assertEqual(re[1]['roomId'], room_id)

        self.leaveRoom(client, room_id)

        # 离会后 检查 getNotFinishedRoom 返回值
        re = client.ms.get_not_finished_room()
        self.assertEqual(re[0], 0)
        self.assertEqual(re[1], {})

    def test_leave_random(self):
        """ 创建个人会议，入会，然后离会 """
        client = self.loginAndAccessIn("17010000003", "123456")
        re = self.createRandomRoom(client)
        room_id = re[1]['roomId']
        self.joinRoom(client, room_id)
        time.sleep(0.3)

        client.ms = MeetingService(client, room_id)

        # 离会前 检查 getNotFinishedRoom 返回值
        re = client.ms.get_not_finished_room()
        self.assertEqual(re[0], 0)
        self.assertEqual(re[1]['roomId'], room_id)

        self.leaveRoom(client, room_id)

        # 离会后 检查 getNotFinishedRoom 返回值
        re = client.ms.get_not_finished_room()
        self.assertEqual(re[0], 0)
        self.assertEqual(re[1], {})

    def test_leave_multiple(self):
        """ 创建随机会议,主持人入会,入会其他5个人，按入会顺序倒序退出 """
        # 主持人入会
        user = self.users[0]
        moderator_client = self.loginAndAccessIn(user['phone'], user['pwd'])
        re = self.createRandomRoom(moderator_client)
        room_id = re[1]['roomId']
        self.joinRoom(moderator_client, room_id)
        moderator_client.ms = MeetingService(moderator_client, room_id)

        clients = []
        part_size = 1
        for i in range(1, 10):
            part_user = self.users[i]
            part_client = self.loginAndAccessIn(part_user['phone'], part_user['pwd'])
            clients.append(part_client)
            self.joinRoom(part_client, room_id)
            part_size += 1
            time.sleep(0.1)
        clients.reverse()
        for c in clients:
            self.leaveRoom(c, room_id)
            part_size -= 1
            re = moderator_client.ms.get_participants('all')
            self.assertEqual(len(re[1]['participantList']), part_size, '会议剩余人数不正常')
            time.sleep(0.2)

    def test_leave_twice(self):
        """ 创建随机会议,主持人入会,第二个人入会，主持人离开会议，查看order """
        logger.info('创建随机会议,主持人入会,第二个人入会，主持人离开会议，查看order')
        user = self.users[0]
        moderator_client = self.loginAndAccessIn(user['phone'], user['pwd'])
        re = self.createRandomRoom(moderator_client)
        room_id = re[1]['roomId']
        self.joinRoom(moderator_client, room_id)
        moderator_client.ms = MeetingService(moderator_client, room_id)

        part_user = self.users[1]
        part_client = self.loginAndAccessIn(part_user['phone'], part_user['pwd'])
        self.joinRoom(part_client, room_id)
        part_client.collecting_notify()
        # 主持人离会
        self.leaveRoom(moderator_client, room_id)

        time.sleep(1)
        # 校验各种离会通知
        self.assertTrue(part_client.has_notify('participantLeft'), '缺少 participantLeft 通知')
        left_notify = part_client.search_notify_list('participantLeft')[0]
        self.assertEqual(left_notify['params']['uuid'], moderator_client.uuid)

        self.assertTrue(part_client.has_notify('partOrderOrRoleChangeNotify'), '缺少 partOrderOrRoleChangeNotify 通知')
        order_notify = part_client.search_notify_list('partOrderOrRoleChangeNotify')[0]
        self.assertEqual(order_notify['params']['updateParticipantsOrder'][0]['order'], 0)
        self.assertEqual(order_notify['params']['updateParticipantsOrder'][0]['uuid'], part_client.uuid,
                         '错误json ' + str(order_notify))

    def test_leave_cycle(self):
        """ 创建随机会议,主持人入会,第二个人重复多次入会离会，然后主持人重复入会离会 """
        # 主持人入会
        moderator = self.users[0]
        moderator_client = self.loginAndAccessIn(moderator['phone'], moderator['pwd'])
        re = self.createRandomRoom(moderator_client)
        room_id = re[1]['roomId']
        self.joinRoom(moderator_client, room_id)
        moderator_client.ms = MeetingService(moderator_client, room_id)

        part_user = self.users[1]
        part_size = 1
        part_client = self.loginAndAccessIn(part_user['phone'], part_user['pwd'])
        for i in range(0, 10):
            self.joinRoom(part_client, room_id)
            time.sleep(0.3)
            part_size += 1
            re = moderator_client.ms.get_participants('all')
            self.assertEqual(len(re[1]['participantList']), part_size, '会议剩余人数不正常')
            # 检查排序
            part_list = re[1]['participantList']
            for part in part_list:
                if part['account'] == moderator['uuid']:
                    self.assertEqual(part['order'], 0, " order不正确" + str(re))
                if part['account'] == part_user['uuid']:
                    self.assertEqual(part['order'], 1, " order不正确" + str(re))
            self.leaveRoom(part_client, room_id)
            part_size -= 1
            re = moderator_client.ms.get_participants('all')
            self.assertEqual(len(re[1]['participantList']), part_size, '会议剩余人数不正常')
            time.sleep(0.3)

        self.joinRoom(part_client, room_id)
        part_client.ms = MeetingService(part_client, room_id)
        self.leaveRoom(moderator_client, room_id)
        for i in range(0, 6):
            self.joinRoom(moderator_client, room_id)
            part_size += 1
            time.sleep(0.3)
            re = part_client.ms.get_participants('all')
            self.assertEqual(len(re[1]['participantList']), part_size, '会议剩余人数不正常')
            # 检查排序
            part_list = re[1]['participantList']
            for part in part_list:
                if part['account'] == moderator['uuid']:
                    self.assertEqual(part['order'], 0, " order不正确" + str(re))
                if part['account'] == part_user['uuid']:
                    self.assertEqual(part['order'], 1, " order不正确" + str(re))

            self.leaveRoom(moderator_client, room_id)
            part_size -= 1
            re = part_client.ms.get_participants('all')
            self.assertEqual(len(re[1]['participantList']), part_size, '会议剩余人数不正常')
            time.sleep(0.3)
            # 检查 order
            self.assertEqual(re[1]['participantList'][0]['order'], 0, " order不正确" + str(re))
            self.assertEqual(re[1]['participantList'][0]['account'], part_user['uuid'], " account不正确" + str(re))

    def test_leave_role_change(self):
        """ 创建随机会议,主持人入会,入会其他9个人，墙上退出一个，最后一个人上墙，角色变化
        前提 sfuPublisher = 9
        创建随机会议,主持人入会,入会其他9个人，墙上退出一个，最后一个人上墙，角色变化
        """
        # 主持人入会
        user = self.users[0]
        moderator_client = self.loginAndAccessIn(user['phone'], user['pwd'])
        re = self.createRandomRoom(moderator_client)
        room_id = re[1]['roomId']
        self.joinRoom(moderator_client, room_id)
        moderator_client.ms = MeetingService(moderator_client, room_id)

        clients = []
        part_size = 1
        for i in range(1, 11):
            part_user = self.users[i]
            part_client = self.loginAndAccessIn(part_user['phone'], part_user['pwd'])
            clients.append(part_client)
            self.joinRoom(part_client, room_id)
            part_size += 1
            time.sleep(0.1)
        self.leaveRoom(clients[0], room_id)

    def test_evict(self):
        """ 踢人"""
        # 主持人入会
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

        params = {'uuid': part_user['uuid']}
        re = moderator_client.request("forceDisconnect", params)
        self.assertEqual(re[0], 0)

    def test_leave_reconnect(self):
        """ 重连离会 """
        # 主持人入会
        moderator = self.users[0]
        moderator_client = self.loginAndAccessIn(moderator['phone'], moderator['pwd'])
        re = self.createRandomRoom(moderator_client)
        room_id = re[1]['roomId']
        self.joinRoom(moderator_client, room_id)
        moderator_client.ms = MeetingService(moderator_client, room_id)

        # 在分布式情况下离会时不应该出现会议不存在。有偶现概率，所以试20次
        for i in range(0, 20):
            # 第二人入会
            part_user = self.users[1]
            part_client = self.loginAndAccessIn(part_user['phone'], part_user['pwd'])
            self.joinRoom(part_client, room_id)
            # 重新登录，模拟掉线
            part_client = self.loginAndAccessIn(part_user['phone'], part_user['pwd'])
            part_client.ms = MeetingService(part_client, room_id)
            re = part_client.ms.get_not_finished_room()
            self.assertIsNotNone(re[1], 'get_not_finished_room 错误')
            re = part_client.leave_room(room_id)
            self.assertEqual(re[0], 0, '离会错误')
            # time.sleep(2)
            logger.info(f'leave {i}')

    def test_last_part_leave(self):
        """ 测试最后一人离开会议，自动关闭会议 """
        logger.info('测试最后一人离开会议，自动关闭会议')
        # 主持人入会
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

        self.leaveRoom(moderator_client, room_id)
        self.leaveRoom(part_client, room_id)
        time.sleep(1)
        re = part_client.joinRoom(room_id)
        self.assertEqual(re[0], 13001, '结果应是会议不存在 ' + str(re))

    def test_leave_not_exist(self):
        """测试离开不存在的会议
        测试目的：测试离开不存在的会议
        测试过程: 1、登录
                2、离开不存在的会议
        结果期望： 返回 13001 会议不存在 错误
        """
        logger.info('测试加入不存在的会议')
        client = self.loginAndAccessIn2(self.users[0])
        re = client.leave_room('1234567890')
        self.assertEqual(re[0], 13001, '测试离开不存在的会议 错误')
        pass

    ###########################################################


if __name__ == '__main__':
    unittest2.main()
