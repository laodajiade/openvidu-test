import sys
import time

import unittest2
from loguru import logger

import test
from test.service.services import MeetingService


class TestRecord(test.MyTestCase):
    """ 录制相关 """

    def test_record(self):
        """ 创建个人会议, 主持人推流，开始录制，7秒后结束录制 """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        # 主持人入会
        moderator = self.users[0]
        moderator_client = self.loginAndAccessIn(moderator['phone'], moderator['pwd'])
        re = self.createPersonalRoom(moderator_client)
        room_id = re[1]['roomId']
        self.joinRoom(moderator_client, room_id)

        major_stream_id = self.publish_video(moderator_client, 'MAJOR')

        re = moderator_client.request('startConferenceRecord', {'roomId': room_id})
        self.assertEqual(re[0], 0, ' 开启录制失败 ' + str(re))
        time.sleep(7)
        re = moderator_client.request('stopConferenceRecord', {'roomId': room_id})
        self.assertEqual(re[0], 0, ' 关闭录制失败 ' + str(re))
        time.sleep(1)

    def test_record_over_9(self):
        """ 测试墙上人数超过9人情况下开启录制。
        测试目的：录制布局最大只有9人，超过9的只有声音
        测试过程: 1、主持人入会创建会议
                2、加入11人
                3、主持人开启会议
                4、监听录制回调，10秒没报错表示成功。
        结果期望：墙上人数超过9人，最大只有9等分，剩下只录音
        """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        # 主持人入会
        moderator_client = self.loginAndAccessIn2(self.users[0])
        try:
            self.set_sfu_publisher_threshold(moderator_client, 11)
            logger.info("修改墙上人数为11人")
            moderator_client, room_id = self.loginAndAccessInAndCreateAndJoin(self.users[0])
            self.publish_video(moderator_client, 'MAJOR')
            logger.info('连续入会11人')
            clients = self.batchJoinRoom(room_id, 1, 12)

            re = moderator_client.request('startConferenceRecord', {'roomId': room_id})
            self.assertEqual(re[0], 0, ' 开启录制失败 ' + str(re))
            time.sleep(10)
            re = moderator_client.request('stopConferenceRecord', {'roomId': room_id})
            self.assertEqual(re[0], 0, ' 关闭录制失败 ' + str(re))
            time.sleep(1)
        finally:
            self.set_sfu_publisher_threshold(moderator_client, 9)
            logger.info("修改墙上人数为9人")

    def test_record_speaker_tag(self):
        """ 测试录制的发言标签
        测试目的：录测试录制的发言标签
        测试过程: 1、主持人入会创建会议
                2、加入8人
                3、主持人开始录制
                4、主持人点名发言
                5、主持人替换发言
        结果期望：发言标签正常被替换 """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        # 主持人入会
        moderator_client, room_id = self.loginAndAccessInAndCreateAndJoin(self.users[0])
        clients = self.batchJoinRoom(room_id, 1, 7)

        # client2 = self.loginAndAccessIn2(self.users[9], type='HDC')
        client2 = self.loginAndAccessIn2(self.users[9])
        client2.joinRoom(room_id)

        time.sleep(1)
        self.publish_video(moderator_client, 'MAJOR')
        result = moderator_client.request('startConferenceRecord', {'roomId': room_id})
        self.assertEqual(result[0], 0, ' 开启录制失败 ' + str(result))
        time.sleep(5)
        moderator_client.ms = MeetingService(moderator_client, room_id)
        result = moderator_client.ms.set_roll_call(client2.uuid)
        self.assertEqual(result[0], 0, ' 点名发言失败 ' + str(result))
        time.sleep(10)

    @unittest2.skip('没有真正的流，录制会失败')
    def test_record_speaker_shareing(self):
        """测试录制画面，同时存在发言和分享的情况下布局
        测试目的：测试录制画面，同时存在发言和分享的情况下布局
        测试过程: 1、创建会议
                2、开启录制
                3、order=1 分享
                4、order=1 发言
                5、停止录制
        结果期望： 当有发言、有共享时，小画面第一位显示发言人。其他小画面按照参会者列表顺序排列。
        """
        logger.info(getattr(self, sys._getframe().f_code.co_name).__doc__)
        if True:
            # 暂时不测试
            return
        moderator_client, room_id = self.loginAndAccessInAndCreateAndJoin(self.users[0])

        major_stream_id = self.publish_video(moderator_client, 'MAJOR')
        part_client, re = self.loginAndAccessInAndJoin(self.users[1], room_id)

        re = moderator_client.request('startConferenceRecord', {'roomId': room_id})
        self.assertEqual(re[0], 0, ' 开启录制失败 ' + str(re))
        time.sleep(3)

        re = part_client.request('applyShare', {'targetId': part_client.uuid})
        self.assertEqual(re[0], 0, '分享失败')

        time.sleep(5)
        re = moderator_client.request("setRollCall", {"roomId": room_id, "originator": moderator_client.uuid,
                                                      "targetId": part_client.uuid})
        self.assertEqual(re[0], 0, '发言失败')
        time.sleep(5)

        re = moderator_client.request('stopConferenceRecord', {'roomId': room_id})
        self.assertEqual(re[0], 0, ' 关闭录制失败 ' + str(re))
        time.sleep(1)


if __name__ == '__main__':
    unittest2.main()
