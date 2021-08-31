import os

from loguru import logger

import test.service
from test.my_test_case import MyTestCase

logger.add(os.path.abspath(__file__) + "/../../log.log", filter="", level="INFO", rotation="10 MB", encoding='utf-8')
