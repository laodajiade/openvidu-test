import os
import time
import unittest

from loguru import logger

t1 = time.time()
logger.info("******************** all_test ********************")
loader = unittest.TestLoader()
suite = loader.discover(os.getcwd() + '/test')
#suite = loader.discover(os.getcwd(), pattern='test_create_room2.py')

runner = unittest.TextTestRunner()
re = runner.run(suite)
t2 = time.time()
logger.info("******************** all_test ********************")
logger.info(f'ran in {round(t2 - t1, 2)}s')
logger.info(re)
