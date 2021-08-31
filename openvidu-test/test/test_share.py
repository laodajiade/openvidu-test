import sys
import time

import unittest2
from loguru import logger

import test
from test.service.services import MeetingService


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

    def test_share_order(self):
        """ 测试分享后order变化
            期望：分享者顺序变为order1
         """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        moderator = self.users[0]
        moderator_client, room_id = self.loginAndAccessInAndCreateAndJoin(moderator)

        part_client1, re = self.loginAndAccessInAndJoin(self.users[1], room_id)
        part_client2, re = self.loginAndAccessInAndJoin(self.users[2], room_id)

        time.sleep(1)
        moderator_client.collecting_notify()
        re = part_client2.request('applyShare', {'targetId': part_client2.uuid})
        self.assertEqual(re[0], 0, '申请共享失败 ' + str(re))
        notify = moderator_client.find_any_notify("partOrderOrRoleChangeNotify")
        for obj in notify['params']['updateParticipantsOrder']:
            if obj['uuid'] == part_client2.uuid:
                self.assertEqual(obj['order'], 1, '分享者的order不是1')

    def test_share_order_no_moderator(self):
        """ 测试在没有主持人情况下分享后order变化
            期望：分享者顺序变为order0
         """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        moderator = self.users[0]
        moderator_client, room_id = self.loginAndAccessInAndCreateAndJoin(moderator)

        part_client1, re = self.loginAndAccessInAndJoin(self.users[1], room_id)
        part_client2, re = self.loginAndAccessInAndJoin(self.users[2], room_id)
        moderator_client.leave_room(room_id)  # 主持人离会

        time.sleep(1)
        part_client1.collecting_notify()
        re = part_client2.request('applyShare', {'targetId': part_client2.uuid})
        self.assertEqual(re[0], 0, '申请共享失败 ' + str(re))
        notify = part_client1.find_any_notify("partOrderOrRoleChangeNotify")
        for obj in notify['params']['updateParticipantsOrder']:
            if obj['uuid'] == part_client2.uuid:
                self.assertEqual(obj['order'], 0, '分享者的order不是1')

    def test_speaker_sharing_order(self):
        """ 发言者分享不改变order
            期望：发言者分享不改变order
         """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        moderator = self.users[0]
        moderator_client, room_id = self.loginAndAccessInAndCreateAndJoin(moderator)

        part_client1, re = self.loginAndAccessInAndJoin(self.users[1], room_id)
        part_client2, re = self.loginAndAccessInAndJoin(self.users[2], room_id)
        moderator_client.ms = MeetingService(moderator_client, room_id)
        moderator_client.ms.set_roll_call(part_client2.uuid)  # 给予发言

        time.sleep(1)
        moderator_client.collecting_notify()
        re = part_client2.request('applyShare', {'targetId': part_client2.uuid})
        self.assertEqual(re[0], 0, '申请共享失败 ' + str(re))
        notifies = moderator_client.search_notify_list("partOrderOrRoleChangeNotify")
        self.assertEqual(len(notifies), 0, '收到了partOrderOrRoleChangeNotify，错误')


if __name__ == '__main__':
    unittest2.main()
