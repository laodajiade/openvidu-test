import sys

import unittest2
from loguru import logger

import test
import time
from test.service.services import MeetingService


class TestReconnect(test.MyTestCase):
    """ 重连的相关测试  """

    def test_reconnect_not_finished(self):
        """在会议中，重连后getNotFinishedRoom查询,重连时可能重连到其他openvidu上，导致无结果
        测试目的：每次重连后的查询getNotFinishedRoom，都应该是正确的
        测试过程: 1、主持人入会创建会议
                2、第二人加入会议
                3、第二人重连40次，每次getNotFinishedRoom都需要有会议
        结果期望：第二人重连40次，每次getNotFinishedRoom都需要有会议
        """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        moderator_client, room_id = self.loginAndAccessInAndCreateAndJoin(self.users[0])
        part_client = self.loginAndAccessIn2(self.users[2], udid='123456')
        part_client.joinRoom(room_id)
        for i in range(0, 40):
            part_client = self.loginAndAccessIn2(self.users[2], udid='123456')
            part_client.ms = MeetingService(part_client, room_id)
            re = part_client.ms.get_not_finished_room()
            self.assertEqual(re[0], 0, '请求失败')
            self.assertNotEqual(re[1], {}, '没查询到会议')

    def test_reconnect_order(self):
        """ A B C入会后，B离会，C重连，验证C的order
        测试目的：每次重连后的查询getNotFinishedRoom，都应该是正确的
        测试过程: 1、主持人入会创建会议
                2、第二人加入会议
                3、第二人重连40次，每次getNotFinishedRoom都需要有会议
        结果期望：第二人重连40次，每次getNotFinishedRoom都需要有会议
        """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        moderator_client, room_id = self.loginAndAccessInAndCreateAndJoin(self.users[0])
        part_client1, re = self.loginAndAccessInAndJoin(self.users[1], room_id)
        part_client2 = self.loginAndAccessIn2(self.users[2], udid='123456')
        part_client2.joinRoom(room_id)

        logger.info('B 离会')
        part_client1.leave_room(room_id)
        logger.info('C 重连')
        part_client2 = self.loginAndAccessIn2(self.users[2], udid='123456')
        part_client2.joinRoom(room_id)
        moderator_client.collecting_notify()
        notify = moderator_client.find_any_notify('partOrderOrRoleChangeNotify')
        self.check_order_list(notify)


    def check_order_list(self, notify):
        """ 检查order 0-N 是不是连续的 """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        orders = notify['params']['updateParticipantsOrder']
        for i in range(0, len(orders)):
            flag = False
            for order in orders:
                if order['order'] == i:
                    flag = True
                    break
            self.assertTrue(flag, ' order ' + str(i) + ' 不存在 ' + str(notify))

if __name__ == '__main__':
    unittest2.main()
