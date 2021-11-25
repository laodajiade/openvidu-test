import base64
import hashlib
import hmac
import json
import random
import ssl
import string
import sys
import time
from urllib import error
from urllib import parse
from urllib import request

from loguru import logger


class SDUtil():
    @staticmethod
    def RandomString():
        return ''.join(random.sample(string.ascii_letters + string.digits, 32))

    @staticmethod
    def GetUtcMs():
        return str(int(time.time() * 1000))  # 时间戳以ms为单位

    @staticmethod
    def MD5(inputStr):
        m = hashlib.md5()
        m.update(inputStr)
        return m.hexdigest()

    @staticmethod
    def HMAC_SHA256_BASE64(secrectKey, inputStr):
        h = hmac.new(bytearray(secrectKey, 'utf-8'),
                     bytearray(inputStr, 'utf-8'),
                     digestmod=hashlib.sha256)
        return base64.b64encode(h.digest())

    @staticmethod
    def waitTime(runTime):
        # 等待时间
        beginMs = int(SDUtil.GetUtcMs())
        timeoutMs = runTime * 1000
        taskProgressVar = ['.', '..', '...']
        taskProgressIndex = 0
        while int(SDUtil.GetUtcMs()) - beginMs < timeoutMs:
            SDUtil.printCurrentTimeMs(
                'benchmark persist task' +
                str(taskProgressVar[taskProgressIndex % len(taskProgressVar)]))
            taskProgressIndex += 1
            time.sleep(1)

    @staticmethod
    def printCurrentTimeMs(externalStr='', printStat=True, printFileAndLine=True):
        ct = time.time()
        local_time = time.localtime(ct)
        data_time = time.strftime("%Y.%m.%d %H:%M:%S", local_time)
        data_ms = (ct - int(ct)) * 1000
        if printFileAndLine:
            time_stamp = "%s.%03d %s %d %s" % (
                data_time, data_ms, sys._getframe().f_back.f_code.co_name,
                sys._getframe().f_back.f_lineno, externalStr)
        else:
            time_stamp = "%s.%03d %s" % (
                data_time, data_ms, externalStr)

        if printStat:
            print(time_stamp)
        return time_stamp


class SDHTTPClient():
    def __init__(self, serverUrl, account, password):
        self.serverUrl = serverUrl
        self.account = account
        self.password = password

    def __del__(self):
        # print('SDHTTPClient finish!')
        pass

    def doHTTPHeaderAuth(self, req, pwd):
        httpMethod = req.get_method()
        httpUri = parse.urlparse(req.get_full_url()).path
        contentMD5 = SDUtil.MD5(req.data)
        contentType = 'application/json'
        nonceStr = ''.join(
            random.sample(string.ascii_letters + string.digits, 32))
        curTimestamp = str(int(time.time() * 1000))  # 时间戳以ms为单位

        signStr = httpMethod + '\n' + httpUri + '\n' + contentMD5 + '\n' + contentType + '\n' + \
                  'X-Sd-Account:' + self.account + '\n' + 'X-Sd-Apiver:' + '1.0' + '\n' + \
                  'X-Sd-Nonce:' + nonceStr + '\n' + 'X-Sd-Timestamp:' + curTimestamp + '\n'
        signature = SDUtil.HMAC_SHA256_BASE64(pwd, signStr)
        req.add_header('X-Real-Ip', '127.0.0.1')
        req.add_header('Content-MD5', contentMD5)
        req.add_header('Content-Type', contentType)
        req.add_header('X-Sd-Account', self.account)
        req.add_header('X-Sd-Apiver', '1.0')
        req.add_header('X-Sd-Nonce', nonceStr)
        req.add_header('X-Sd-Timestamp', curTimestamp)
        req.add_header('X-Sd-Signature', signature)
        req.add_header('Accept-Encoding', "gzip")

        # print(nonceStr, httpMethod, httpUri, curTimestamp, signStr, signature)
        return req

    # Do http request and enable auth.
    # URL:         url path
    # param:    url body json param
    def request(self, URL, param):
        httpReqRet = False
        responseJson = ''

        try:
            httpReqBodyData = json.dumps(param)
            URL = self.serverUrl + URL
            ssl._create_default_https_context = ssl._create_unverified_context
            # print('URL:',URL,'password:',self.password)

            data = bytes(httpReqBodyData, 'utf-8')
            req = request.Request(url=URL, data=data)
            req = self.doHTTPHeaderAuth(req, self.password)

            responseData = request.urlopen(req).read().decode("utf-8")
            # print responseData
            responseJson = json.loads(responseData)
            # print json.dumps(responseJson,sort_keys=True,indent=4)

            httpReqRet = True
        except error.HTTPError as e:
            if hasattr(e, 'code') and e.code == 401:
                responseData = e.read()
                responseJson = json.loads(responseData)
                return True, responseJson
            else:
                # print '---url open failed---'
                # print e.code
                responseData = e.read().decode("utf-8")
                logger.error(
                    "%s",
                    SDUtil.printCurrentTimeMs(
                        'url open failed ' + str(e.code) + str(e.msg) + responseData, False))
                pass
        except Exception as e:
            logger.error(
                "%s", SDUtil.printCurrentTimeMs('exception:' + str(e), False))
            return False, responseJson

        return httpReqRet, responseJson
