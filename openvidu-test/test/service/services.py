import time

from common.mock_client import SdClient


class ApptService:

    def __init__(self, client: SdClient):
        self.client = client
        self.uuid = client.uuid

    def create_person_appt(self, *invites):
        params = self.__constructParams(self.uuid, 'personal', invites)
        return self.client.request('createAppointmentRoom', params=params)

    def __constructParams(self, room_id, roomIdType, invites):
        param = self.__get_defalut_params()
        param['roomId'] = room_id
        param['startTime'] = (int(time.time()) + 3600) * 1000
        param['subject'] = self.client.uuid + '预约的会议'
        param['roomIdType'] = roomIdType
        if len(invites) == 0:
            invites = [self.uuid]
        param['participants'] = invites
        return param

    def __get_defalut_params(self):
        return {
            "autoCall": True,
            "conferenceMode": "SFU",
            "desc": "",
            "duration": 60,
            "moderatorJoinConfig": {
                "micStatus": "on",
                "videoStatus": "on"
            },
            "moderatorPassword": "345680",  # 2.0 干掉
            "moderatorRoomId": "",
            "participantJoinConfig": {
                "micStatus": "on",
                "videoStatus": "on"
            },
            "participants": [
                "80103600005"
            ],
            "password": "",
            "roomCapacity": 8,
            "roomId": "",
            "roomIdType": "random",
            "startTime": "1625109600000",
            "subject": "a005预约的会议"
        }


class MeetingService:
    def __init__(self, client: SdClient, room_id: str):
        self.client = client
        self.uuid = client.uuid
        self.room_id = room_id

    def get_participants(self, search_type, target_ids=None):
        """
        获取与会者列表
        "searchType":"exact/list/publisher/raisingHands",
        """
        if target_ids is None:
            target_ids = []
        params = {}
        params['roomId'] = self.room_id
        params['searchType'] = search_type
        params['targetIds'] = target_ids
        params['order'] = None
        params['reverse'] = None
        params['limit'] = None
        params['reducedMode'] = None
        params['fields'] = None
        return self.client.request('getParticipants', params)

    def get_not_finished_room(self):
        """  getNotFinishedRoom """
        return self.client.request('getNotFinishedRoom', {})

    def invite_participant(self, target_id: list):
        """  inviteParticipant 邀请入会 """
        return self.client.request('inviteParticipant', {'roomId': self.room_id, 'sourceId': self.uuid,
                                                         'targetId': target_id,
                                                         'expireTime': int(time.time() + 60) * 1000})

    def set_roll_call(self, target_uuid):
        return self.client.request("setRollCall", {"roomId": self.room_id, "originator": self.client.uuid,
                                                   "targetId": target_uuid})

    def end_roll_call(self, target_uuid):
        return self.client.request("endRollCall", {"roomId": self.room_id, "originator": self.client.uuid,
                                                   "targetId": target_uuid})

    def replace_roll_call(self, start_target_uuid, end_target_uuid):
        return self.client.request("replaceRollCall", {"roomId": self.room_id, "originator": self.client.uuid,
                                                       "endTargetId": end_target_uuid,
                                                       "startTargetId": start_target_uuid})
