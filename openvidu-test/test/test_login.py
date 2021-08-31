import unittest2

import test
from common.mock_client import SdClient


class TestLogin(test.MyTestCase):
    """登录相关"""

    def test_login(self):
        """ getToken """
        client = SdClient(self.users[0]['phone'], self.users[0]['pwd'], self.server_url)
        self.assertEqual(client.login(), self.users[0]['uuid'])
        # self.assertTrue(False)

    def test_accessIn(self):
        """ accessIn """
        client = SdClient(self.users[0]['phone'], self.users[0]['pwd'], self.server_url)
        # self.client = client
        re = client.loginAndAccessIn()
        self.assertEqual(re[0], 0)
        client.logout()


if __name__ == '__main__':
    unittest2.main()
