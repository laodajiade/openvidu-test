import json
import sys
import time
from loguru import logger
import unittest2

import test


class TestShare(test.MyTestCase):
    """ 分享相关 """


    def test_share(self):
        """ 创建随机会议，入会2人，然后分享 """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        moderator = self.users[0]
        moderator_client = self.loginAndAccessIn(moderator['phone'], moderator['pwd'])
        re = self.createRandomRoom(moderator_client)
        room_id = re[1]['roomId']
        self.joinRoom(moderator_client, room_id)

        part1 = self.users[1]
        part1_client = self.loginAndAccessIn(part1['phone'], part1['pwd'])
        self.joinRoom(part1_client, room_id)

        part2 = self.users[2]
        part2_client = self.loginAndAccessIn(part2['phone'], part2['pwd'])
        self.joinRoom(part2_client, room_id)

        # 第一位申请共享，给通过
        re = part1_client.request('applyShare', {'targetId': part1_client.uuid})
        self.assertEqual(re[0], 0, '申请共享失败 ' + str(re))

        # 第二位申请共享，不给通过
        re = part2_client.request('applyShare', {'targetId': part2_client.uuid})
        self.assertEqual(re[0], 13005, '返回值不是 共享已存在 ' + str(re))

        re = part1_client.request('endShare', {'targetId': part1_client.uuid})
        self.assertEqual(re[0], 0, '结束共享失败 ' + str(re))

        # 第二位申请共享，通过
        re = part2_client.request('applyShare', {'targetId': part2_client.uuid})
        self.assertEqual(re[0], 0, '申请共享失败 ' + str(re))

    def test_share_leave(self):
        """ 创建随机会议，入会1人，分享，离开会议
         期望：主持人收到结束共享通知
         """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        moderator = self.users[0]
        moderator_client = self.loginAndAccessIn(moderator['phone'], moderator['pwd'])
        re = self.createRandomRoom(moderator_client)
        room_id = re[1]['roomId']
        self.joinRoom(moderator_client, room_id)

        part1 = self.users[1]
        part1_client = self.loginAndAccessIn(part1['phone'], part1['pwd'])
        self.joinRoom(part1_client, room_id)

        # 第一位申请共享，给通过
        re = part1_client.request('applyShare', {'targetId': part1_client.uuid})
        self.assertEqual(re[0], 0, '申请共享失败 ' + str(re))
        moderator_client.collecting_notify()
        part1_client.leave_room(room_id)

        time.sleep(1)
        self.assertTrue(moderator_client.has_notify('endShareNotify'), ' 没有收到离会endShareNotify的通知')

if __name__ == '__main__':
    unittest2.main()
