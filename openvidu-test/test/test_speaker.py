# -*- coding: UTF-8 -*-
import sys
import time

import unittest2
from loguru import logger

import test
from test.service.services import MeetingService


class TestSpeaker(test.MyTestCase):
    """ 点名发言相关 """

    def test_speaker(self):
        """ 测试点名发言
        测试目的：测试墙上点名发言和墙下点名发言
        测试过程: 1、创建会议，入会16人
                2、点名墙上
                3、结束发言
                4、点名墙下
                5、结束发言
        结果期望： 墙上点名发言正常，墙下的点名发言会被修改角色和开启麦克风
        """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        logger.info('step 1')
        moderator_client, room_id = self.loginAndAccessInAndCreateAndJoin(self.users[0])
        clients = []
        for i in range(1, 16):
            client, re = self.loginAndAccessInAndJoin(self.users[i], room_id)
            self.assertEqual(re[0], 0, "入会失败")
            clients.append(client)
        logger.info('step 2')
        moderator_client.collecting_notify()
        roll_call_client = clients[1]
        moderator_client.ms = MeetingService(moderator_client, room_id)
        moderator_client.ms.set_roll_call(roll_call_client.uuid)
        time.sleep(2)
        notify = moderator_client.search_notify_list('setRollCallNotify')[0]
        self.assertEqual(notify['params']['targetId'], roll_call_client.uuid)

        logger.info('step 3')
        moderator_client.clear_notify()
        moderator_client.ms.end_roll_call(roll_call_client.uuid)
        time.sleep(2)
        notify = moderator_client.search_notify_list('endRollCallNotify')[0]
        self.assertEqual(notify['params']['targetId'], roll_call_client.uuid)

        logger.info('step 4')
        moderator_client.clear_notify()
        roll_call_client = clients[12]
        moderator_client.ms = MeetingService(moderator_client, room_id)
        moderator_client.ms.set_roll_call(roll_call_client.uuid)
        time.sleep(2)
        notify = moderator_client.search_notify_list('setRollCallNotify')[0]
        self.assertEqual(notify['params']['targetId'], roll_call_client.uuid)
        # 角色变更
        self.assertEqual(notify['params']['roleChange'][0]['uuid'], roll_call_client.uuid)
        self.assertEqual(notify['params']['roleChange'][0]['presentRole'], 'PUBLISHER')
        self.assertEqual(notify['params']['roleChange'][0]['originalRole'], 'SUBSCRIBER')
        # 麦克风变换
        self.assertEqual(notify['params']['setAudioStatus'][0]['targetId'], roll_call_client.uuid)
        self.assertEqual(notify['params']['setAudioStatus'][0]['status'], 'on')

        logger.info('step 5')
        moderator_client.clear_notify()
        moderator_client.ms.end_roll_call(roll_call_client.uuid)
        time.sleep(2)
        notify = moderator_client.search_notify_list('endRollCallNotify')[0]
        self.assertEqual(notify['params']['targetId'], roll_call_client.uuid)
        # 角色变更
        self.assertEqual(notify['params']['roleChange'][0]['uuid'], roll_call_client.uuid)
        self.assertEqual(notify['params']['roleChange'][0]['presentRole'], 'SUBSCRIBER', "角色错误")
        self.assertEqual(notify['params']['roleChange'][0]['originalRole'], 'PUBLISHER')
        # 麦克风变换
        self.assertEqual(notify['params']['setAudioStatus'][0]['targetId'], roll_call_client.uuid)
        self.assertEqual(notify['params']['setAudioStatus'][0]['status'], 'off')

    def test_replace_speaker(self):
        """ 替换发言
        测试目的：测试墙上和墙下相互替换发言
        测试过程: 1、创建会议，入会16人
                2、点名墙上
                3、用墙下的替换墙上的发言
                4、用墙上的替换墙下的发言
        结果期望： 墙下的点名发言会被修改角色和开启麦克风
        """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        logger.info('step 1')
        moderator_client, room_id = self.loginAndAccessInAndCreateAndJoin(self.users[0])
        clients = []
        for i in range(1, 16):
            client, re = self.loginAndAccessInAndJoin(self.users[i], room_id)
            self.assertEqual(re[0], 0, "入会失败")
            clients.append(client)
        logger.info('step 2')
        moderator_client.collecting_notify()
        up_client = clients[1]  # 墙上人
        moderator_client.ms = MeetingService(moderator_client, room_id)
        moderator_client.ms.set_roll_call(up_client.uuid)
        time.sleep(2)
        notify = moderator_client.search_notify_list('setRollCallNotify')[0]
        self.assertEqual(notify['params']['targetId'], up_client.uuid)

        logger.info('step 3')
        moderator_client.clear_notify()
        down_client = clients[12]  # 墙下人
        moderator_client.ms = MeetingService(moderator_client, room_id)
        moderator_client.ms.replace_roll_call(down_client.uuid, up_client.uuid)
        time.sleep(2)
        notify = moderator_client.search_notify_list('replaceRollCallNotify')[0]
        self.assertEqual(notify['params']['endTargetId'], up_client.uuid)
        self.assertEqual(notify['params']['startTargetId'], down_client.uuid)
        # 角色变更
        self.assertEqual(notify['params']['roleChange'][0]['uuid'], down_client.uuid)
        self.assertEqual(notify['params']['roleChange'][0]['presentRole'], 'PUBLISHER')
        self.assertEqual(notify['params']['roleChange'][0]['originalRole'], 'SUBSCRIBER')
        # 麦克风变换
        self.assertEqual(notify['params']['setAudioStatus'][0]['targetId'], down_client.uuid)
        self.assertEqual(notify['params']['setAudioStatus'][0]['status'], 'on')
        #
        logger.info('step 4')
        moderator_client.clear_notify()
        moderator_client.ms.replace_roll_call(up_client.uuid, down_client.uuid)
        time.sleep(2)
        notify = moderator_client.search_notify_list('replaceRollCallNotify')[0]
        self.assertEqual(notify['params']['endTargetId'], down_client.uuid)
        self.assertEqual(notify['params']['startTargetId'], up_client.uuid)
        # 角色变更
        self.assertEqual(notify['params']['roleChange'][0]['uuid'], down_client.uuid)
        self.assertEqual(notify['params']['roleChange'][0]['presentRole'], 'SUBSCRIBER', "角色错误")
        self.assertEqual(notify['params']['roleChange'][0]['originalRole'], 'PUBLISHER')
        # 麦克风变换
        self.assertEqual(notify['params']['setAudioStatus'][0]['targetId'], up_client.uuid)
        self.assertEqual(notify['params']['setAudioStatus'][0]['status'], 'on')
        self.assertEqual(notify['params']['setAudioStatus'][1]['targetId'], down_client.uuid)
        self.assertEqual(notify['params']['setAudioStatus'][1]['status'], 'off')

    def test_speaker_not_stop_sharing(self):
        """ 测试替换点名发言不结束共享
        测试目的：替换点名发言 存在结束共享的问题
        测试过程: 1、创建会议，入会3人
                2、点名order 1
                3、order 1分享
                4、order2 替换order1发言
        结果期望： order1的不会被结束分享
        """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        logger.info('step 1')
        moderator_client, room_id = self.loginAndAccessInAndCreateAndJoin(self.users[0])
        client1, re = self.loginAndAccessInAndJoin(self.users[7], room_id)
        client2, re = self.loginAndAccessInAndJoin(self.users[8], room_id)
        moderator_client.collecting_notify()
        moderator_client.ms = MeetingService(moderator_client, room_id)
        moderator_client.ms.set_roll_call(client1.uuid)

        notify = moderator_client.find_any_notify('setRollCallNotify')
        self.assertEqual(notify['params']['targetId'], client1.uuid)

        client1.request('applyShare', {'targetId': client1.uuid})
        self.assertEqual(re[0], 0, '分享失败')

        moderator_client.clear_notify()
        re = moderator_client.ms.replace_roll_call(client2.uuid, client1.uuid)
        self.assertEqual(re[0], 0, '替换发言失败')
        # 验证有没有结束分享
        self.assertFalse(moderator_client.has_notify_sync('endShareNotify'), '被错误停止了共享')

    def test_speaker_end(self):
        """ 测试结束发言，释放所有的流
        测试目的：替换点名发言 存在结束共享的问题
        测试过程: 1、创建会议，入会16人
                2、点名墙下，
                4、发言者推流
                3、结束点名
        结果期望： 结束点名 结束推流
        """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        logger.info('step 1')
        moderator_client, room_id = self.loginAndAccessInAndCreateAndJoin(self.users[0])

        clients = []
        for i in range(1, 16):
            client, re = self.loginAndAccessInAndJoin(self.users[i], room_id)
            self.assertEqual(re[0], 0, "入会失败")
            clients.append(client)

        client = clients[14]
        moderator_client.collecting_notify()
        moderator_client.ms = MeetingService(moderator_client, room_id)
        moderator_client.ms.set_roll_call(client.uuid)

        notify = moderator_client.find_any_notify('setRollCallNotify')
        self.assertEqual(notify['params']['targetId'], client.uuid)

        self.publish_video(client, 'MAJOR')

        moderator_client.clear_notify()
        re = moderator_client.ms.end_roll_call(client.uuid)
        self.assertEqual(re[0], 0, '结束发言失败')
        self.assertTrue(moderator_client.has_notify_sync('participantUnpublished'), '没有被停止推流')


if __name__ == '__main__':
    unittest2.main()
