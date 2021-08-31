import time

import json
import os
import test
import unittest2
from test.service.services import  MeetingService
from loguru import logger
from common.mock_client import SdClient
import threading

class TestStringMethods(test.MyTestCase):
    """ 连接数压测 """


    def test_create_and_join(self):
        """  """
        # 与会者个数
        part_size = 10
        # 入会完成后等待时长 秒
        timeout = 2000

        # 主持人入会
        base_phone = '17188810501'
        default_pwd = '123456'
        success_cnt = 0
        failure_cnt = 0
        thread_cnt = 10
        for i in range(0, 1000):
            phone = str(int(base_phone) + i)
            client = SdClient(phone, default_pwd, self.server_url)
            re = client.loginAndAccessIn()
            if re[0] == 0:
                success_cnt += 1
            else:
                failure_cnt += 1
        time.sleep(30)
        for i in range(0, thread_cnt):
            t = threading.Thread(target=self.login)
            t.setDaemon(False)
            t.start()
            pass
        print(f'success {str(success_cnt)}, failure {str(failure_cnt)}')


    def login(self):
        pass


if __name__ == '__main__':
    unittest2.main()
