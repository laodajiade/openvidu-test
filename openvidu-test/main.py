import getopt
import os
import sys
import time
import unittest

from TestRunner import HTMLTestRunner
from loguru import logger


def all_test(path):
    t1 = time.time()
    logger.info("******************** all_test ********************")
    loader = unittest.TestLoader()
    # suite = loader.discover(os.getcwd() + '/test')
    suite = loader.discover(os.getcwd(), pattern='test_login.py')

    time_postfix = time.strftime("%Y_%m_%d_%H_%M_%S", time.localtime())
    file_name = path + 'test_report_' + time_postfix + '.html'
    with open(file_name, 'wb') as fp:
        runner = HTMLTestRunner(
            stream=fp,
            title='信令自测用例',
            description='信令2.0自测冒烟用例'
        )
        re = runner.run(suite)
        logger.info(re.success_count)
        logger.info(re.failure_count)
        logger.info(re.error_count)
        logger.info(re.skip_count)
    t2 = time.time()
    logger.info("******************** all_test ********************")
    logger.info(f'ran in {round(t2 - t1, 2)}s')


def main():
    """
    n=job_name 任务名
    s=skip  1=T 是否跳过耗时的用例，默认跳过
    -d, --directory  目录
     """
    sys.modules['fast_test'] = False
    shortargs = 'n:b:c:s:'
    opts, args = getopt.getopt(sys.argv[1:], shortargs)
    print(opts)
    job_name = ''
    git_branch = 'master'
    commit_id = None
    direct = ''
    for opt, val in opts:
        if opt in ('-n'):
            job_name = val
            continue
        if opt in ('-b'):
            git_branch = val
            continue
        if opt in ('-c'):
            commit_id = val[0:8]
            continue
        if opt in ('-s'):
            if val != '1':
                sys.modules['fast_test'] = False
            continue
        if opt in ('-d'):
            direct = val
            continue
    all_test(direct, )


if __name__ == '__main__':
    main()
