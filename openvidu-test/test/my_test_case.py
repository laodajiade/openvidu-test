# -*- coding: UTF-8 -*-
import json
import os
import sys
import time
import unittest

from loguru import logger

from common.mock_client import SdClient


class MyTestCase(unittest.TestCase):
    sfuLimit = 9
    smart_mic_on_threshold = 6

    ######################### init #########################
    def setUp(self):
        """ 数据准备 """
        self.clients = []
        with open(os.path.abspath(__file__) + '/../resource/conf.json', 'r', encoding='UTF-8') as load_f:
            load_dict = json.load(load_f)
            conf_json = load_dict['default']
            use_evn = load_dict['use_evn']
            for k, v in load_dict[use_evn].items():
                conf_json[k] = v
        self.server_url = conf_json['server_url']
        self.users = conf_json['users']
        self.sips = conf_json['SIP']
        self.fixed_rooms = conf_json['fixed_rooms']
        # 耗时用例是否测试，本地快速跑通用例时可以跳过，在测试环境最好全用例测试
        self.fast = conf_json['fast']
        sys.modules['fast_test'] = self.fast

    def tearDown(self):
        """ 自动释放资源 """
        time.sleep(1)
        logger.info("#################### tear down ####################")
        for client in self.clients:
            client.safeOut()
        time.sleep(1)

    ######################### init #########################
    ######################### room #########################
    def getParticipant(self, parts, uuid):
        """ 从 getParticipants 的列表中找到需要的uuid """
        for part in parts['participantList']:
            if part['account'] == uuid:
                return part
        return None

    def createPersonalRoom(self, client):
        self.moderatorClient = client
        self.room_id = client.uuid
        re = client.createRoom(self.room_id, client.uuid + '的会议')
        self.assertEqual(re[0], 0, msg=re[1])
        logger.info('create room ' + re[1]['roomId'])
        return re

    def createRandomRoom(self, client):
        self.moderatorClient = client
        re = client.createRoom('', client.uuid + '的随机会议', 'random')
        self.assertEqual(re[0], 0, msg='create room error ' + str(re))
        logger.info('create room ' + re[1]['roomId'])
        return re

    def joinRoom(self, client, room_id, **kwargs):
        re = client.joinRoom(room_id, **kwargs)
        self.assertEqual(re[0], 0, msg=re[1])
        return re

    def batchJoinRoom(self, room_id, users_index_start, users_index_end):
        clients = []
        for i in range(users_index_start, users_index_end):
            part_user = self.users[i]
            part_client = self.loginAndAccessIn(part_user['phone'], part_user['pwd'])
            clients.append(part_client)
            self.joinRoom(part_client, room_id)
            time.sleep(0.1)
        return clients

    def leaveRoom(self, client, room_id):
        re = client.leave_room(room_id)
        self.assertEqual(re[0], 0, msg=re[1])

        # 登录

    def loginAndAccessIn2(self, user, **kwargs):
        return self.loginAndAccessIn(user['phone'], user['pwd'], **kwargs)

    def loginAndAccessIn(self, account, pwd, **kwargs):
        client = SdClient(account, pwd, self.server_url)
        re = client.loginAndAccessIn(**kwargs)
        self.assertEqual(re[0], 0)
        self.clients.append(client)
        return client

    def sipLoginAndAccessIn(self, account, pwd, **kwargs):
        client = SdClient(account, pwd, self.server_url)
        kwargs['type'] = 'S'
        kwargs['deviceModel'] = 'deviceModel'
        kwargs['accessType'] = 'terminal'
        re = client.loginAndAccessIn(**kwargs)
        self.assertEqual(re[0], 0)
        self.clients.append(client)
        return client

    def loginAndAccessInAndCreateAndJoin(self, user):
        moderator = user
        moderator_client = self.loginAndAccessIn(moderator['phone'], moderator['pwd'])
        re = self.createRandomRoom(moderator_client)
        room_id = re[1]['roomId']
        self.joinRoom(moderator_client, room_id)
        return moderator_client, room_id

    def loginAndAccessInAndJoin(self, user, room_id):
        client = self.loginAndAccessIn(user['phone'], user['pwd'])
        re = self.joinRoom(client, room_id)
        return client, re

    def search_client(self, clients, uuid):
        for c in clients:
            if c.uuid == uuid:
                return c
        return None

    ######################### room #########################
    ######################### stream #########################

    def publish_video(self, client, stream_type):
        client.collecting_notify()
        re = client.publish_video(stream_type)
        self.assertEqual(re[0], 0, '推流错误')
        stream_id = re[1]['publishId']  # 获取到stream_id
        self.assertIsNotNone(stream_id, '推流没有stream_id')
        self.assertIsNotNone(re[1]['sdpAnswer'], '推流没有 sdpAnswer')
        time.sleep(1)
        self.valid_publish_video(client, stream_id)
        return stream_id

    def valid_publish_video(self, client, stream_id):
        # 检查媒体下发的ice candidate
        ice_candidate_notify = client.search_notify_list('iceCandidate')
        for notify in ice_candidate_notify:
            self.assertEqual(notify['params']['endpointName'], stream_id, 'iceCandidate 错误')

        participant_published = client.search_notify_list('participantPublished')
        self.assertEqual(participant_published[0]['params']['streams'][0]['publishId'], stream_id,
                         'participantPublished 错误')

    def subscribe_video(self, client, uuid, stream_type, publish_id):
        re = client.subscribe_video(uuid, stream_type, publish_id)
        self.assertEqual(re[0], 0, '拉流错误' + publish_id)
        stream_id = re[1]['subscribeId']  # 获取到stream_id
        self.assertIsNotNone(stream_id, ' 拉流没有subscribeId')
        self.assertIsNotNone(re[1]['sdpAnswer'], '推流没有 sdpAnswer')
        time.sleep(1)
        return stream_id

    def set_sfu_publisher_threshold(self, client, cnt):
        """ 修改企业的墙上人数，调试接口"""
        client.request('devSetCorpInfo', {'sfuPublisherThreshold': cnt, 'pwd': 'sudi123'})

    ######################### stream #########################
