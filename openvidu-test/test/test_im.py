import sys

import unittest2
from loguru import logger

import test


class TestLogin(test.MyTestCase):
    """ IM相关"""

    def test_error(self):
        # self.assertEqual(1, 0, "hhhhhhh")
        pass

    def test_im_send(self):
        """ 测试IM发消息能力
        测试目的：测试IM发消息能力
        测试过程: 1、
        结果期望：
        """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        moderator_client = self.loginAndAccessIn2(self.users[0])
        result = self.createRandomRoom(moderator_client)
        room_id = result[1]['roomId']
        result = self.joinRoom(moderator_client, room_id)
        ruid = result[1]['roomInfo']['ruid']
        result = self.send_msg(moderator_client, room_id, ruid, 'Hello world')
        self.assertEqual(result[0], 0)

    def test_im_history(self):
        """ 测试IM查询消息历史
        测试目的：测试IM查询消息历史
        测试过程: 1、
        结果期望：
        """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        moderator_client = self.loginAndAccessIn2(self.users[0])
        result = self.createRandomRoom(moderator_client)
        room_id = result[1]['roomId']
        result = self.joinRoom(moderator_client, room_id)
        ruid = result[1]['roomInfo']['ruid']
        result = self.send_msg(moderator_client, room_id, ruid, 'Hello world')
        self.assertEqual(result[0], 0)

        result = moderator_client.request("getMsgHistory", {'time': 0, 'ruid': ruid, 'limit': 10, 'reverse': 1})
        self.assertEqual(result[0], 0)
        self.assertIsNotNone(result[1]['list'][0]['sender']['username'], '用户名不存在')

    ####################################################################################################################
    ####################################################################################################################

    def send_msg(self, client, room_id, ruid, content):
        return client.request("sendMsg", {
            "clientMsgId": "999999",
            "ruid": ruid,
            "roomId": room_id,
            "timestamp": 0,
            "msgType": 0,
            "resendFlag": 0,
            "operate": 1,
            "reciverAccount": [
                ""
            ],
            "atAccount": [],
            "senderAccount": "80100201367",
            "content": content,
            "ext": "",
            "sessionId": "011206-3387b4a0"
        })


if __name__ == '__main__':
    unittest2.main()
