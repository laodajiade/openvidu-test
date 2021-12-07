import sys
import time

import unittest2
from loguru import logger

import test
from test.service.services import MeetingService


class TestInvite(test.MyTestCase):
    """邀请的相关测试 """

    def test_get_invite_info(self):
        """ 查询邀请情况 """
        moderator = self.users[0]
        moderator_client = self.loginAndAccessIn(moderator['phone'], moderator['pwd'])
        re = self.createRandomRoom(moderator_client)
        room_id = re[1]['roomId']
        self.joinRoom(moderator_client, room_id)
        moderator_client.ms = MeetingService(moderator_client, room_id)

        re = moderator_client.request('getInviteInfo', {'roomId': room_id})
        self.assertEqual(re[0], 0, 'get_invite_info error')
        self.assertIsNotNone(re[1]['inviteUrl'], 'inviteUrl 空')

    def test_invite(self):
        """测试被邀请的人是否能收到invite
        描述：主持人入会后邀请
        测试目的：测试被邀请的人是否能收到invite
        测试过程: 1、主持人创建会议入会
                2、再登录2个被邀请人
                3、主持人邀请
        结果期望：step3 能收到主持人的邀请通知
        """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        moderator_client, room_id = self.loginAndAccessInAndCreateAndJoin(self.users[0])
        moderator_client.ms = MeetingService(moderator_client, room_id)
        logger.info('step 2')
        part_client1 = self.loginAndAccessIn2(self.users[1])
        part_client2 = self.loginAndAccessIn2(self.users[2])
        part_client1.collecting_notify()
        part_client2.collecting_notify()
        logger.info('step 3')
        re = moderator_client.ms.invite_participant([part_client1.uuid, part_client2.uuid])
        self.assertEqual(re[0], 0, '邀请失败')
        time.sleep(10)  # 期间会收到多次邀请
        notifies = part_client1.search_notify_list('inviteParticipant')
        self.assertTrue(len(notifies) > 1, '应至少需要2次邀请')
        for notify in notifies:
            self.assertEqual(notify['params']['targetId'], part_client1.uuid, '被邀请人的uuid不正确')
        notifies = part_client2.search_notify_list('inviteParticipant')
        self.assertTrue(len(notifies) > 1, '应至少需要2次邀请')
        for notify in notifies:
            self.assertEqual(notify['params']['targetId'], part_client2.uuid, '被邀请人的uuid不正确')

    @unittest2.skipIf(sys.modules.get('fast_test'), '跳过耗时用例')
    def test_invite_ring(self):
        """测试被邀请的人响应ring后是否还会推送invite通知
        描述：测试被邀请人响应ring
        测试目的：测试被邀请的人响应ring后是否还会推送invite通知
        测试过程: 1、主持人创建会议入会
                2、再登录10个被邀请人
                3、主持人邀请
                4、被邀请人响应ring
        结果期望：1、step3 能收到主持人的邀请通知
                2、step4 后不会在有invite通知下发
        注意点：需要测试多人，确保在分布式情况下能被分配到不同的机器下
        """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        moderator_client, room_id = self.loginAndAccessInAndCreateAndJoin(self.users[0])
        moderator_client.ms = MeetingService(moderator_client, room_id)
        logger.info('step 2')
        clients = []
        invite_ids = []
        for i in range(1, 11):
            part_client = self.loginAndAccessIn2(self.users[i])
            part_client.collecting_notify()
            clients.append(part_client)
            invite_ids.append(part_client.uuid)

        logger.info('step 3')
        re = moderator_client.ms.invite_participant(invite_ids)
        self.assertEqual(re[0], 0, '邀请失败')
        time.sleep(10)  # 期间会收到多次邀请
        for part_client in clients:
            notifies = part_client.search_notify_list('inviteParticipant')
            self.assertTrue(len(notifies) > 1, '只收到0/1次邀请')
            for notify in notifies:
                self.assertEqual(notify['params']['targetId'], part_client.uuid, '被邀请人的uuid不正确')
            logger.info('step 4')
            target_id = notifies[0]['params']['sourceId']
            re = part_client.request("ringring", {'sourceId': part_client.uuid, 'targetId': target_id})
            self.assertEqual(re[0], 0, 'ringring失败')
            part_client.clear_notify()
        time.sleep(10)
        for part_client in clients:
            notifies = part_client.search_notify_list('inviteParticipant')
            self.assertEqual(len(notifies), 0, '收到了错误的inviteParticipant的通知')

    def test_refuse_invite(self):
        """测试被邀请的人拒绝邀请后是否还会收到邀请
        描述：测试拒绝邀请
        测试目的：测试被邀请的人拒绝邀请后是否还会
        测试过程: 1、主持人创建会议入会
                2、
                3、
        结果期望：
        """
        pass

    def test_get_invite_info(self):
        """ 测试主持人能在外部获取邀请信息
        描述：测试获取邀请信息
        测试目的：测试主持人身份可以获取邀请信息
        测试过程: 1、主持人创建会议入会
                2、其他人入会
                3、主持人离会并getInviteInfo
        结果期望：
        """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        moderator_client, room_id = self.loginAndAccessInAndCreateAndJoin(self.users[0])
        part_client, result = self.loginAndAccessInAndJoin(self.users[1], room_id)
        moderator_client.leave_room(room_id)
        result = moderator_client.request("getInviteInfo", {"roomId": room_id})
        self.assertEqual(result[0], 0)

    def test_get_invite_info_by_reconnect(self):
        """ 测试重连后从外部获取邀请信息
        描述：测试获取邀请信息
        测试目的：测试主持人身份可以获取邀请信息
        测试过程: 1、主持人创建会议入会
                2、其他人入会
                3、主持人重连并getInviteInfo
                4、分布式下可能存在问题，循环20次。
        结果期望：
        """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        moderator_client = self.loginAndAccessIn2(self.users[0], udid='123456')
        room_id = self.createRandomRoom(moderator_client)[1]['roomId']
        result = self.joinRoom(moderator_client, room_id)

        part_client, result = self.loginAndAccessInAndJoin(self.users[1], room_id)
        logger.info('主持人重连并getInviteInfo')
        for i in range(0, 20):
            moderator_client = self.loginAndAccessIn2(self.users[0], udid='123456')
            result = moderator_client.request("getInviteInfo", {"roomId": room_id})
            self.assertEqual(result[0], 0)


if __name__ == '__main__':
    unittest2.main()
