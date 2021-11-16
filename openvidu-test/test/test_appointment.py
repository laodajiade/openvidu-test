import sys
import time

import unittest2
from loguru import logger

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

    def test_empty(self):
        pass

    def test_cancel_started_appt(self):
        """ 分布式下取消已经开始的预约会议
       测试目的：分布式下取消已经开始的预约会议存在无法关闭会议的情况
       测试过程: 1、预约会议，主持人提前开始
                2、进入一人
                3、主持人离开会议
                4、主持人重新登录，让路由重新选择instanceId
       结果期望： 每次取消会议都会关闭会议
       注意点：因为分布式，instanceId会被随机分配，需要多次测试覆盖多个信令
       """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        moderator = self.loginAndAccessIn2(self.users[0])
        room_id, ruid = self.create_appointment_room(moderator, moderator.uuid)
        self.start_appointment_room(moderator, room_id, ruid)

        part, result = self.loginAndAccessInAndJoin(self.users[2], room_id)
        part.collecting_notify()
        logger.info('主持人 离开会议和退出登录')
        moderator.leave_room(room_id)
        moderator.logout()
        logger.info('主持人 重新登录')
        moderator = self.loginAndAccessIn2(self.users[0])
        logger.info('主持人 取消会议')
        moderator.request('cancelAppointmentRoom', {'ruid': ruid})
        logger.info('检查是否收到关闭会议的通知')
        self.assertTrue(part.has_notify_sync('closeRoomNotify', timeout=2000), '与会者没有收到关闭会议通知')

    def test_cancel_started_appt_cycle_20(self):
        """ test_cancel_started_appt 执行20次，增加随机性 """
        for i in range(0, 20):
            self.test_cancel_started_appt()

    def a_test_urge_fix_room(self):
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

    def create_appointment_room(self, client, *participants):
        """创建预约会议"""
        startTime = int(SDUtil.GetUtcMs()) + 60000
        param = self.general_appt_params('123', startTime, participants=participants, room_id_type='random', )
        result = client.request('createAppointmentRoom', param)
        self.assertEqual(result[0], 0)
        return result[1]['roomId'], result[1]['ruid']

    def start_appointment_room(self, client, roomId, ruid):
        """提前开始预约会议"""
        result = client.createRoom(roomId, client.uuid + '的预约会议', room_id_type='random', ruid=ruid)
        self.assertEqual(result[0], 0, msg='create room error ' + str(result))
        logger.info('create room ' + result[1]['roomId'])

    def general_appt_params(self, room_id, start_time, participants, room_id_type='personal'):
        params = {'roomId': room_id, 'subject': 'auto创建预约会议', 'desc': '', 'conferenceMode': 'SFU',
                  'autoCall': True, 'roomCapacity': 500, 'startTime': start_time, 'duration': 5,
                  'participants': participants, 'roomIdType': room_id_type}
        return params


if __name__ == '__main__':
    unittest2.main()
