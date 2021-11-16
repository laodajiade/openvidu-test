import sys

import unittest2
from loguru import logger

import test

try:
    import thread
except ImportError:
    import _thread as thread


class TestCreateRoom(test.MyTestCase):
    """ 固定会议室相关用例 """

    def tearDown(self):
        super(TestCreateRoom, self).tearDown()

    def test_join_room_by_short_id(self):
        """ 短号入会,通过并发测试分布式下短号的问题 """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        room_id = self.fixed_rooms[0]['roomId']
        short_id = self.fixed_rooms[0]['sortId']
        moderator_client = self.loginAndAccessIn2(self.users[0])
        result = moderator_client.createRoom(room_id, '固定会议室', room_id_type='fixed')
        self.assertEqual(result[0], 0)
        result = moderator_client.joinRoom(short_id)
        moderator_client.room_id = room_id
        self.assertEqual(result[0], 0)

        logger.info('批量入会，让负载均衡器尽量照顾到各个服务器')
        clients = self.batchJoinRoom(short_id, 1, 10)
        self.tearDown()

    def test_join_room_by_short_id_cycle_10(self):
        """ 短号入会,通过并发测试分布式下短号的问题,循环10次 """
        for i in range(0, 10):
            self.test_join_room_by_short_id()


if __name__ == '__main__':
    unittest2.main()
