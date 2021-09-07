import sys
import time

import unittest2
from loguru import logger

import test
from test.service.services import MeetingService


class TestStream(test.MyTestCase):
    """ 推拉流测试 """

    def test_multiple_room(self):
        """ 创建多个会议，每个会议都推流，测试kms的负载均衡策略 """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        moderator_client, room_id = self.loginAndAccessInAndCreateAndJoin(self.users[1])
        self.publish_video(moderator_client, 'MAJOR')
        moderator_client, room_id = self.loginAndAccessInAndCreateAndJoin(self.users[2])
        self.publish_video(moderator_client, 'MAJOR')
        moderator_client, room_id = self.loginAndAccessInAndCreateAndJoin(self.users[3])
        self.publish_video(moderator_client, 'MAJOR')
        moderator_client, room_id = self.loginAndAccessInAndCreateAndJoin(self.users[4])
        self.publish_video(moderator_client, 'MAJOR')
        moderator_client, room_id = self.loginAndAccessInAndCreateAndJoin(self.users[5])
        self.publish_video(moderator_client, 'MAJOR')
        moderator_client, room_id = self.loginAndAccessInAndCreateAndJoin(self.users[6])
        self.publish_video(moderator_client, 'MAJOR')
        moderator_client, room_id = self.loginAndAccessInAndCreateAndJoin(self.users[7])
        self.publish_video(moderator_client, 'MAJOR')

    def test_join_publish(self):
        """ 创建个人会议, 测试单人推流 """
        # 主持人入会
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        moderator = self.users[0]
        moderator_client = self.loginAndAccessIn(moderator['phone'], moderator['pwd'])
        re = self.createRandomRoom(moderator_client)
        room_id = re[1]['roomId']
        self.joinRoom(moderator_client, room_id)
        moderator_client.ms = MeetingService(moderator_client, room_id)
        moderator_client.collecting_notify()

        # ============= 推 MAJOR 流
        major_stream_id = self.publish_video(moderator_client, 'MAJOR')
        # 检查媒体下发的ice candidate
        ice_candidate_notify = moderator_client.search_notify_list('iceCandidate')
        for notify in ice_candidate_notify:
            self.assertEqual(notify['params']['endpointName'], major_stream_id, 'iceCandidate 错误')

        participant_published = moderator_client.search_notify_list('participantPublished')
        self.assertEqual(participant_published[0]['params']['streams'][0]['publishId'], major_stream_id,
                         'participantPublished 错误')
        # ============= 推 MAJOR 流

        moderator_client.ms = MeetingService(moderator_client, room_id)
        re = moderator_client.ms.get_participants('all')

        # ============= 推 MINOR 流
        time.sleep(2)
        moderator_client.clear_notify()
        minor_stream_id = self.publish_video(moderator_client, 'MINOR')
        # 检查媒体下发的ice candidate
        ice_candidate_notify = moderator_client.search_notify_list('iceCandidate')
        for notify in ice_candidate_notify:
            self.assertEqual(notify['params']['endpointName'], minor_stream_id, 'iceCandidate 错误,' + str(notify))
        participant_published = moderator_client.search_notify_list('participantPublished')
        self.assertEqual(participant_published[0]['params']['streams'][0]['publishId'], minor_stream_id,
                         'participantPublished 错误')

        re = moderator_client.ms.get_participants('all')
        print(re)

        # 查看 get_participants 中的流信息
        moderator_client.ms = MeetingService(moderator_client, room_id)
        re = moderator_client.ms.get_participants('all')
        streams = re[1]['participantList'][0]['streams']
        for stream in streams:
            if stream['streamType'] == 'MAJOR':
                self.assertEqual(stream['publishId'], major_stream_id, 'get_participants 错误' + str(re))
            elif stream['streamType'] == 'MINOR':
                self.assertEqual(stream['publishId'], minor_stream_id, 'get_participants 错误' + str(re))
            else:
                self.assertTrue(True, 'get_participants 错误' + str(re))

        # 打洞协议
        self.on_ice_candidate(moderator_client, major_stream_id)
        self.on_ice_candidate(moderator_client, major_stream_id)
        self.on_ice_candidate(moderator_client, minor_stream_id)
        self.on_ice_candidate(moderator_client, minor_stream_id)

    def test_receive(self):
        """ 创建会议, 主持人推主流，与会者1拉主流 """
        # 主持人入会
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        moderator = self.users[0]
        moderator_client = self.loginAndAccessIn(moderator['phone'], moderator['pwd'])
        re = self.createRandomRoom(moderator_client)
        room_id = re[1]['roomId']
        self.joinRoom(moderator_client, room_id)
        moderator_client.ms = MeetingService(moderator_client, room_id)
        moderator_client.collecting_notify()

        # 主持人入会
        part1 = self.users[1]
        part1_client = self.loginAndAccessIn(part1['phone'], part1['pwd'])
        self.joinRoom(part1_client, room_id)
        part1_client.ms = MeetingService(part1_client, room_id)
        part1_client.collecting_notify()

        publish_id = self.publish_video(moderator_client, 'MAJOR')  # 主持人推流
        subscribe_id = self.subscribe_video(part1_client, moderator_client.uuid, 'MAJOR', publish_id)  # 拉流
        time.sleep(2)
        self.on_ice_candidate(part1_client, subscribe_id)

        logger.info('停止拉流')
        time.sleep(2)
        re = part1_client.unsubscribe_video(subscribe_id)
        self.assertEqual(re[0], 0, "停止拉流失败" + str(re))

        re = part1_client.unsubscribe_video("unknown")
        self.assertEqual(re[0], 13088, "停止拉流失败" + str(re))

        # 停止推流
        time.sleep(2)
        logger.info('停止推流')
        part1_client.clear_notify()
        re = moderator_client.unpublish_video(publish_id)
        self.assertEqual(re[0], 0, "停止推流失败" + str(re))
        time.sleep(1)
        notifies = part1_client.search_notify_list('participantUnpublished')
        notify = notifies[0]
        self.assertEqual(notify['params']['streamType'], 'MAJOR', "停止推流通知失败" + str(notify))
        self.assertEqual(notify['params']['uuid'], moderator['uuid'], "停止推流通知失败" + str(notify))
        self.assertEqual(notify['params']['publishId'], publish_id, "停止推流通知失败" + str(notify))
        self.assertEqual(notify['params']['reason'], 'unpublish', "停止推流通知失败" + str(notify))

    def test_pause_and_resume(self):
        """ 测试暂停拉流和恢复拉流 """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        result = self.publish_and_subscribe()
        part_client = result['part_client']
        subscribe_id = result['subscribe_id']
        params = {}
        params['operation'] = 'off'
        streams = {'mediaType': 'video', 'subscribeId': subscribe_id}
        params['streams'] = [streams]
        re = part_client.request('pauseAndResumeStream', params)
        self.assertEqual(re[0], 0, '停止拉流失败' + str(re))

        time.sleep(1)
        params['operation'] = 'on'
        re = part_client.request('pauseAndResumeStream', params)
        self.assertEqual(re[0], 0, '恢复拉流失败' + str(re))

    def test_share(self):
        """ 推送分享流，非分享者不能推分享流 """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        result = self.publish_and_subscribe()
        part_client = result['part_client']
        subscribe_id = result['subscribe_id']

        # 推送SHARE流
        re = part_client.publish_video('SHARING')
        self.assertEqual(re[0], 13089, '没有分享角色也能分享' + str(re))

        params = {'targetId': part_client.uuid}
        re = part_client.request('applyShare', params)
        self.assertEqual(re[0], 0, '申请分享失败' + str(re))
        re = part_client.publish_video('SHARING')
        self.assertEqual(re[0], 0, '推送分享流失败' + str(re))

    def test_switch_voice_mode(self):
        """ 切换语音模式
        测试目的：切换语音模式不报错
        测试过程: 1、创建会议，入会2人
                2、主持人推流，与会者拉流
                3、与会者切换语音模式
                4、与会者切回视频模式
        结果期望： 与会者切换语音模式 不报错
        """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        result = self.publish_and_subscribe()
        logger.info('step 3')
        part_client = result['part_client']
        re = part_client.request('switchVoiceMode', {"operation": "on"})
        self.assertEqual(re[0], 0, '切换语音模式失败')

        moderator_client = result['moderator_client']
        notify = moderator_client.find_any_notify('switchVoiceModeNotify')
        self.assertEqual(notify['params']['operation'], 'on')
        self.assertEqual(notify['params']['uuid'], part_client.uuid, 'uuid错误')

        moderator_client.clear_notify()
        re = part_client.request('switchVoiceMode', {"operation": "off"})
        self.assertEqual(re[0], 0, '切换语音模式失败')
        notify = moderator_client.find_any_notify('switchVoiceModeNotify')
        self.assertEqual(notify['params']['operation'], 'off')
        self.assertEqual(notify['params']['uuid'], part_client.uuid, 'uuid错误')

    def a_test_sip_publish(self):
        """ SIP推流测试
        测试目的：SIP推流测试
        测试过程: 1、创建会议，主持人
                2、sip入会
                3、sip推流
        结果期望： SIP入会后自动开启MCU，SIP推拉流不出错
        """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        moderator_client, room_id = self.loginAndAccessInAndCreateAndJoin(self.users[0])

        sip_client = self.sipLoginAndAccessIn(self.sips[0]['account'], self.sips[0]['pwd'])
        self.joinRoom(sip_client, room_id)
        self.publish_video(sip_client, 'MAJOR')

    ############################################################

    def on_ice_candidate(self, client, stream_id):
        re = client.on_ice_candidate(stream_id)
        self.assertEqual(re[0], 0)

    def publish_and_subscribe(self):
        """
        创建会议, 主持人推主流，与会者1拉主流
        """
        result = {}
        # 主持人入会
        moderator = self.users[0]
        moderator_client, room_id = self.loginAndAccessInAndCreateAndJoin(moderator)
        moderator_client.ms = MeetingService(moderator_client, room_id)
        moderator_client.collecting_notify()

        # 主持人入会
        part = self.users[1]
        part_client, re = self.loginAndAccessInAndJoin(part, room_id)
        part_client.ms = MeetingService(part_client, room_id)
        part_client.collecting_notify()

        publish_id = self.publish_video(moderator_client, 'MAJOR')  # 主持人推流
        subscribe_id = self.subscribe_video(part_client, moderator_client.uuid, 'MAJOR', publish_id)  # 拉流
        time.sleep(1)

        result['moderator_client'] = moderator_client
        result['moderator'] = moderator
        result['part_client'] = part_client
        result['part'] = part
        result['room_id'] = room_id
        result['publish_id'] = publish_id
        result['subscribe_id'] = subscribe_id
        return result


if __name__ == '__main__':
    unittest2.main()
