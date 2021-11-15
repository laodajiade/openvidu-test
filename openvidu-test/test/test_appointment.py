import sys

import unittest2
from loguru import logger
import time

import test
from common.sd_utils import SDUtil

try:
    import thread
except ImportError:
    import _thread as thread


class TestCreateRoom(test.MyTestCase):
    """ 创建预约会议相关用例 """

    def tearDown(self):
        super(TestCreateRoom, self).tearDown()

    def test_urge_fix_room(self):
        """ 催促固定会议室 """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        room_id = self.fixed_rooms[0]['roomId']

        client0 = self.loginAndAccessIn2(self.users[0])
        result = client0.request('createAppointmentRoom',
                                 params=self.general_appt_param(room_id, 'fixed', client0.uuid))
        self.assertEqual(result[0], 0)
        ruid = result[1]['ruid']

        # 创建会议，抢占会议室
        client1 = self.loginAndAccessIn2(self.users[1])
        result = client1.createRoom(room_id, '固定会议室123')
        self.assertEqual(result[0], 0)
        result = client1.joinRoom(room_id)
        self.assertEqual(result[0], 0)
        logger.info('催促结束')
        client0.request('urgedPeopleToEnd', {'roomId': room_id, 'ruid': ruid})
        time.sleep(1)


    def general_appt_param(self, room_id, room_id_type='personal', *participants):
        start_time = int(SDUtil.GetUtcMs()) + 6000000
        params = {'roomId': room_id, 'password': '111111', 'subject': '创建预约会议', 'desc': '', 'conferenceMode': 'SFU',
                  'autoCall': True, 'roomCapacity': 500, 'startTime': start_time, 'duration': 5,
                  'participants': participants, 'roomIdType': room_id_type}
        return params


if __name__ == '__main__':
    unittest2.main()
