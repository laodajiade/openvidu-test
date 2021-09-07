import sys
import time

import unittest2
from loguru import logger

import test


class TestOperation(test.MyTestCase):
    """ 会议操作 """

    def test_set_video_status(self):
        """ 设置视频状态 """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        # 主持人入会
        moderator_client, room_id = self.loginAndAccessInAndCreateAndJoin(self.users[0])
        part_client, re = self.loginAndAccessInAndJoin(self.users[3], room_id)
        moderator_client.collecting_notify()
        part_client.collecting_notify()
        re = moderator_client.request('setVideoStatus',
                                      {'roomId': room_id, 'source': moderator_client.uuid,
                                       'targets': [part_client.uuid],
                                       'status': 'off'})
        self.assertEqual(re[0], 0, '设置视频状态错误')
        time.sleep(1)
        self.assertTrue(moderator_client.has_notify_sync('setVideoStatus'))
        self.assertTrue(part_client.has_notify_sync('setVideoStatus'))


if __name__ == '__main__':
    unittest2.main()
