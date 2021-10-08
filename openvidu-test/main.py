import json
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
    suite = loader.discover(os.getcwd() + '/test')
    # suite = loader.discover(os.getcwd(), pattern='test_login.py')

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
    with open(os.path.abspath(__file__) + '/../test/resource/conf.json', 'r', encoding='UTF-8') as load_f:
        load_dict = json.load(load_f)
        conf_json = load_dict['default']
        use_evn = load_dict['use_evn']
        for k, v in load_dict[use_evn].items():
            conf_json[k] = v
    # 耗时用例是否测试，本地快速跑通用例时可以跳过，在测试环境最好全用例测试
    sys.modules['fast_test'] = conf_json['fast']
    print(conf_json['fast'])
    direct = ''
    all_test(direct, )


if __name__ == '__main__':
    main()
