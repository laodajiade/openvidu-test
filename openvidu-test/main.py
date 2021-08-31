import os
import time
import unittest
from TestRunner import HTMLTestRunner

from loguru import logger

def main():
    t1 = time.time()
    logger.info("******************** all_test ********************")
    loader = unittest.TestLoader()
    #suite = loader.discover(os.getcwd())
    suite = loader.discover(os.getcwd(), pattern='test_speaker.py')

    runner = unittest.TextTestRunner()
    re = runner.run(suite)
    t2 = time.time()
    logger.info("******************** all_test ********************")
    logger.info(f'ran in {round(t2 - t1, 2)}s')
    logger.info(re)

def main2():
    t1 = time.time()
    logger.info("******************** all_test ********************")
    loader = unittest.TestLoader()
    suite = loader.discover(os.getcwd() + '/test')
    # suite = loader.discover(os.getcwd(), pattern='test_close*.py')

    time_postfix = time.strftime("%Y_%m_%d_%H_%M_%S", time.localtime())
    file_name = 'rest_report_' + time_postfix+'.html'
    with open(file_name, 'wb') as fp:
        runner = HTMLTestRunner(
            stream=fp,
            title='信令自动化测试用例',
            description='信令2.0自动化测试用例'
        )
        re = runner.run(suite)
        logger.info(re)
    t2 = time.time()
    logger.info("******************** all_test ********************")
    logger.info(f'ran in {round(t2 - t1, 2)}s')


if __name__ == '__main__':
    main2()