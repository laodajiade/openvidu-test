import sys
import time

import unittest2
from loguru import logger

import test
from test.service.services import MeetingService


class TestMCU(test.MyTestCase):
    """ 轮询相关 """

    def test_simple_polling(self):
        """ 简单轮询
        """
        moderator_client, room_id = self.loginAndAccessInAndCreateAndJoin(self.users[0])
        logger.info('强制开启MCU')
        self.set_mcu_mode(moderator_client)
        time.sleep(3)

        part_client, re = self.loginAndAccessInAndJoin(self.users[1], room_id)
        mix_stream_id = re[1]['roomInfo']['mixFlows'][0]['streamId']
        self.assertIsNotNone(mix_stream_id, '混流id不存在')

        logger.info('与会者拉mcu画面')
        re = part_client.subscribe_mcu(mix_stream_id)
        self.assertIsNotNone(re[1]['sdpAnswer'], '没有sdpAnswer，拉流失败')
        subscribeId = re[1]['subscribeId']
        time.sleep(2)
        logger.info('与会者停止拉mcu画面')
        re = part_client.unsubscribe_video(subscribeId)


    ############################################################

    def subscribe_mcu_stream(self, client, publish_id):
        re = client.subscribe_video(client.uuid, 'MAJOR', publish_id, stream_mode='MIX_MAJOR')
        self.assertEqual(re[0], 0, '拉流错误' + publish_id)
        stream_id = re[1]['subscribeId']  # 获取到stream_id
        self.assertIsNotNone(stream_id, ' 拉流没有subscribeId')
        self.assertIsNotNone(re[1]['sdpAnswer'], '推流没有 sdpAnswer')
        time.sleep(1)
        # self.valid_publish_video(client, stream_id)
        return stream_id

    def valid_publish_video(self, client, stream_id):
        # 检查媒体下发的ice candidate
        ice_candidate_notify = client.search_notify_list('iceCandidate')
        for notify in ice_candidate_notify:
            self.assertEqual(notify['params']['endpointName'], stream_id, 'iceCandidate 错误')

        participant_published = client.search_notify_list('participantPublished')
        self.assertEqual(participant_published[0]['params']['streams'][0]['publishId'], stream_id,
                         'participantPublished 错误')

    def on_ice_candidate(self, client, stream_id):
        re = client.on_ice_candidate(stream_id)
        self.assertEqual(re[0], 0)

    def set_mcu_mode(self, client):
        client.request('setConferenceMode', {'mode': 'mcu', 'pwd': 'sudi123', 'roomId': client.room_id})
        logger.info('强制开启MCU')
        time.sleep(1)

    def publish_and_subscribe(self):
        """ 创建会议, 主持人推主流，与会者1拉主流 """
        result = {}
        # 主持人入会
        moderator = self.users[0]
        moderator_client = self.loginAndAccessIn(moderator['phone'], moderator['pwd'])
        re = self.createRandomRoom(moderator_client)
        room_id = re[1]['roomId']
        self.joinRoom(moderator_client, room_id)
        moderator_client.ms = MeetingService(moderator_client, room_id)
        moderator_client.collecting_notify()

        # 主持人入会
        part = self.users[1]
        part_client = self.loginAndAccessIn(part['phone'], part['pwd'])
        self.joinRoom(part_client, room_id)
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
