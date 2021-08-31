import time

import json
import os
import test
import unittest2
from test.service.services import  MeetingService
from loguru import logger


class TestStringMethods(test.MyTestCase):



    def tearDown(self):
        """ 自动释放资源 """
        time.sleep(1)
        for client in self.clients:
            client.safeOut()

    def test_create_and_join(self):
        """  """
        # 与会者个数
        part_size = 10
        # 入会完成后等待时长 秒
        timeout = 2000

        # 主持人入会
        user = self.users[0]
        moderator_client = self.loginAndAccessIn(user['phone'], user['pwd'])
        re = self.createRandomRoom(moderator_client)
        room_id = re[1]['roomId']
        logger.info('创建会议 ' + room_id)
        self.joinRoom(moderator_client, room_id)
        logger.info(moderator_client.uuid + ' 加入会议 ' + room_id)
        moderator_client.ms = MeetingService(moderator_client, room_id)

        clients = []
        for i in range(1, part_size):
            part_user = self.users[i]
            part_client = self.loginAndAccessIn(part_user['phone'], part_user['pwd'])
            clients.append(part_client)
            self.joinRoom(part_client, room_id)
            logger.info(part_client.uuid + ' 加入会议 ' + room_id)
            time.sleep(0.3)
        time.sleep(timeout)

        logger.info('时间到，陆续推出会议并关闭会议')
        for c in clients:
            self.leaveRoom(c, room_id)
            time.sleep(0.3)
        logger.info('推出')


if __name__ == '__main__':
    unittest2.main()
