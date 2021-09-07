import sys
import time

import unittest2
from loguru import logger

import test
from test.service.services import MeetingService


class TestOrder(test.MyTestCase):
    """ 会议排序 """

    def test_order(self):
        """ 创建随机会议，入会11人，墙上置顶一次，墙下置顶一次 """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        moderator = self.users[0]
        moderator_client = self.loginAndAccessIn(moderator['phone'], moderator['pwd'])
        re = self.createRandomRoom(moderator_client)
        room_id = re[1]['roomId']
        self.joinRoom(moderator_client, room_id)

        clients = self.batchJoinRoom(room_id, 1, 11)
        logger.info('全部入会完毕')
        moderator_client.collecting_notify()
        moderator_client.ms = MeetingService(moderator_client, room_id)
        moderator_client.ms.get_participants("all")

        params = {'orderedParts': [{'account': clients[6].uuid, 'order': 1}]}
        moderator_client.request('updateParticipantsOrder', params)
        time.sleep(1)
        notify = moderator_client.search_notify_list('partOrderOrRoleChangeNotify')[0]
        self.check_order_list(notify)

        # 墙下置顶一次
        moderator_client.clear_notify()
        params = {'orderedParts': [{'account': clients[9].uuid, 'order': 1}]}
        moderator_client.request('updateParticipantsOrder', params)
        time.sleep(1)
        notify = moderator_client.search_notify_list('partOrderOrRoleChangeNotify')[0]
        self.check_order_list(notify)

    def check_order_list(self, notify):
        """ 检查order 0-N 是不是连续的 """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        orders = notify['params']['updateParticipantsOrder']
        for i in range(0, len(orders)):
            flag = False
            for order in orders:
                if order['order'] == i:
                    flag = True
                    break
            self.assertTrue(flag, ' order ' + str(i) + ' 不存在 ' + str(notify))

    def test_order_replace(self):
        """ 创建随机会议，入会11人，墙上互换位置， 墙上和墙下互换 """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        moderator = self.users[0]
        moderator_client = self.loginAndAccessIn(moderator['phone'], moderator['pwd'])
        re = self.createRandomRoom(moderator_client)
        room_id = re[1]['roomId']
        self.joinRoom(moderator_client, room_id)
        logger.info('批量入会')
        clients = self.batchJoinRoom(room_id, 1, 11)
        logger.info('全部入会完毕')

        moderator_client.ms = MeetingService(moderator_client, room_id)
        re = moderator_client.ms.get_participants("exact", target_ids=[clients[1].uuid, clients[5].uuid])
        source_id = re[1]['participantList'][0]['account']
        source_order = re[1]['participantList'][0]['order']
        target_id = re[1]['participantList'][1]['account']
        target_order = re[1]['participantList'][1]['order']
        logger.info(f'{source_id} order {source_order} -> {target_order}')
        logger.info(f'{target_id} order {target_order} -> {source_order}')

        moderator_client.collecting_notify()
        # 墙上互换
        moderator_client.request('replaceParticipantsOrder',
                                 {'roomId': room_id, 'target': target_id, 'source': source_id})
        time.sleep(1)
        notify = moderator_client.search_notify_list('replaceParticipantsOrderNotify')[0]
        self.assertEqual(notify['params']['roleChange'], [], '发生了角色变化 ' + str(notify))
        self.assertEqual(notify['params']['source'], source_id, 'source_id 不对 ' + str(notify))
        self.assertEqual(notify['params']['target'], target_id, 'target_id 不对 ' + str(notify))
        # expect = [{'uuid': target_id, 'order': source_order}, {'uuid': source_id, 'order': target_order}]
        # self.assertListEqual(notify['params']['updateParticipantsOrder'], expect, ' 不对 ' + str(notify))

        # 墙上和墙下互换
        re = moderator_client.ms.get_participants("exact", target_ids=[clients[2].uuid, clients[9].uuid])
        source_id = re[1]['participantList'][0]['account']
        source_order = re[1]['participantList'][0]['order']
        target_id = re[1]['participantList'][1]['account']
        target_order = re[1]['participantList'][1]['order']
        logger.info(f'{source_id} order {source_order} -> {target_order}')
        logger.info(f'{target_id} order {target_order} -> {source_order}')
        moderator_client.clear_notify()
        moderator_client.request('replaceParticipantsOrder',
                                 {'roomId': room_id, 'target': target_id, 'source': source_id})
        time.sleep(1)
        notify = moderator_client.search_notify_list('replaceParticipantsOrderNotify')[0]
        self.assertEqual(len(notify['params']['roleChange']), 2, '发生了角色变化错误 ' + str(notify))
        self.assertEqual(notify['params']['source'], source_id, 'source_id 不对 ' + str(notify))
        self.assertEqual(notify['params']['target'], target_id, 'target_id 不对 ' + str(notify))
        # expect = [{'uuid': target_id, 'order': source_order}, {'uuid': source_id, 'order': target_order}]
        # self.assertListEqual(notify['params']['updateParticipantsOrder'], expect, ' 不对 ' + str(notify))
        self.assertTrue(moderator_client.has_notify('setAudioStatus'), ' 没有收到 setAudioStatus 通知')

    def test_force_disconnect_part(self):
        """ 测试主持人踢人，然后重新入会,重复10次
        测试目的：主持人踢人，然后重新入会order可能会冲突
        测试过程: 1、主持人入会，入会至少2与会者
                2、然后踢掉第一个与会者
                3、被踢的与会者入会
        结果期望：order正常
        """
        logger.info('测试主持人重连入会')
        moderator_client, room_id = self.loginAndAccessInAndCreateAndJoin(self.users[0])
        moderator_client.collecting_notify()
        part_client1, re = self.loginAndAccessInAndJoin(self.users[4], room_id)
        part_client2, re = self.loginAndAccessInAndJoin(self.users[5], room_id)
        part_client2.collecting_notify()
        notify = moderator_client.find_any_notify('partOrderOrRoleChangeNotify')
        self.check_order_list(notify)

        # 踢人，加入会议，重复10次
        for i in range(0, 10):
            part_client2.clear_notify()
            moderator_client.request("forceDisconnect", {'uuid': part_client1.uuid})
            notify = part_client2.find_any_notify('partOrderOrRoleChangeNotify')
            self.check_order_list(notify)

            part_client2.clear_notify()
            moderator_client.request("forceDisconnect", {'uuid': moderator_client.uuid})
            notify = part_client2.find_any_notify('partOrderOrRoleChangeNotify')
            self.check_order_list(notify)

            part_client2.clear_notify()
            moderator_client, re = self.loginAndAccessInAndJoin(self.users[0], room_id)
            notify = part_client2.find_any_notify('partOrderOrRoleChangeNotify')
            self.check_order_list(notify)

            part_client2.clear_notify()
            part_client1, re = self.loginAndAccessInAndJoin(self.users[4], room_id)
            notify = part_client2.find_any_notify('partOrderOrRoleChangeNotify')
            self.check_order_list(notify)


if __name__ == '__main__':
    unittest2.main()
