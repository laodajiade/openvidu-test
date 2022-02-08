import sys
import time

import unittest2
from loguru import logger

import test
from test.service.services import MeetingService


class TestCloseRoom(test.MyTestCase):
    """ 会议关闭相关用例 """

    def test_simple_close(self):
        """ 创建个人会议，入会，然后离会 """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        moderator = self.users[0]
        client = self.loginAndAccessIn(moderator['phone'], moderator['pwd'])
        re = self.createPersonalRoom(client)
        room_id = re[1]['roomId']
        self.joinRoom(client, room_id)
        time.sleep(0.3)

        re = client.close_room(room_id)
        self.assertEqual(re[0], 0, '关闭会议失败')

    def test_close_many_people(self):
        """ 创建会议，多人入会，不离会，直接关闭会议 """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        moderator = self.users[0]
        moderator_client = self.loginAndAccessIn(moderator['phone'], moderator['pwd'])
        re = self.createPersonalRoom(moderator_client)
        room_id = re[1]['roomId']
        self.joinRoom(moderator_client, room_id)

        user = self.users[1]
        client = self.loginAndAccessIn(user['phone'], user['pwd'])
        self.joinRoom(client, room_id)
        client.collecting_notify()

        re = moderator_client.close_room(room_id)
        self.assertEqual(re[0], 0, '关闭会议失败')
        time.sleep(0.2)
        self.assertIsNotNone(client.search_notify_list('closeRoomNotify'), '未收到 closeRoomNotify 通知')
        # 检查 getNotFinishedRoom 返回值，结果应为空
        client.ms = MeetingService(client, room_id)
        re = client.ms.get_not_finished_room()
        self.assertEqual(re[1], {}, ' getNotFinishedRoom 错误')

    def test_leave_close(self):
        """ 创建个人会议，入会，然后离会,再关闭会议室
        原因：客户端主持人会先离会再关会议室
        """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        moderator = self.users[0]
        moderator_client = self.loginAndAccessIn(moderator['phone'], moderator['pwd'])
        re = self.createRandomRoom(moderator_client)
        room_id = re[1]['roomId']
        self.joinRoom(moderator_client, room_id)
        self.leaveRoom(moderator_client, room_id)

        time.sleep(0.3)
        re = moderator_client.close_room(room_id)
        self.assertEqual(re[0], 13007, '关闭会议失败')

    def test_close_not_exist(self):
        """ 测试关闭不存在的会议
        测试目的：测试关闭不存在的会议
        测试过程: 1、登录
                2、关闭不存在的会议
        结果期望： 返回 13007 会议已关闭 错误
        """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        client = self.loginAndAccessIn2(self.users[0])
        re = client.close_room('1234567890')
        self.assertEqual(re[0], 13007, '测试关闭不存在的会议 错误')
        pass

    def test_close_get_not_finished_room(self):
        """ 主持人关闭会议后，与会者查询未完成会议
        测试目的：硬终端在收到closeRoom通知后，立即退出会议，然后查询一个getNotFinishedRoom，有时有未完成会议。
        测试过程: 1、主持人入会创建会议
                2、第二人加入会议
                3、主持人关闭会议
                4、与会者在收到closeRoom通知后，立即查询getNotFinishedRoom
        结果期望：getNotFinishedRoom结果为空
        """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        moderator_client, room_id = self.loginAndAccessInAndCreateAndJoin(self.users[0])
        part_client, re = self.loginAndAccessInAndJoin(self.users[1], room_id)

        part_client.ms = MeetingService(part_client, room_id)
        receive = []  # 用来接收 listen_close_room_notify 的结果
        part_client.listen_notify("closeRoomNotify", self.listen_close_room_notify, part_client, receive)

        moderator_client.close_room(room_id)
        time.sleep(4)
        self.assertNotEqual(len(receive), 0, 'listen_close_room_notify 没有正常执行')
        for re in receive:
            self.assertEqual(re[0], 0, '请求结果错误' + str(re))
            self.assertEqual(re[1], {}, 'get_not_finished_room 应是空的' + str(re))

    def listen_close_room_notify(self, result, *args):
        """
           收到 closeRoomNotify 通知后，立马查询一次 get
        """
        re = args[0].ms.get_not_finished_room()
        # self.assertEqual(re[1], {}, "查到了未完成会议") 这是异步的，异常抛不到主线程
        args[1].append(re)  # 把结果传回到主线程中判断，这样unittest可以捕获到错误异常。


if __name__ == '__main__':
    unittest2.main()
