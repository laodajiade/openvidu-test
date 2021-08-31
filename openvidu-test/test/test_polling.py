import sys
import time

import unittest2

import test


class TestMCU(test.MyTestCase):
    """ 轮询相关 """

    @unittest2.skipIf(sys.modules.get('fast_test'), '跳过耗时用例')
    def test_simple_polling(self):
        """ 简单轮询
        """
        moderator_client, room_id = self.loginAndAccessInAndCreateAndJoin(self.users[0])

        part_client, re = self.loginAndAccessInAndJoin(self.users[1], room_id)
        self.publish_video(part_client, 'MAJOR')
        part_client, re = self.loginAndAccessInAndJoin(self.users[2], room_id)
        self.publish_video(part_client, 'MAJOR')
        part_client, re = self.loginAndAccessInAndJoin(self.users[3], room_id)
        self.publish_video(part_client, 'MAJOR')
        part_client, re = self.loginAndAccessInAndJoin(self.users[4], room_id)
        self.publish_video(part_client, 'MAJOR')

        moderator_client.collecting_notify()
        moderator_client.request("startPolling", {"roomId": room_id, "time": "1"})
        time.sleep(6)
        notifies = moderator_client.search_notify_list('pollingCheckNotify')
        # 待轮询列表
        uuids = [self.users[1]['uuid'], self.users[2]['uuid'], self.users[3]['uuid'], self.users[4]['uuid']]
        for notify in notifies:
            if notify['params']['isCheck']:
                if notify['params']['targetId'] in uuids:
                    uuids.remove(notify['params']['targetId'])
        self.assertEqual(uuids, [], '有人未被轮询到:' + str(uuids))  # 所有人都被轮询到，理论上数组为空

    ############################################################


if __name__ == '__main__':
    unittest2.main()
