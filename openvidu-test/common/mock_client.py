import json
import random
import ssl
import threading
import time

import websocket
from loguru import logger

from common.sd_utils import SDHTTPClient
from common.sd_utils import SDUtil

try:
    import thread
except ImportError:
    import _thread as thread

log_init = False


class SdClient:
    """

    """

    getTokenURL = "/api/v1/auth/get/token"
    userLogin = "/api/voip/v1/user/login"
    userLogout = "/api/voip/v1/user/logout"
    getSignalAddrList = "/api/voip/v1/lookup/signal/address"
    server_url = ''

    sdpOffers = [
        'v=0\r\no=- 5431140086745666622 2 IN IP4 127.0.0.1\r\ns=-\r\nt=0 0\r\na=group:BUNDLE audio video\r\na=msid-semantic: WMS stream_id\r\nm=audio 9 UDP/TLS/RTP/SAVPF 111 103 104 9 102 0 8 106 105 13 110 112 113 126\r\nc=IN IP4 0.0.0.0\r\na=rtcp:9 IN IP4 0.0.0.0\r\na=ice-ufrag:bOYJ\r\na=ice-pwd:Lep71RqLD8r1bRYLf6nsjWRL\r\na=ice-options:trickle\r\na=fingerprint:sha-256 E7:D2:E8:85:CF:89:42:40:8B:F7:46:BF:44:69:61:A3:0E:2A:92:67:AD:62:A0:E4:29:1E:BB:66:27:CE:AF:37\r\na=setup:actpass\r\na=mid:audio\r\na=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\na=extmap:2 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\na=extmap:3 http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01\r\na=sendrecv\r\na=rtcp-mux\r\na=rtpmap:111 opus/48000/2\r\na=rtcp-fb:111 transport-cc\r\na=fmtp:111 minptime=10;useinbandfec=1\r\na=rtpmap:103 ISAC/16000\r\na=rtpmap:104 ISAC/32000\r\na=rtpmap:9 G722/8000\r\na=rtpmap:102 ILBC/8000\r\na=rtpmap:0 PCMU/8000\r\na=rtpmap:8 PCMA/8000\r\na=rtpmap:106 CN/32000\r\na=rtpmap:105 CN/16000\r\na=rtpmap:13 CN/8000\r\na=rtpmap:110 telephone-event/48000\r\na=rtpmap:112 telephone-event/32000\r\na=rtpmap:113 telephone-event/16000\r\na=rtpmap:126 telephone-event/8000\r\na=ssrc:1179745744 cname:VZTzQtV1zz2dkZNi\r\na=ssrc:1179745744 msid:stream_id audio_label\r\na=ssrc:1179745744 mslabel:stream_id\r\na=ssrc:1179745744 label:audio_label\r\nm=video 9 UDP/TLS/RTP/SAVPF 96 97 98 99 100 101 127 123 125 122 124 107 108\r\nc=IN IP4 0.0.0.0\r\na=rtcp:9 IN IP4 0.0.0.0\r\na=ice-ufrag:bOYJ\r\na=ice-pwd:Lep71RqLD8r1bRYLf6nsjWRL\r\na=ice-options:trickle\r\na=fingerprint:sha-256 E7:D2:E8:85:CF:89:42:40:8B:F7:46:BF:44:69:61:A3:0E:2A:92:67:AD:62:A0:E4:29:1E:BB:66:27:CE:AF:37\r\na=setup:actpass\r\na=mid:video\r\na=extmap:14 urn:ietf:params:rtp-hdrext:toffset\r\na=extmap:2 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\na=extmap:13 urn:3gpp:video-orientation\r\na=extmap:3 http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01\r\na=extmap:5 http://www.webrtc.org/experiments/rtp-hdrext/playout-delay\r\na=extmap:6 http://www.webrtc.org/experiments/rtp-hdrext/video-content-type\r\na=extmap:7 http://www.webrtc.org/experiments/rtp-hdrext/video-timing\r\na=extmap:8 http://tools.ietf.org/html/draft-ietf-avtext-framemarking-07\r\na=extmap:9 http://www.webrtc.org/experiments/rtp-hdrext/color-space\r\na=sendrecv\r\na=rtcp-mux\r\na=rtcp-rsize\r\na=rtpmap:96 H264/90000\r\na=rtcp-fb:96 goog-remb\r\na=rtcp-fb:96 transport-cc\r\na=rtcp-fb:96 ccm fir\r\na=rtcp-fb:96 nack\r\na=rtcp-fb:96 nack pli\r\na=fmtp:96 level-asymmetry-allowed=1;packetization-mode=1;profile-level-id=42e01f\r\na=rtpmap:97 rtx/90000\r\na=fmtp:97 apt=96\r\na=rtpmap:98 H264/90000\r\na=rtcp-fb:98 goog-remb\r\na=rtcp-fb:98 transport-cc\r\na=rtcp-fb:98 ccm fir\r\na=rtcp-fb:98 nack\r\na=rtcp-fb:98 nack pli\r\na=fmtp:98 level-asymmetry-allowed=1;packetization-mode=0;profile-level-id=42e01f\r\na=rtpmap:99 rtx/90000\r\na=fmtp:99 apt=98\r\na=rtpmap:100 VP8/90000\r\na=rtcp-fb:100 goog-remb\r\na=rtcp-fb:100 transport-cc\r\na=rtcp-fb:100 ccm fir\r\na=rtcp-fb:100 nack\r\na=rtcp-fb:100 nack pli\r\na=rtpmap:101 rtx/90000\r\na=fmtp:101 apt=100\r\na=rtpmap:127 VP9/90000\r\na=rtcp-fb:127 goog-remb\r\na=rtcp-fb:127 transport-cc\r\na=rtcp-fb:127 ccm fir\r\na=rtcp-fb:127 nack\r\na=rtcp-fb:127 nack pli\r\na=fmtp:127 profile-id=0\r\na=rtpmap:123 rtx/90000\r\na=fmtp:123 apt=127\r\na=rtpmap:125 VP9/90000\r\na=rtcp-fb:125 goog-remb\r\na=rtcp-fb:125 transport-cc\r\na=rtcp-fb:125 ccm fir\r\na=rtcp-fb:125 nack\r\na=rtcp-fb:125 nack pli\r\na=fmtp:125 profile-id=2\r\na=rtpmap:122 rtx/90000\r\na=fmtp:122 apt=125\r\na=rtpmap:124 red/90000\r\na=rtpmap:107 rtx/90000\r\na=fmtp:107 apt=124\r\na=rtpmap:108 ulpfec/90000\r\na=ssrc-group:FID 3247688585 755752525\r\na=ssrc:3247688585 cname:VZTzQtV1zz2dkZNi\r\na=ssrc:3247688585 msid:stream_id video_label\r\na=ssrc:3247688585 mslabel:stream_id\r\na=ssrc:3247688585 label:video_label\r\na=ssrc:755752525 cname:VZTzQtV1zz2dkZNi\r\na=ssrc:755752525 msid:stream_id video_label\r\na=ssrc:755752525 mslabel:stream_id\r\na=ssrc:755752525 label:video_label\r\n',
        'v=0\r\no=- 4123957929568519432 2 IN IP4 127.0.0.1\r\ns=-\r\nt=0 0\r\na=group:BUNDLE audio video\r\na=msid-semantic: WMS stream_id\r\nm=audio 9 UDP/TLS/RTP/SAVPF 111 103 104 9 102 0 8 106 105 13 110 112 113 126\r\nc=IN IP4 0.0.0.0\r\na=rtcp:9 IN IP4 0.0.0.0\r\na=ice-ufrag:s8/R\r\na=ice-pwd:kUomDX/z+H8rrWVBwN7Jn79/\r\na=ice-options:trickle\r\na=fingerprint:sha-256 F0:B9:11:A1:68:24:47:B2:08:7F:CA:25:6E:CA:16:CC:3D:34:53:A7:AF:C1:F7:FC:76:55:E1:8F:03:6F:F7:41\r\na=setup:actpass\r\na=mid:audio\r\na=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\na=extmap:2 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\na=extmap:3 http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01\r\na=sendrecv\r\na=rtcp-mux\r\na=rtpmap:111 opus/48000/2\r\na=rtcp-fb:111 transport-cc\r\na=fmtp:111 minptime=10;useinbandfec=1\r\na=rtpmap:103 ISAC/16000\r\na=rtpmap:104 ISAC/32000\r\na=rtpmap:9 G722/8000\r\na=rtpmap:102 ILBC/8000\r\na=rtpmap:0 PCMU/8000\r\na=rtpmap:8 PCMA/8000\r\na=rtpmap:106 CN/32000\r\na=rtpmap:105 CN/16000\r\na=rtpmap:13 CN/8000\r\na=rtpmap:110 telephone-event/48000\r\na=rtpmap:112 telephone-event/32000\r\na=rtpmap:113 telephone-event/16000\r\na=rtpmap:126 telephone-event/8000\r\na=ssrc:4094818924 cname:oyHBCMGuvCC2GfSS\r\na=ssrc:4094818924 msid:stream_id audio_label\r\na=ssrc:4094818924 mslabel:stream_id\r\na=ssrc:4094818924 label:audio_label\r\nm=video 9 UDP/TLS/RTP/SAVPF 96 97 98 99 100 101 127 123 125 122 124 107 108\r\nc=IN IP4 0.0.0.0\r\na=rtcp:9 IN IP4 0.0.0.0\r\na=ice-ufrag:s8/R\r\na=ice-pwd:kUomDX/z+H8rrWVBwN7Jn79/\r\na=ice-options:trickle\r\na=fingerprint:sha-256 F0:B9:11:A1:68:24:47:B2:08:7F:CA:25:6E:CA:16:CC:3D:34:53:A7:AF:C1:F7:FC:76:55:E1:8F:03:6F:F7:41\r\na=setup:actpass\r\na=mid:video\r\na=extmap:14 urn:ietf:params:rtp-hdrext:toffset\r\na=extmap:2 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\na=extmap:13 urn:3gpp:video-orientation\r\na=extmap:3 http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01\r\na=extmap:5 http://www.webrtc.org/experiments/rtp-hdrext/playout-delay\r\na=extmap:6 http://www.webrtc.org/experiments/rtp-hdrext/video-content-type\r\na=extmap:7 http://www.webrtc.org/experiments/rtp-hdrext/video-timing\r\na=extmap:8 http://tools.ietf.org/html/draft-ietf-avtext-framemarking-07\r\na=extmap:9 http://www.webrtc.org/experiments/rtp-hdrext/color-space\r\na=sendrecv\r\na=rtcp-mux\r\na=rtcp-rsize\r\na=rtpmap:96 H264/90000\r\na=rtcp-fb:96 goog-remb\r\na=rtcp-fb:96 transport-cc\r\na=rtcp-fb:96 ccm fir\r\na=rtcp-fb:96 nack\r\na=rtcp-fb:96 nack pli\r\na=fmtp:96 level-asymmetry-allowed=1;packetization-mode=1;profile-level-id=42e01f\r\na=rtpmap:97 rtx/90000\r\na=fmtp:97 apt=96\r\na=rtpmap:98 H264/90000\r\na=rtcp-fb:98 goog-remb\r\na=rtcp-fb:98 transport-cc\r\na=rtcp-fb:98 ccm fir\r\na=rtcp-fb:98 nack\r\na=rtcp-fb:98 nack pli\r\na=fmtp:98 level-asymmetry-allowed=1;packetization-mode=0;profile-level-id=42e01f\r\na=rtpmap:99 rtx/90000\r\na=fmtp:99 apt=98\r\na=rtpmap:100 VP8/90000\r\na=rtcp-fb:100 goog-remb\r\na=rtcp-fb:100 transport-cc\r\na=rtcp-fb:100 ccm fir\r\na=rtcp-fb:100 nack\r\na=rtcp-fb:100 nack pli\r\na=rtpmap:101 rtx/90000\r\na=fmtp:101 apt=100\r\na=rtpmap:127 VP9/90000\r\na=rtcp-fb:127 goog-remb\r\na=rtcp-fb:127 transport-cc\r\na=rtcp-fb:127 ccm fir\r\na=rtcp-fb:127 nack\r\na=rtcp-fb:127 nack pli\r\na=fmtp:127 profile-id=0\r\na=rtpmap:123 rtx/90000\r\na=fmtp:123 apt=127\r\na=rtpmap:125 VP9/90000\r\na=rtcp-fb:125 goog-remb\r\na=rtcp-fb:125 transport-cc\r\na=rtcp-fb:125 ccm fir\r\na=rtcp-fb:125 nack\r\na=rtcp-fb:125 nack pli\r\na=fmtp:125 profile-id=2\r\na=rtpmap:122 rtx/90000\r\na=fmtp:122 apt=125\r\na=rtpmap:124 red/90000\r\na=rtpmap:107 rtx/90000\r\na=fmtp:107 apt=124\r\na=rtpmap:108 ulpfec/90000\r\na=ssrc-group:FID 3482833646 1057359504\r\na=ssrc:3482833646 cname:oyHBCMGuvCC2GfSS\r\na=ssrc:3482833646 msid:stream_id video_label\r\na=ssrc:3482833646 mslabel:stream_id\r\na=ssrc:3482833646 label:video_label\r\na=ssrc:1057359504 cname:oyHBCMGuvCC2GfSS\r\na=ssrc:1057359504 msid:stream_id video_label\r\na=ssrc:1057359504 mslabel:stream_id\r\na=ssrc:1057359504 label:video_label\r\n',
        'v=0\r\no=- 1504417604294395442 2 IN IP4 127.0.0.1\r\ns=-\r\nt=0 0\r\na=group:BUNDLE audio video\r\na=msid-semantic: WMS 102\r\nm=audio 9 UDP/TLS/RTP/SAVPF 111 103 104 9 102 0 8 106 105 13 110 112 113 126\r\nc=IN IP4 0.0.0.0\r\na=rtcp:9 IN IP4 0.0.0.0\r\na=ice-ufrag:yqnf\r\na=ice-pwd:xZdM6CbUsYvCv9z6WOQxxcj1\r\na=ice-options:trickle renomination\r\na=fingerprint:sha-256 0C:A5:16:CD:C8:E2:AB:73:0E:08:90:95:2F:A3:35:0F:1F:CB:49:96:41:CD:53:B4:73:FC:8A:D7:1A:B8:02:21\r\na=setup:actpass\r\na=mid:audio\r\na=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\na=extmap:2 http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01\r\na=sendrecv\r\na=rtcp-mux\r\na=rtpmap:111 opus/48000/2\r\na=rtcp-fb:111 transport-cc\r\na=fmtp:111 minptime=10;useinbandfec=1\r\na=rtpmap:103 ISAC/16000\r\na=rtpmap:104 ISAC/32000\r\na=rtpmap:9 G722/8000\r\na=rtpmap:102 ILBC/8000\r\na=rtpmap:0 PCMU/8000\r\na=rtpmap:8 PCMA/8000\r\na=rtpmap:106 CN/32000\r\na=rtpmap:105 CN/16000\r\na=rtpmap:13 CN/8000\r\na=rtpmap:110 telephone-event/48000\r\na=rtpmap:112 telephone-event/32000\r\na=rtpmap:113 telephone-event/16000\r\na=rtpmap:126 telephone-event/8000\r\na=ssrc:2727183610 cname:WrRhnOANDOId9+uz\r\na=ssrc:2727183610 msid:102 101\r\na=ssrc:2727183610 mslabel:102\r\na=ssrc:2727183610 label:101\r\nm=video 9 UDP/TLS/RTP/SAVPF 96 97 98 99 100 101 127 123 125 122 124\r\nc=IN IP4 0.0.0.0\r\na=rtcp:9 IN IP4 0.0.0.0\r\na=ice-ufrag:yqnf\r\na=ice-pwd:xZdM6CbUsYvCv9z6WOQxxcj1\r\na=ice-options:trickle renomination\r\na=fingerprint:sha-256 0C:A5:16:CD:C8:E2:AB:73:0E:08:90:95:2F:A3:35:0F:1F:CB:49:96:41:CD:53:B4:73:FC:8A:D7:1A:B8:02:21\r\na=setup:actpass\r\na=mid:video\r\na=extmap:14 urn:ietf:params:rtp-hdrext:toffset\r\na=extmap:13 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\na=extmap:3 urn:3gpp:video-orientation\r\na=extmap:2 http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01\r\na=extmap:5 http://www.webrtc.org/experiments/rtp-hdrext/playout-delay\r\na=extmap:6 http://www.webrtc.org/experiments/rtp-hdrext/video-content-type\r\na=extmap:7 http://www.webrtc.org/experiments/rtp-hdrext/video-timing\r\na=extmap:8 http://tools.ietf.org/html/draft-ietf-avtext-framemarking-07\r\na=extmap:9 http://www.webrtc.org/experiments/rtp-hdrext/color-space\r\na=sendrecv\r\na=rtcp-mux\r\na=rtcp-rsize\r\na=rtpmap:96 H264/90000\r\na=rtcp-fb:96 goog-remb\r\na=rtcp-fb:96 transport-cc\r\na=rtcp-fb:96 ccm fir\r\na=rtcp-fb:96 nack\r\na=rtcp-fb:96 nack pli\r\na=fmtp:96 level-asymmetry-allowed=1;packetization-mode=1;profile-level-id=42e01f\r\na=rtpmap:97 rtx/90000\r\na=fmtp:97 apt=96\r\na=rtpmap:98 VP8/90000\r\na=rtcp-fb:98 goog-remb\r\na=rtcp-fb:98 transport-cc\r\na=rtcp-fb:98 ccm fir\r\na=rtcp-fb:98 nack\r\na=rtcp-fb:98 nack pli\r\na=rtpmap:99 rtx/90000\r\na=fmtp:99 apt=98\r\na=rtpmap:100 VP9/90000\r\na=rtcp-fb:100 goog-remb\r\na=rtcp-fb:100 transport-cc\r\na=rtcp-fb:100 ccm fir\r\na=rtcp-fb:100 nack\r\na=rtcp-fb:100 nack pli\r\na=rtpmap:101 rtx/90000\r\na=fmtp:101 apt=100\r\na=rtpmap:127 H265/90000\r\na=rtcp-fb:127 goog-remb\r\na=rtcp-fb:127 transport-cc\r\na=rtcp-fb:127 ccm fir\r\na=rtcp-fb:127 nack\r\na=rtcp-fb:127 nack pli\r\na=rtpmap:123 rtx/90000\r\na=fmtp:123 apt=127\r\na=rtpmap:125 red/90000\r\na=rtpmap:122 rtx/90000\r\na=fmtp:122 apt=125\r\na=rtpmap:124 ulpfec/90000\r\na=ssrc-group:FID 1041555940 3283356583\r\na=ssrc:1041555940 cname:WrRhnOANDOId9+uz\r\na=ssrc:1041555940 msid:102 100\r\na=ssrc:1041555940 mslabel:102\r\na=ssrc:1041555940 label:100\r\na=ssrc:3283356583 cname:WrRhnOANDOId9+uz\r\na=ssrc:3283356583 msid:102 100\r\na=ssrc:3283356583 mslabel:102\r\na=ssrc:3283356583 label:100\r\n',
        'v=0\r\no=- 8058700252215630093 2 IN IP4 127.0.0.1\r\ns=-\r\nt=0 0\r\na=group:BUNDLE audio video\r\na=msid-semantic: WMS 102\r\nm=audio 9 UDP/TLS/RTP/SAVPF 111 103 104 9 102 0 8 106 105 13 110 112 113 126\r\nc=IN IP4 0.0.0.0\r\na=rtcp:9 IN IP4 0.0.0.0\r\na=ice-ufrag:wxVq\r\na=ice-pwd:de5pHehq/nghdxUDjq5oRLqw\r\na=ice-options:trickle renomination\r\na=fingerprint:sha-256 22:17:B2:B9:28:2B:57:BC:D9:D8:A4:DE:83:39:1F:3B:0E:18:0D:4D:DB:43:71:FC:93:E7:C8:AF:22:7A:34:EC\r\na=setup:actpass\r\na=mid:audio\r\na=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\na=extmap:2 http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01\r\na=sendrecv\r\na=rtcp-mux\r\na=rtpmap:111 opus/48000/2\r\na=rtcp-fb:111 transport-cc\r\na=fmtp:111 minptime=10;useinbandfec=1\r\na=rtpmap:103 ISAC/16000\r\na=rtpmap:104 ISAC/32000\r\na=rtpmap:9 G722/8000\r\na=rtpmap:102 ILBC/8000\r\na=rtpmap:0 PCMU/8000\r\na=rtpmap:8 PCMA/8000\r\na=rtpmap:106 CN/32000\r\na=rtpmap:105 CN/16000\r\na=rtpmap:13 CN/8000\r\na=rtpmap:110 telephone-event/48000\r\na=rtpmap:112 telephone-event/32000\r\na=rtpmap:113 telephone-event/16000\r\na=rtpmap:126 telephone-event/8000\r\na=ssrc:889008323 cname:E+ZJf5icu+lOI/w7\r\na=ssrc:889008323 msid:102 101\r\na=ssrc:889008323 mslabel:102\r\na=ssrc:889008323 label:101\r\nm=video 9 UDP/TLS/RTP/SAVPF 96 97 98 99 100 101 127 123 125 122 124\r\nc=IN IP4 0.0.0.0\r\na=rtcp:9 IN IP4 0.0.0.0\r\na=ice-ufrag:wxVq\r\na=ice-pwd:de5pHehq/nghdxUDjq5oRLqw\r\na=ice-options:trickle renomination\r\na=fingerprint:sha-256 22:17:B2:B9:28:2B:57:BC:D9:D8:A4:DE:83:39:1F:3B:0E:18:0D:4D:DB:43:71:FC:93:E7:C8:AF:22:7A:34:EC\r\na=setup:actpass\r\na=mid:video\r\na=extmap:14 urn:ietf:params:rtp-hdrext:toffset\r\na=extmap:13 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\na=extmap:3 urn:3gpp:video-orientation\r\na=extmap:2 http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01\r\na=extmap:5 http://www.webrtc.org/experiments/rtp-hdrext/playout-delay\r\na=extmap:6 http://www.webrtc.org/experiments/rtp-hdrext/video-content-type\r\na=extmap:7 http://www.webrtc.org/experiments/rtp-hdrext/video-timing\r\na=extmap:8 http://tools.ietf.org/html/draft-ietf-avtext-framemarking-07\r\na=extmap:9 http://www.webrtc.org/experiments/rtp-hdrext/color-space\r\na=sendrecv\r\na=rtcp-mux\r\na=rtcp-rsize\r\na=rtpmap:96 H264/90000\r\na=rtcp-fb:96 goog-remb\r\na=rtcp-fb:96 transport-cc\r\na=rtcp-fb:96 ccm fir\r\na=rtcp-fb:96 nack\r\na=rtcp-fb:96 nack pli\r\na=fmtp:96 level-asymmetry-allowed=1;packetization-mode=1;profile-level-id=42e01f\r\na=rtpmap:97 rtx/90000\r\na=fmtp:97 apt=96\r\na=rtpmap:98 VP8/90000\r\na=rtcp-fb:98 goog-remb\r\na=rtcp-fb:98 transport-cc\r\na=rtcp-fb:98 ccm fir\r\na=rtcp-fb:98 nack\r\na=rtcp-fb:98 nack pli\r\na=rtpmap:99 rtx/90000\r\na=fmtp:99 apt=98\r\na=rtpmap:100 VP9/90000\r\na=rtcp-fb:100 goog-remb\r\na=rtcp-fb:100 transport-cc\r\na=rtcp-fb:100 ccm fir\r\na=rtcp-fb:100 nack\r\na=rtcp-fb:100 nack pli\r\na=rtpmap:101 rtx/90000\r\na=fmtp:101 apt=100\r\na=rtpmap:127 H265/90000\r\na=rtcp-fb:127 goog-remb\r\na=rtcp-fb:127 transport-cc\r\na=rtcp-fb:127 ccm fir\r\na=rtcp-fb:127 nack\r\na=rtcp-fb:127 nack pli\r\na=rtpmap:123 rtx/90000\r\na=fmtp:123 apt=127\r\na=rtpmap:125 red/90000\r\na=rtpmap:122 rtx/90000\r\na=fmtp:122 apt=125\r\na=rtpmap:124 ulpfec/90000\r\na=ssrc-group:FID 414877678 1809551269\r\na=ssrc:414877678 cname:E+ZJf5icu+lOI/w7\r\na=ssrc:414877678 msid:102 100\r\na=ssrc:414877678 mslabel:102\r\na=ssrc:414877678 label:100\r\na=ssrc:1809551269 cname:E+ZJf5icu+lOI/w7\r\na=ssrc:1809551269 msid:102 100\r\na=ssrc:1809551269 mslabel:102\r\na=ssrc:1809551269 label:100\r\n',
        'v=0\r\no=- 4736694148830170425 2 IN IP4 127.0.0.1\r\ns=-\r\nt=0 0\r\na=group:BUNDLE audio video\r\na=msid-semantic: WMS 102\r\nm=audio 9 UDP/TLS/RTP/SAVPF 111 103 104 9 102 0 8 106 105 13 110 112 113 126\r\nc=IN IP4 0.0.0.0\r\na=rtcp:9 IN IP4 0.0.0.0\r\na=ice-ufrag:9xYB\r\na=ice-pwd:6mP5ZDYg/ckEUtji+pKlNjV7\r\na=ice-options:trickle renomination\r\na=fingerprint:sha-256 01:1C:AF:71:12:5A:73:91:61:B2:93:96:61:4E:02:B5:EA:9C:CF:FA:88:03:A2:1F:D5:B1:8E:72:2D:D0:A1:05\r\na=setup:actpass\r\na=mid:audio\r\na=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\na=extmap:2 http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01\r\na=sendrecv\r\na=rtcp-mux\r\na=rtpmap:111 opus/48000/2\r\na=rtcp-fb:111 transport-cc\r\na=fmtp:111 minptime=10;useinbandfec=1\r\na=rtpmap:103 ISAC/16000\r\na=rtpmap:104 ISAC/32000\r\na=rtpmap:9 G722/8000\r\na=rtpmap:102 ILBC/8000\r\na=rtpmap:0 PCMU/8000\r\na=rtpmap:8 PCMA/8000\r\na=rtpmap:106 CN/32000\r\na=rtpmap:105 CN/16000\r\na=rtpmap:13 CN/8000\r\na=rtpmap:110 telephone-event/48000\r\na=rtpmap:112 telephone-event/32000\r\na=rtpmap:113 telephone-event/16000\r\na=rtpmap:126 telephone-event/8000\r\na=ssrc:1016307788 cname:hLXsr+8FlNBwrVeo\r\na=ssrc:1016307788 msid:102 101\r\na=ssrc:1016307788 mslabel:102\r\na=ssrc:1016307788 label:101\r\nm=video 9 UDP/TLS/RTP/SAVPF 96 97 98 99 100 101 127 123 125 122 124\r\nc=IN IP4 0.0.0.0\r\na=rtcp:9 IN IP4 0.0.0.0\r\na=ice-ufrag:9xYB\r\na=ice-pwd:6mP5ZDYg/ckEUtji+pKlNjV7\r\na=ice-options:trickle renomination\r\na=fingerprint:sha-256 01:1C:AF:71:12:5A:73:91:61:B2:93:96:61:4E:02:B5:EA:9C:CF:FA:88:03:A2:1F:D5:B1:8E:72:2D:D0:A1:05\r\na=setup:actpass\r\na=mid:video\r\na=extmap:14 urn:ietf:params:rtp-hdrext:toffset\r\na=extmap:13 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\na=extmap:3 urn:3gpp:video-orientation\r\na=extmap:2 http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01\r\na=extmap:5 http://www.webrtc.org/experiments/rtp-hdrext/playout-delay\r\na=extmap:6 http://www.webrtc.org/experiments/rtp-hdrext/video-content-type\r\na=extmap:7 http://www.webrtc.org/experiments/rtp-hdrext/video-timing\r\na=extmap:8 http://tools.ietf.org/html/draft-ietf-avtext-framemarking-07\r\na=extmap:9 http://www.webrtc.org/experiments/rtp-hdrext/color-space\r\na=sendrecv\r\na=rtcp-mux\r\na=rtcp-rsize\r\na=rtpmap:96 H264/90000\r\na=rtcp-fb:96 goog-remb\r\na=rtcp-fb:96 transport-cc\r\na=rtcp-fb:96 ccm fir\r\na=rtcp-fb:96 nack\r\na=rtcp-fb:96 nack pli\r\na=fmtp:96 level-asymmetry-allowed=1;packetization-mode=1;profile-level-id=42e01f\r\na=rtpmap:97 rtx/90000\r\na=fmtp:97 apt=96\r\na=rtpmap:98 VP8/90000\r\na=rtcp-fb:98 goog-remb\r\na=rtcp-fb:98 transport-cc\r\na=rtcp-fb:98 ccm fir\r\na=rtcp-fb:98 nack\r\na=rtcp-fb:98 nack pli\r\na=rtpmap:99 rtx/90000\r\na=fmtp:99 apt=98\r\na=rtpmap:100 VP9/90000\r\na=rtcp-fb:100 goog-remb\r\na=rtcp-fb:100 transport-cc\r\na=rtcp-fb:100 ccm fir\r\na=rtcp-fb:100 nack\r\na=rtcp-fb:100 nack pli\r\na=rtpmap:101 rtx/90000\r\na=fmtp:101 apt=100\r\na=rtpmap:127 H265/90000\r\na=rtcp-fb:127 goog-remb\r\na=rtcp-fb:127 transport-cc\r\na=rtcp-fb:127 ccm fir\r\na=rtcp-fb:127 nack\r\na=rtcp-fb:127 nack pli\r\na=rtpmap:123 rtx/90000\r\na=fmtp:123 apt=127\r\na=rtpmap:125 red/90000\r\na=rtpmap:122 rtx/90000\r\na=fmtp:122 apt=125\r\na=rtpmap:124 ulpfec/90000\r\na=ssrc-group:FID 138599044 405807346\r\na=ssrc:138599044 cname:hLXsr+8FlNBwrVeo\r\na=ssrc:138599044 msid:102 100\r\na=ssrc:138599044 mslabel:102\r\na=ssrc:138599044 label:100\r\na=ssrc:405807346 cname:hLXsr+8FlNBwrVeo\r\na=ssrc:405807346 msid:102 100\r\na=ssrc:405807346 mslabel:102\r\na=ssrc:405807346 label:100\r\n']
    sdp_offer_inc = 0

    def __init__(self, account, pwd, server_url='http://172.25.12.50:5000'):
        self.account = account
        self.password = pwd
        self.server_url = server_url
        self.signalAddr = ''
        self.room_id = None
        global log_init
        if not log_init:
            log_init = True
            # logger.add("log.log", format="{time:YYYY-MM-DD HH:mm:ss} | {level} | {message}", filter="", level="INFO", rotation="10 MB", encoding='utf-8')
            # logger.add("../log.log", filter="", level="INFO", rotation="10 MB", encoding='utf-8')
        logger.info('server_url:' + server_url)



    def loginAndAccessIn(self, **kwargs):
        self.login()
        return self.__accessIn(**kwargs)

    def loginHDCAndAccessIn(self, **kwargs):
        self.loginHDC()
        return self.__accessIn(**kwargs)

    def login(self):
        try:
            uuid, token = self.__getToken(self.account, self.password)
            if token == '':
                logger.error('getToken failed. account:' + self.account + ' password:' + self.password)
                return None
            self.uuid = uuid
            self.token = token
            self.__src_uuid = self.uuid

            uuid, userId = self.__userLogin(uuid, token)
            if uuid == '':
                logger.error('userLogin failed.')
                return None
            self.userId = userId

            self.signalAddr = self.__getSignalAddrList(uuid, token)
            if self.signalAddr == '':
                logger.error('getSignalAddr failed.')
                return None

            self.signalAddr += "/openvidu"
            logger.info('{} signalAddr:{}', uuid, self.signalAddr)
        except Exception as e:
            logger.error(e)
        return self.uuid

    def loginHDC(self):
        try:
            uuid, token = self.__getToken(self.account, self.password)
            if token == '':
                logger.error('getToken failed. account:' + self.account + ' password:' + self.password)
                return None
            self.uuid = uuid
            self.token = token
            self.__src_uuid = self.uuid
            # uuid, userId = self.__userLogin(uuid, token)
            # if uuid == '':
            #     logger.error('userLogin failed.')
            #     return None
            # self.userId = userId

            self.signalAddr = self.__getSignalAddrList(uuid, token)
            if self.signalAddr == '':
                logger.error('getSignalAddr failed.')
                return None

            self.signalAddr += "/openvidu"
        except Exception as e:
            logger.error(e)
        return self.uuid

    def safeOut(self):
        """ 安全退出,释放资源 """
        logger.info("================== " + self.uuid + " safe outing ==================")
        if self.room_id is not None:
            if len(self.room_id) == 11 and self.room_id == self.uuid:
                self.close_room(self.room_id)
            elif len(self.room_id) == 9 or len(self.room_id) == 8:
                self.close_room(self.room_id)
        if self.wsClient is not None:
            self.request("accessOut", {})
            self.wsClient.close()
        logger.info("****************** " + self.uuid + " safe outed ******************")

    def logout(self):
        self.wsClient.close()

    def listen_notify(self, method, func, *args):
        self.wsClient.notify_listen_funcs[method] = func
        self.wsClient.notify_listen_args[method] = args
        pass

    def publish_video(self, stream_type):
        params = {}
        params['sdpOffer'] = SdClient.get_offer()
        params['audioOnly'] = False
        params['doLoopback'] = False
        params['hasAudio'] = True
        params['hasVideo'] = True
        params['micStatus'] = 'off'
        params['streamType'] = stream_type
        params['videoStatus'] = "on"
        return self.request('publishVideo', params)

    def unpublish_video(self, stream_id):
        return self.request('unpublishVideo', {'publishId': stream_id})

    def subscribe_video(self, uuid, stream_type, stream_id, stream_mode='SFU_SHARING'):
        params = {}
        params['sdpOffer'] = SdClient.get_offer()
        params['streamType'] = stream_type
        params['senderUuid'] = uuid
        params['publishId'] = stream_id
        params['streamMode'] = stream_mode
        return self.request('subscribeVideo', params)

    def subscribe_mcu(self, stream_id):
        params = {}
        params['sdpOffer'] = SdClient.get_offer()
        params['streamMode'] = 'MIX_MAJOR'
        params['publishId'] = stream_id
        return self.request('subscribeVideo', params)

    def unsubscribe_video(self, stream_id):
        return self.request('unsubscribeVideo', {'subscribeId': stream_id})

    def on_ice_candidate(self, stream_id):
        params = {}
        params['candidate'] = SdClient.get_offer()
        params['endpointName'] = stream_id
        params['sdpMLineIndex'] = "0"
        params['sdpMid'] = "audio"
        return self.request('onIceCandidate', params)

    @classmethod
    def get_offer(cls):
        cls.sdp_offer_inc += 1
        return cls.sdpOffers[cls.sdp_offer_inc % 5]

    def request(self, method, params, timeout=2000):
        return self.wsClient.request(method, params, timeout)

    def createRoom(self, room_id, subject, room_id_type='personal', **args):
        self.room_id = room_id
        params = {}
        params['allowPartOperMic'] = 'on'
        params['allowPartOperShare'] = 'on'
        params['conferenceMode'] = "SFU"
        params['duration'] = 120
        params['micStatusInRoom'] = "on"
        params['password'] = ''
        params['quietStatusInRoom'] = 'smart'
        params['roomId'] = room_id
        params['roomIdType'] = room_id_type
        params['sharePowerInRoom'] = 'on'
        params['subject'] = subject
        params['useIdInRoom'] = 'allParticipant'
        params['videoStatusInRoom'] = 'on'
        params['ruid'] = '' #会议的唯一id

        for k, v in args.items():
            params[k] = v

        re = self.wsClient.request("createRoom", params)
        if re[0] == 0:
            self.room_id = re[1]['roomId']
            logger.info('create room success room_id = ' + self.room_id)
        return re

    def joinRoom(self, room_id, **kwargs):
        self.room_id = room_id
        params = {}
        params['conferenceMode'] = 'SFU'
        params['dataChannels'] = 'false'
        params['isReconnected'] = False
        params['duration'] = 120
        params['joinType'] = "active"
        params['metadata'] = '{"account":' + self.uuid + '}'
        params['micStatus'] = "on"
        params['password'] = ""
        params['platform'] = "windows"
        params['role'] = "PUBLISHER"
        params['ruid'] = ""
        params['session'] = self.room_id
        params['streamType'] = "MAJOR"
        params['useIdInRoom'] = "allParticipants"  # SessionPresetUseIDEnum
        params['videoStatus'] = "on"
        params['secret'] = ""

        for k, v in kwargs.items():
            params[k] = v

        re = self.wsClient.request("joinRoom", params)
        return re

    def leave_room(self, room_id):
        params = {}
        params['roomId'] = room_id
        params['sourceId'] = self.uuid  # 2.0 需要砍掉
        params['streamType'] = "MAJOR"  # 2.0 需要砍掉
        self.room_id = None
        return self.wsClient.request("leaveRoom", params)

    def close_room(self, room_id):
        params = {}
        params['roomId'] = room_id
        self.room_id = None
        return self.wsClient.request("closeRoom", params)

    def collecting_notify(self):
        """ 调用这个接口后，开始主动将平台的通知收集起来 """
        if self.wsClient is not None:
            self.wsClient.isCollecting = True
            self.clear_notify()

    def get_notify_list(self):
        """ 读取缓存的通知 """
        if self.wsClient is not None:
            return self.wsClient.notify_list
        return []

    def search_notify_list(self, method):
        """ 搜索缓存中的通知,返回匹配method的json数组 """
        result = []
        list = self.get_notify_list()
        for msg in list:
            try:
                j = json.loads(msg)
                if j['method'] == method:
                    result.append(j)
            except Exception as e:
                logger.error('search_notify_list error message ' + msg, e)
        return result

    def find_any_notify(self, method, timeout=2000):
        """ 搜索缓存中的通知,返回匹配method的json """
        result = {}
        beginMs = int(SDUtil.GetUtcMs())
        while int(SDUtil.GetUtcMs()) - beginMs < timeout:
            list = self.get_notify_list()
            for msg in list:
                try:
                    j = json.loads(msg)
                    if j['method'] == method:
                        return j
                except Exception as e:
                    logger.error('find_any_notify error message ' + msg, e)
                    return result
            time.sleep(0.01)
        return result

    def has_notify(self, method):
        """ 搜索缓存中的通知,是否包该改通知 """
        list = self.get_notify_list()
        for l in list:
            j = json.loads(l)
            if j['method'] == method:
                return True
        return False

    def has_notify_sync(self, method, timeout=2):
        """ 搜索缓存中的通知,是否包该改通知,同步一直简单到有结果或超时 """
        beginMs = int(SDUtil.GetUtcMs())
        while int(SDUtil.GetUtcMs()) - beginMs < timeout:
            result = self.has_notify(method)
            if result:
                return result
            time.sleep(0.01)
        return False

    def clear_notify(self):
        self.wsClient.notify_list = []

    def is_close(self):
        """ 客户端是否关闭的 """
        return not self.wsClient.ws.keep_running

    def close_ping_pong(self):
        self.wsClient.closePingPong()

    def __accessIn(self, **args):
        self.wsClient = WsClient(self.uuid, self.signalAddr)
        params = {}
        params['account'] = self.uuid
        params['token'] = self.token
        params['udid'] = 'mock_client_' + str(random.randint(1000000, 9000000))
        params['deviceVersion'] = '1378'
        params['forceLogin'] = False
        params['accessType'] = 'terminal'
        params['userType'] = 'register'
        params['deviceModel'] = 'mock_client_v1'

        functionalityJson = {}
        functionalityJson['MultiCastPlay'] = False
        functionalityJson['MultiCastPlayUsable'] = False
        functionalityJson['ScreenShare'] = False
        functionalityJson['ScreenShareUsable'] = False
        params['functionality'] = json.dumps(functionalityJson)

        params['clientTimestamp'] = int(SDUtil.GetUtcMs())
        params['type'] = 'W'

        for k, v in args.items():
            params[k] = v

        return self.wsClient.request("accessIn", params)

    def __userLogin(self, uuid, token):
        userLoginParam = {"platform": "windows"}
        userId = ''

        httpClient = SDHTTPClient(self.server_url, uuid, token)
        ret, res = httpClient.request(self.userLogin, userLoginParam)
        if ret and res['errorCode'] == 0:
            uuid = res['result']['account']
            userId = res['result']['userId']
        else:
            logger.error('userLogin failed' + json.dumps(res, sort_keys=True, indent=4, ensure_ascii=False))
        return uuid, userId

    def __getSignalAddrList(self, uuid, token):
        signalAddr = ''
        httpClient = SDHTTPClient(self.server_url, uuid, token)
        ret, res = httpClient.request(self.getSignalAddrList, '')
        if ret and res['errorCode'] == 0:
            # TODO. Fixme, it's a list.
            ## 使用pre-meeting时，使用内网进行压测，不使用公网流量
            signalAddr = res['result']['signalAddrList'][0]
            if "pre-meeting.sensedigit.com" in signalAddr:
                signalAddr = 'ws://172.18.153.220:4443'
            elif "120.79.185.46" in signalAddr:
                signalAddr = 'ws://172.18.153.227:4443'
        else:
            logger.error('failed.' + json.dumps(res, sort_keys=True, indent=4, ensure_ascii=False))
        return signalAddr

    def __getToken(self, account, password):
        httpClient = SDHTTPClient(self.server_url, account, SDUtil.MD5(password.encode('utf-8')))
        ret, res = httpClient.request(self.getTokenURL, '')
        uuid = ''
        token = ''
        if ret and res['errorCode'] == 0:
            uuid = res['result']['account']
            token = res['result']['token']
        else:
            logger.error('getToken failed.' + json.dumps(res, sort_keys=True, indent=4, ensure_ascii=False))
        return uuid, token

    def __getAccount(self, account, serial_number):
        httpClient = SDHTTPClient(self.server_url, account, serial_number)
        ret, res = httpClient.request(self.getTokenURL, '')
        uuid = ''
        token = ''
        if ret and res['errorCode'] == 0:
            uuid = res['result']['account']
            token = res['result']['token']
        else:
            logger.error('getToken failed.' + json.dumps(res, sort_keys=True, indent=4, ensure_ascii=False))
        return uuid, token

#zyx个人练习———————————————————————请忽略
    def getMemberDetails(self, uuid):
        self.uuid = uuid
        re = self.wsClient.request("getMemberDetails", {"uuid":uuid})
        return re

#zyx个人练习———————————————————————请忽略
    def getUploadToken(self, type):
        self.type = type
        re = self.wsClient.request("getUploadToken", {"type":type})
        return re




class WsClient:

    # 收集的通知将放再这个列表中
    def __init__(self, uuid, signalAddr):
        self.notify_listen_funcs = {}
        self.notify_listen_args = {}
        self.notify_list = []
        self.uuid = uuid
        self.msgRespMap = {}
        websocket.enableTrace(False)
        self.traceId = 1
        self.lockMsgId = threading.Lock()
        self.is_close = False
        # signalAddr = 'ws://172.25.11.200:4443/openvidu'
        self.ws = websocket.WebSocketApp(signalAddr,
                                         on_open=self.on_open,
                                         on_message=self.on_message,
                                         on_error=self.on_error,
                                         on_close=self.on_close)
        # self.wst = threading.Thread(target=self.ws.run_forever)
        self.wst = threading.Thread(target=self.run_forever)
        self.wst.daemon = True
        self.wst.start()
        time.sleep(0.1)
        self.ping = True  # ping pong
        self.pt = threading.Thread(target=self.ping_method)
        self.pt.daemon = True
        self.pt.start()
        # 是否启用收集通知
        self.isCollecting = False

    def run_forever(self):
        self.ws.run_forever(sslopt={"cert_reqs": ssl.CERT_NONE}, ping_interval=10, ping_timeout=5)

    def ping_method(self):
        time.sleep(10)
        while self.ping and self.ws.keep_running:
            msgId = self.__getMsgId()
            content = {}
            method = 'ping'
            try:
                content["id"] = msgId
                content["method"] = method
                content["jsonrpc"] = "2.0"
                content['params'] = {}
                json_params = json.dumps(content)
                self.ws.send(json_params)
                # logger.info(self.uuid + " request " + str(json_params))
            except Exception as e:
                if "is already closed" in str(e):
                    break
            time.sleep(10)
        # logger.info('shutdown ping thread')

    def on_message(self, ws, message):
        if '"value":"pong"' in message:
            return
        logger.info(self.uuid + " on_message " + message)
        result = json.loads(message)
        if 'id' in result:
            self.msgRespMap[str(result['id'])] = message
        elif self.isCollecting:
            self.notify_list.append(message)
        for method, func in self.notify_listen_funcs.items():
            if method == result['method']:
                self.pt = threading.Thread(target=func, args=(result, *self.notify_listen_args[method]), daemon=True)
                self.pt.start()

    def on_error(self, ws, error):
        # print("on_error " + str(error))
        pass

    def on_close(self, message):
        logger.info(self.uuid + ' ws connect close')

    def on_open(self, message):
        logger.info(self.uuid + ' ws connect open')

    def request(self, method, params, timeout=2000):
        msgId = self.__getMsgId()
        content = {}
        try:
            content["id"] = msgId
            content["method"] = method
            content["jsonrpc"] = "2.0"
            content['params'] = params
            json_params = json.dumps(content)
            logger.info(self.uuid + " request " + str(json_params))
            self.ws.send(json_params)
            # self.ws.send(content)
        except Exception as e:
            logger.error('msgId:' + msgId + ' method:' + method + ' request:error ' + str(e), e)
            return {}
        return self.response(msgId, timeout)

    def response(self, msgId, timeoutMs=2000):
        try:
            beginMs = int(SDUtil.GetUtcMs())

            while int(SDUtil.GetUtcMs()) - beginMs < timeoutMs:
                # self.lockMsgResp.acquire()
                if (msgId in self.msgRespMap.keys()):
                    respMsg = self.msgRespMap.pop(msgId)
                    res = json.loads(respMsg)
                    if 'result' in res:
                        return 0, res['result']
                    else:
                        return int(res['error']['code']), res['error']
                time.sleep(0.01)
        except Exception as e:
            logger.error('exception:' + str(e))
        logger.info("{} request timeout msgId {}", self.uuid, msgId)
        return -1, {'message': 'request timeout msgId'}

    def closePingPong(self):
        self.ping = False

    def close(self):
        self.is_close = True
        self.ws.close()

    def __getMsgId(self):
        self.lockMsgId.acquire()
        self.traceId += 1
        msgId = self.traceId
        self.lockMsgId.release()
        return str(msgId)


