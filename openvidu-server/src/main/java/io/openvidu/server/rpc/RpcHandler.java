/*
 * (C) Copyright 2017-2019 OpenVidu (https://openvidu.io/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.openvidu.server.rpc;

import com.google.gson.*;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.OpenViduException.Code;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.cache.CacheManage;
import io.openvidu.server.common.dao.*;
import io.openvidu.server.common.enums.*;
import io.openvidu.server.common.manage.AuthorizationManage;
import io.openvidu.server.common.manage.DepartmentManage;
import io.openvidu.server.common.manage.DeviceManage;
import io.openvidu.server.common.manage.UserManage;
import io.openvidu.server.common.pojo.*;
import io.openvidu.server.config.OpenviduConfig;
import io.openvidu.server.core.*;
import io.openvidu.server.kurento.core.KurentoParticipant;
import io.openvidu.server.utils.GeoLocation;
import io.openvidu.server.utils.GeoLocationByIp;
import io.openvidu.server.utils.StringUtil;
import org.kurento.jsonrpc.DefaultJsonRpcHandler;
import org.kurento.jsonrpc.Session;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.message.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class RpcHandler extends DefaultJsonRpcHandler<JsonObject> {

	private static final Logger log = LoggerFactory.getLogger(RpcHandler.class);

	private static final Gson gson = new GsonBuilder().create();

	@Resource
	RpcHandlerFactory rpcHandlerFactory;

	@Autowired
	OpenviduConfig openviduConfig;

	@Autowired
	GeoLocationByIp geoLocationByIp;

	@Autowired
	SessionManager sessionManager;

	@Autowired
	RpcNotificationService notificationService;

	@Autowired
	CacheManage cacheManage;

	@Resource
	UserMapper userMapper;

	@Resource
    ConferenceMapper conferenceMapper;

	@Resource
	DeviceMapper deviceMapper;

	@Resource
	DeviceDeptMapper deviceDeptMapper;

	@Resource
	UserDeptMapper userDeptMapper;

	@Resource
	DepartmentMapper depMapper;

	@Autowired
    AuthorizationManage authorizationManage;

	@Autowired
	DepartmentManage departmentManage;

	@Autowired
    DeviceManage deviceManage;

	@Autowired
    UserManage userManage;

	private ConcurrentMap<String, Boolean> webSocketEOFTransportError = new ConcurrentHashMap<>();

	@Override
	public void handleRequest(Transaction transaction, Request<JsonObject> request) throws Exception {

		String participantPrivateId;
		try {
			participantPrivateId = transaction.getSession().getSessionId();
		} catch (Throwable e) {
			log.error("Error getting WebSocket session ID from transaction {}", transaction, e);
			throw e;
		}

		log.info("WebSocket request session #{} - Request: {}", participantPrivateId, request);

		RpcConnection rpcConnection = null;
		if (ProtocolElements.ACCESS_IN_METHOD.equals(request.getMethod())) {
			rpcConnection = notificationService.newRpcConnection(transaction, request);
		} else if (notificationService.getRpcConnection(participantPrivateId) == null) {
			// Throw exception if any method is called before 'joinRoom'
			log.warn(
					"No connection found for participant with privateId {} when trying to execute method '{}'. Method 'Session.connect()' must be the first operation called in any session",
					participantPrivateId, request.getMethod());
			throw new OpenViduException(Code.TRANSPORT_ERROR_CODE,
					"No connection found for participant with privateId " + participantPrivateId
							+ ". Method 'Session.connect()' must be the first operation called in any session");
		}

		// Authorization Check
//        if (authorizationManage.checkIfOperationPermitted(request.getMethod(), rpcConnection)) {
//            assert rpcConnection != null;
//            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
//                    null, ErrorCodeEnum.PERMISSION_LIMITED);
//            return;
//        }

		rpcConnection = notificationService.addTransaction(transaction, request);
		request.setSessionId(rpcConnection.getParticipantPrivateId());

		String sessionId = rpcConnection.getSessionId();
		if (sessionId == null && !ProtocolElements.FILTERS.contains(request.getMethod())) {
			log.warn(
					"No session information found for participant with privateId {} when trying to execute method '{}'. Method 'Session.connect()' must be the first operation called in any session",
					participantPrivateId, request.getMethod());
			throw new OpenViduException(Code.TRANSPORT_ERROR_CODE,
					"No session information found for participant with privateId " + participantPrivateId
							+ ". Method 'Session.connect()' must be the first operation called in any session");
		}

		transaction.startAsync();
		RpcAbstractHandler rpcAbstractHandler = rpcHandlerFactory.getRpcHandler(request.getMethod());
		rpcAbstractHandler.handRpcRequest(rpcConnection, request);

		/*switch (request.getMethod()) {
            case ProtocolElements.ACCESS_IN_METHOD:
                accessIn(rpcConnection, request);
                break;
            case ProtocolElements.CONFIRM_APPLY_FOR_LOGIN_METHOD:
                confirmApplyForLogin(rpcConnection, request);
                break;
            case ProtocolElements.ACCESS_OUT_METHOD:
                accessOut(rpcConnection, request);
				break;
			case ProtocolElements.CREATE_ROOM_METHOD:
				createRoom(rpcConnection, request);
				break;
			case ProtocolElements.SHARE_SCREEN_METHOD:
				shareScreen(rpcConnection, request);
				break;
			case ProtocolElements.STOP_SHARE_SCREEN_METHOD:
				stopShareScreen(rpcConnection, request);
				break;
			case ProtocolElements.GET_PARTICIPANTS_METHOD:
				getParticipants(rpcConnection, request);
				break;
			case ProtocolElements.SET_AUDIO_STATUS_METHOD:
				setAudioStatus(rpcConnection, request);
				break;
			case ProtocolElements.SET_VIDEO_STATUS_METHOD:
				setVideoStatus(rpcConnection, request);
				break;
            case ProtocolElements.RAISE_HAND_METHOD:
                raiseHand(rpcConnection, request);
                break;
			case ProtocolElements.PUT_DOWN_HAND_METHOD:
				putDownHand(rpcConnection, request);
				break;
			case ProtocolElements.LOCK_SESSION_METHOD:
				lockSession(rpcConnection, request);
				break;
			case ProtocolElements.UNLOCK_SESSION_METHOD:
				unlockSession(rpcConnection, request);
				break;
			case ProtocolElements.JOINROOM_METHOD:
				joinRoom(rpcConnection, request);
				break;
			case ProtocolElements.LEAVEROOM_METHOD:
				leaveRoom(rpcConnection, request);
				break;
			case ProtocolElements.PUBLISHVIDEO_METHOD:
				publishVideo(rpcConnection, request);
				break;
			case ProtocolElements.ONICECANDIDATE_METHOD:
				onIceCandidate(rpcConnection, request);
				break;
			case ProtocolElements.RECEIVEVIDEO_METHOD:
				receiveVideoFrom(rpcConnection, request);
				break;
			case ProtocolElements.UNSUBSCRIBEFROMVIDEO_METHOD:
				unsubscribeFromVideo(rpcConnection, request);
				break;
			case ProtocolElements.SENDMESSAGE_ROOM_METHOD:
				sendMessage(rpcConnection, request);
				break;
			case ProtocolElements.UNPUBLISHVIDEO_METHOD:
				unpublishVideo(rpcConnection, request);
				break;
			case ProtocolElements.STREAMPROPERTYCHANGED_METHOD:
				streamPropertyChanged(rpcConnection, request);
				break;
			case ProtocolElements.FORCEDISCONNECT_METHOD:
				forceDisconnect(rpcConnection, request);
				break;
			case ProtocolElements.FORCEUNPUBLISH_METHOD:
				forceUnpublish(rpcConnection, request);
				break;
			case ProtocolElements.APPLYFILTER_METHOD:
				applyFilter(rpcConnection, request);
				break;
			case ProtocolElements.EXECFILTERMETHOD_METHOD:
				execFilterMethod(rpcConnection, request);
				break;
			case ProtocolElements.REMOVEFILTER_METHOD:
				removeFilter(rpcConnection, request);
				break;
			case ProtocolElements.ADDFILTEREVENTLISTENER_METHOD:
				addFilterEventListener(rpcConnection, request);
				break;
			case ProtocolElements.REMOVEFILTEREVENTLISTENER_METHOD:
				removeFilterEventListener(rpcConnection, request);
				break;
			case ProtocolElements.CLOSE_ROOM_METHOD:
				closeRoom(rpcConnection, request);
				break;
			case ProtocolElements.INVITE_PARTICIPANT_METHOD:
				inviteParticipant(rpcConnection, request);
				break;
			case ProtocolElements.REFUSE_INVITE_METHOD:
				refuseInvite(rpcConnection, request);
				break;
			case ProtocolElements.GET_PRESET_INFO_METHOD:
				getPresetInfo(rpcConnection, request);
				break;
			case ProtocolElements.SET_AUDIO_SPEAKER_STATUS_METHOD:
				setAudioSpeakerStatus(rpcConnection, request);
				break;
			case ProtocolElements.SET_SHARE_POWER_METHOD:
				setSharePower(rpcConnection, request);
				break;
			case ProtocolElements.TRANSFER_MODERATOR_METHOD:
				transferModerator(rpcConnection, request);
				break;
			case ProtocolElements.SET_ROLL_CALL_METHOD:
				setRollCall(rpcConnection, request);
				break;
			case ProtocolElements.END_ROLL_CALL_METHOD:
				endRollCall(rpcConnection, request);
				break;
			case ProtocolElements.GET_ORG_METHOD:
				getOrgList(rpcConnection, request);
				break;
			case ProtocolElements.GET_USER_DEVICE_METHOD:
				getUserDeviceList(rpcConnection, request);
				break;
			case ProtocolElements.GET_DEVICE_INFO_METHOD:
				getDeviceInfo(rpcConnection, request);
				break;
			case ProtocolElements.UPDATE_DEVICE_INFO_METHOD:
				updateDeviceInfo(rpcConnection, request);
				break;
			case ProtocolElements.ROOM_DELAY_METHOD:
				roomDelay(rpcConnection, request);
				break;
			case ProtocolElements.GET_NOT_FINISHED_ROOM_METHOD:
				getNotFinishedRoom(rpcConnection, request);
				break;
			default:
				log.error("Unrecognized request {}", request);
				break;
		}*/
	}

    private void accessIn(RpcConnection rpcConnection, Request<JsonObject> request) {
	    /*String uuid = getStringParam(request, ProtocolElements.ACCESS_IN_UUID_PARAM);
	    String token = getStringParam(request, ProtocolElements.ACCESS_IN_TOKEN_PARAM);
		String deviceSerialNumber = getStringOptionalParam(request, ProtocolElements.ACCESS_IN_SERIAL_NUMBER_PARAM);
		String deviceMac = getStringParam(request, ProtocolElements.ACCESS_IN_MAC_PARAM);
		boolean forceLogin = getBooleanParam(request, ProtocolElements.ACCESS_IN_FORCE_LOGIN_PARAM);
		ErrorCodeEnum errCode = ErrorCodeEnum.SUCCESS;
        Device device = null;
		RpcConnection previousRpc = null;
		boolean reconnect = false;
		Long accessInUserId = null;
		do {
			// verify parameters
			if (StringUtils.isEmpty(uuid) || StringUtils.isEmpty(token) ||
					(StringUtils.isEmpty(deviceSerialNumber) && StringUtils.isEmpty(deviceMac))) {
				errCode = ErrorCodeEnum.REQUEST_PARAMS_ERROR;
				break;
			}

			// verify access token
			Map userInfo = cacheManage.getUserInfoByUUID(uuid);
			if (Objects.isNull(userInfo) || !Objects.equals(token, userInfo.get("token"))) {
				log.warn("local token:{} userInfo:{}", token, userInfo);
				errCode = ErrorCodeEnum.TOKEN_INVALID;
				break;
			}
			accessInUserId = Long.valueOf(String.valueOf(userInfo.get("userId")));
			rpcConnection.setMacAddr(deviceMac);
			rpcConnection.setUserId(accessInUserId);

			// verify device valid & TODO. check user org and dev org. the dev org must lower than user org. whether refuse and disconnect it.
			if (!StringUtils.isEmpty(deviceSerialNumber)) {
				DeviceSearch search = new DeviceSearch();
				search.setSerialNumber(deviceSerialNumber);
				if (Objects.isNull(device = deviceMapper.selectBySearchCondition(search))) {
					errCode = ErrorCodeEnum.DEVICE_NOT_FOUND;
					break;
				}
				rpcConnection.setDeviceSerailNumber(deviceSerialNumber);
			}

			previousRpc = notificationService.getRpcConnections().stream().filter(s -> {
				if (!Objects.equals(rpcConnection, s) && Objects.equals(s.getUserUuid(), uuid)) {
					log.info("find same login user:{}, previous connection id:{}, ", uuid, s.getParticipantPrivateId());
					log.info("previous connection userUuid:{}, macAddr:{}, userId:{}", s.getUserUuid(), s.getMacAddr(), s.getUserId());
					return true;
				} else {
					log.info("not found previous connection belong to the same user:{}, connection id:{}", uuid, s.getParticipantPrivateId());
					return false;
				}
			}).findFirst().orElse(null);

			// SINGLE LOGIN
			if (Objects.equals(userInfo.get("status"), UserOnlineStatusEnum.online.name())) {
				*//*previousRpc = notificationService.getRpcConnections().stream().filter(s -> !Objects.equals(rpcConnection, s)
						&& Objects.equals(s.getUserUuid(), uuid)).findFirst().orElse(null);*//*
				if (!Objects.isNull(previousRpc) && !Objects.equals(previousRpc.getParticipantPrivateId(),
						rpcConnection.getParticipantPrivateId()) && !Objects.equals(deviceMac, previousRpc.getMacAddr())) {
					log.warn("SINGLE LOGIN ==> User:{} already online.", userInfo.get("userUuid"));
					errCode = ErrorCodeEnum.USER_ALREADY_ONLINE;
                    rpcConnection.setUserUuid(uuid);
					break;
				}
			}

			// OFFLINE RECONNECT
			if (!Objects.isNull(previousRpc) && Objects.equals(userInfo.get("status"), UserOnlineStatusEnum.offline.name())
                    && Objects.equals(previousRpc.getMacAddr(), deviceMac)) {
				reconnect = true;
				rpcConnection.setReconnected(true);
				cacheManage.updateReconnectInfo(uuid, previousRpc.getParticipantPrivateId());
				rpcConnection.setUserUuid(uuid);
				previousRpc.setUserUuid(null);
				log.info("the account:{} now reconnect.", uuid);
				break;
			}

			if (!Objects.isNull(previousRpc)) {
				log.warn("NOT MATCH SINGLE LOGIN either RECONNECT and connection id:{}, userUuid:{}, macAddr:{}, userId:{}",
						rpcConnection.getParticipantPrivateId(), uuid, rpcConnection.getMacAddr(), rpcConnection.getUserId());
				// TODO. 原有断线重连服务端无法及时捕捉原有链接断开信息，此处根据mac及online状态直接判定为重连
				if (Objects.equals(previousRpc.getMacAddr(), deviceMac) &&
						Objects.equals(userInfo.get("status"), UserOnlineStatusEnum.online.name())) {
					log.info("the account:{} now reconnect.", uuid);
					rpcConnection.setReconnected(true);
					reconnect = true;
					cacheManage.updateReconnectInfo(uuid, previousRpc.getParticipantPrivateId());
					rpcConnection.setUserUuid(uuid);
					previousRpc.setUserUuid(null);
					break;
				}
				errCode = ErrorCodeEnum.USER_ALREADY_ONLINE;
                rpcConnection.setUserUuid(uuid);
				break;
			}

			rpcConnection.setUserUuid(uuid);
			log.info("NORMAL METHOD CALL ==> rpcConnection userUuid:{}, macAddr:{}, userId:{}",
					rpcConnection.getUserUuid(), rpcConnection.getMacAddr(), rpcConnection.getUserId());
		} while (false);

		if (!ErrorCodeEnum.SUCCESS.equals(errCode)) {
			log.warn("AccessIn Warning. privateId:{}, errCode:{}", rpcConnection.getParticipantPrivateId(), errCode.name());
			if (!Objects.equals(errCode, ErrorCodeEnum.USER_ALREADY_ONLINE)) {
				notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),null, errCode);
                sessionManager.accessOut(rpcConnection);

				return;
            } else {
				if (!forceLogin) {
					JsonObject result = new JsonObject();
					result.addProperty(ProtocolElements.ACCESS_IN_MAC_PARAM, !StringUtils.isEmpty(deviceMac) ? deviceMac : "");
					if (!Objects.isNull(device))
						result.addProperty(ProtocolElements.ACCESS_IN_DEVICE_NAME_PARAM, device.getDeviceName());
					notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(), result, errCode);
					//---------------------
					notificationService.closeRpcSession(rpcConnection.getParticipantPrivateId());
					return;
				} else {
					// send remote login notify to current terminal
					assert previousRpc != null;
					notificationService.sendNotification(previousRpc.getParticipantPrivateId(), ProtocolElements.REMOTE_LOGIN_NOTIFY_METHOD, new JsonObject());
					leaveRoomAfterConnClosed(previousRpc.getParticipantPrivateId(), EndReason.sessionClosedByServer);
					notificationService.closeRpcSession(previousRpc.getParticipantPrivateId());
				}
            }
		}
		// update user online status in cache
		cacheManage.updateDeviceName(uuid, Objects.isNull(device) ? "" : device.getDeviceName());
        cacheManage.updateUserOnlineStatus(uuid, reconnect ? UserOnlineStatusEnum.reconnect : UserOnlineStatusEnum.online);
		notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());

		if (reconnect) {
			String conferenceId = previousRpc.getSessionId();
			String previousRpcConnectId = previousRpc.getParticipantPrivateId();
			rpcConnection.setSessionId(conferenceId);
			// Send user break line notify
			JsonObject params = new JsonObject();
			params.addProperty(ProtocolElements.USER_BREAK_LINE_CONNECTION_ID_PARAM,
					this.sessionManager.getParticipant(previousRpcConnectId, StreamType.MAJOR).getParticipantPublicId());

			Participant preSharingPart = this.sessionManager.getParticipant(previousRpcConnectId, StreamType.SHARING);
			JsonObject notifyObj = new JsonObject();
			boolean shareNotify = !Objects.isNull(preSharingPart);
			if (shareNotify) {
				// Send reconnected participant stop publish previous sharing if exists
				notifyObj.addProperty(ProtocolElements.RECONNECTPART_STOP_PUBLISH_SHARING_CONNECTIONID_PARAM,
						preSharingPart.getParticipantPublicId());
			}

			this.sessionManager.getParticipants(conferenceId).forEach(participant -> {
				if (!Objects.equals(previousRpcConnectId, participant.getParticipantPrivateId())) {
					RpcConnection rpc = notificationService.getRpcConnection(participant.getParticipantPrivateId());
					if (!Objects.isNull(rpc)) {
						if (Objects.equals(cacheManage.getUserInfoByUUID(rpc.getUserUuid()).get("status"), UserOnlineStatusEnum.online.name())) {
							if (shareNotify) {
								notificationService.sendNotification(participant.getParticipantPrivateId(),
										ProtocolElements.RECONNECTPART_STOP_PUBLISH_SHARING_METHOD, notifyObj);
							}
							notificationService.sendNotification(participant.getParticipantPrivateId(), ProtocolElements.USER_BREAK_LINE_METHOD, params);
						}
					}
				}
			});
		}*/
    }

    /*private void confirmApplyForLogin(RpcConnection rpcConnection, Request<JsonObject> request) {
	    boolean accept = getBooleanParam(request, ProtocolElements.CONFIRM_APPLY_FOR_LOGIN_ACCEPT_PARAM);
	    String applicantSessionId = getStringOptionalParam(request, ProtocolElements.CONFIRM_APPLY_FOR_LOGIN_APPLICANT_SESSION_ID_PARAM);

	    notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());

        if (!Objects.isNull(notificationService.getRpcConnection(applicantSessionId))) {
            JsonObject param = new JsonObject();
            param.addProperty("loginAllowable", accept);
            notificationService.sendNotification(applicantSessionId, ProtocolElements.RESULT_OF_LOGIN_APPLY_NOTIFY, param);

            if (accept) {
                sessionManager.accessOut(rpcConnection);
            }
        }
    }*/

    /*private void accessOut(RpcConnection rpcConnection, Request<JsonObject> request) {
		if (request != null) {
			notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
		}

		sessionManager.accessOut(rpcConnection);
	}*/

	/*private void createRoom(RpcConnection rpcConnection, Request<JsonObject> request) {
		String sessionId = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_ID_PARAM);
		String password = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_PASSWORD_PARAM);
		if (StringUtils.isEmpty(sessionId)) {
			sessionId = generalRoomId();
        }

        if (isExistingRoom(sessionId, rpcConnection.getUserUuid())) {
			log.warn("conference:{} already exist.", sessionId);
			notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
					null, ErrorCodeEnum.CONFERENCE_ALREADY_EXIST);
        	return ;
		}

		if (sessionManager.isNewSessionIdValid(sessionId)) {
			JsonObject respJson = new JsonObject();
			respJson.addProperty(ProtocolElements.CREATE_ROOM_ID_PARAM, sessionId);
			// save conference info
            Conference conference = new Conference();
			conference.setRoomId(sessionId);
			conference.setPassword(StringUtils.isEmpty(password) ? null : password);
			conference.setStatus(1);
			conference.setStartTime(new Date());
			int insertResult = conferenceMapper.insert(conference);

            // setPresetInfo.
			String roomSubject = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_SUBJECT_PARAM);
			String micStatusInRoom = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_MIC_STATUS_PARAM);
			String videoStatusInRoom = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_VIDEO_STATUS_PARAM);
			String sharePowerInRoom = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_SHARE_POWER_PARAM);
			Integer roomCapacity = getIntOptionalParam(request, ProtocolElements.CREATE_ROOM_ROOM_CAPACITY_PARAM);
			Float roomDuration = getFloatOptionalParam(request, ProtocolElements.CREATE_ROOM_DURATION_PARAM);
			String useIdInRoom = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_USE_ID_PARAM);
			String allowPartOperMic = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_ALLOW_PART_OPER_MIC_PARAM);
			String allowPartOperShare = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_ALLOW_PART_OPER_SHARE_PARAM);

			SessionPreset preset = new SessionPreset(micStatusInRoom, videoStatusInRoom, sharePowerInRoom,
					roomSubject, roomCapacity, roomDuration, useIdInRoom, allowPartOperMic, allowPartOperShare);
			sessionManager.setPresetInfo(sessionId, preset);

            // store this inactive session
            sessionManager.storeSessionNotActiveWhileRoomCreated(sessionId);
			notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), respJson);
		} else {
			log.warn("conference:{} already exist.", sessionId);
			notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
					null, ErrorCodeEnum.CONFERENCE_ALREADY_EXIST);
		}
	}*/

	private boolean isExistingRoom(String sessionId, String userUuid) {
		// verify room id ever exists
		ConferenceSearch search = new ConferenceSearch();
		search.setRoomId(sessionId);
		// 会议状态：0 未开始(当前不存在该状态) 1 进行中 2 已结束
		search.setStatus(1);
		try {
			List<Conference> conferences = conferenceMapper.selectBySearchCondition(search);
			if (conferences != null && !conferences.isEmpty()) {
				if (sessionId.equals(userUuid)) {
					// force close previous room when sessionId is userUuid.
					log.warn("conference:{} will be force closed.", sessionId);
					// TODO
					conferences.forEach(conference -> sessionManager.endConferenceInfo(conference));
					cleanSession(sessionId, "", false, EndReason.forceCloseSessionByUser);
					return false;
				}

				log.warn("conference:{} already exist.", sessionId);
				return true;
			}
		} catch (Exception e) {
			log.info("conferenceMapper selectBySearchCondition(search) exception {}", e);
		}
		return false;
	}

	/*private String generalRoomId() {
		String sessionId = "";
		int tryCnt = 10;
		while (tryCnt-- > 0) {
			sessionId = StringUtil.createSessionId();
			if (isExistingRoom(sessionId, "")) {
				log.warn("conference:{} already exist.", sessionId);
				continue;
			}

			log.info("general sessionId:{}", sessionId);
			break;
		}

		return sessionId;
	}*/

	/*private void shareScreen(RpcConnection rpcConnection, Request<JsonObject> request) {
		String sessionId = getStringParam(request, ProtocolElements.SHARE_ROOM_ID_PARAM);
		String sourceId = getStringParam(request, ProtocolElements.SHARE_SOURCE_ID_PARAM);
		if (sessionManager.isSessionIdValid(sessionId)) {
			if (!sessionManager.isConflictSharing(sessionId, sourceId)) {
				notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
			} else {
				notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
						null, ErrorCodeEnum.SHARING_ALREADY_EXIST);
			}
		} else {
			notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
					null, ErrorCodeEnum.CONFERENCE_NOT_EXIST);
		}
	}*/

	/*private void stopShareScreen(RpcConnection rpcConnection, Request<JsonObject> request) {
		String sessionId = getStringParam(request, ProtocolElements.STOP_SHARE_ROOM_ID_PARAM);
		String sourceId = getStringParam(request, ProtocolElements.STOP_SHARE_SOURCE_ID_PARAM);
		if (sessionManager.isSessionIdValid(sessionId)) {
			ConcurrentHashMap<String, String> sessionInfo = sessionManager.getSessionInfo(sessionId);
			if (Objects.equals(sourceId, sessionInfo.get("sharingSourceId"))) {
				notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
			} else {
				notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
						null, ErrorCodeEnum.PERMISSION_LIMITED);
			}
		} else {
			notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
					null, ErrorCodeEnum.CONFERENCE_NOT_EXIST);
		}
	}*/

	/*private void getParticipants(RpcConnection rpcConnection, Request<JsonObject> request) {
		String sessionId = getStringParam(request, ProtocolElements.GET_PARTICIPANTS_ROOM_ID_PARAM);
		JsonObject respJson = new JsonObject();
		JsonArray jsonArray = new JsonArray();
		List<Long> userIds = new ArrayList<>();

		Map<Long, String> onlineUserList = new HashMap<>();
		for (RpcConnection c : notificationService.getRpcConnections()) {
			Map userInfo = cacheManage.getUserInfoByUUID(c.getUserUuid());
			if (Objects.isNull(userInfo)) continue;
			String status = String.valueOf(userInfo.get("status"));
			if (Objects.equals(UserOnlineStatusEnum.online.name(), status)) {
                onlineUserList.put(c.getUserId(), c.getSerialNumber());
                log.info("Status:{}, privateId:{}, userId:{}, serialNumber:{}", status, c.getParticipantPrivateId(), c.getUserId(), c.getSerialNumber());
            }
		}

		sessionManager.getParticipants(sessionId).forEach(s -> userIds.add(gson.fromJson(s.getClientMetadata(), JsonObject.class).get("clientData").getAsLong()));
		if (!CollectionUtils.isEmpty(userIds)) {
			List<User> userList = userMapper.selectByPrimaryKeys(userIds);
			userList.forEach(user -> {
				KurentoParticipant part = (KurentoParticipant) sessionManager.getParticipants(sessionId).stream().filter(s -> user.getId()
						.compareTo(gson.fromJson(s.getClientMetadata(), JsonObject.class).get("clientData").getAsLong()) == 0 &&
						Objects.equals(StreamType.MAJOR, s.getStreamType())).findFirst().get();

				// User and dept info.
				UserDeptSearch udSearch = new UserDeptSearch();
				udSearch.setUserId(user.getId());
				UserDept userDeptCom = userDeptMapper.selectBySearchCondition(udSearch);
				Department userDep = depMapper.selectByPrimaryKey(userDeptCom.getDeptId());

				String shareStatus;
				try{
					sessionManager.getParticipant(sessionId, part.getParticipantPrivateId(), StreamType.SHARING);
					shareStatus = ParticipantShareStatus.on.name();
				} catch (OpenViduException e) {
					shareStatus = ParticipantShareStatus.off.name();
				}

				JsonObject userObj = new JsonObject();
				userObj.addProperty("userId", user.getId());
				userObj.addProperty("account", user.getUsername());
				userObj.addProperty("userOrgName", userDep.getDeptName());
				userObj.addProperty("role", part.getRole().name());
				userObj.addProperty("shareStatus", shareStatus);
				userObj.addProperty("handStatus", part.getHandStatus().name());
				// 获取发布者时存在同步阻塞的状态
                userObj.addProperty("audioActive", part.isStreaming() && part.getPublisherMediaOptions().isAudioActive());
                userObj.addProperty("videoActive", part.isStreaming() && part.getPublisherMediaOptions().isVideoActive());

                // get device info if have device.
				String serialNumber = onlineUserList.get(user.getId());
				if (!StringUtils.isEmpty(serialNumber)) {
					log.info("select userId:{} online key(userId):{} serialNumber:{}", user.getId(),
							onlineUserList.get(user.getId()), serialNumber);

					// device and dept info.
					DeviceSearch deviceSearch = new DeviceSearch();
					deviceSearch.setSerialNumber(serialNumber);
					Device device = deviceMapper.selectBySearchCondition(deviceSearch);
					DeviceDeptSearch ddSearch = new DeviceDeptSearch();
					ddSearch.setSerialNumber(serialNumber);
					List<DeviceDept> devDeptCom = deviceDeptMapper.selectBySearchCondition(ddSearch);
					Department devDep = depMapper.selectByPrimaryKey(devDeptCom.get(0).getDeptId());

					userObj.addProperty("deviceName", device.getDeviceName());
					userObj.addProperty("deviceOrgName", devDep.getDeptName());
					userObj.addProperty("appShowName", device.getDeviceName());
					userObj.addProperty("appShowDesc", "(" + device.getDeviceModel() + ") " + devDep.getDeptName());
				} else {
					userObj.addProperty("appShowName", user.getUsername());
					userObj.addProperty("appShowDesc", "(" + user.getTitle() + ") " + userDep.getDeptName());
				}

				jsonArray.add(userObj);
			});
		}
		respJson.add("participantList", jsonArray);
		notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), respJson);
	}*/

	private void setAudioStatus(RpcConnection rpcConnection, Request<JsonObject> request) {
		String sessionId = getStringParam(request, ProtocolElements.SET_AUDIO_ROOM_ID_PARAM);
//		String targetId = getStringParam(request, ProtocolElements.SET_AUDIO_TARGET_ID_PARAM);
//		String targetId = getStringOptionalParam(request, ProtocolElements.SET_AUDIO_TARGET_ID_PARAM);
		String sourceId = getStringParam(request, ProtocolElements.SET_AUDIO_SOURCE_ID_PARAM);
		String status = getStringParam(request, ProtocolElements.SET_AUDIO_STATUS_PARAM);
		List<String> targetIds = getStringListParam(request, ProtocolElements.SET_AUDIO_TARGET_IDS_PARAM);

		if ((Objects.isNull(targetIds) || targetIds.isEmpty() || !Objects.equals(sourceId, targetIds.get(0))) &&
				sessionManager.getParticipant(sessionId, rpcConnection.getParticipantPrivateId()).getRole() != OpenViduRole.MODERATOR) {
			this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
					null, ErrorCodeEnum.PERMISSION_LIMITED);
			return;
		}

		JsonArray tsArray = new JsonArray();
		if (!Objects.isNull(targetIds) && !targetIds.isEmpty()) {
			targetIds.forEach(t -> {
				KurentoParticipant part = (KurentoParticipant) sessionManager.getParticipants(sessionId).stream().filter(s -> Long.valueOf(t)
						.compareTo(gson.fromJson(s.getClientMetadata(), JsonObject.class).get("clientData")
								.getAsLong()) == 0).findFirst().get();
				if (part.isStreaming())
					part.getPublisherMediaOptions().setAudioActive(!status.equals(ParticipantMicStatus.off.name()));

				tsArray.add(t);
			});
		}

		JsonObject params = new JsonObject();
		params.addProperty(ProtocolElements.SET_AUDIO_ROOM_ID_PARAM, sessionId);
		params.addProperty(ProtocolElements.SET_AUDIO_SOURCE_ID_PARAM, sourceId);
		params.add(ProtocolElements.SET_AUDIO_TARGET_IDS_PARAM, tsArray);
		params.addProperty(ProtocolElements.SET_AUDIO_STATUS_PARAM, getStringParam(request, ProtocolElements.SET_AUDIO_STATUS_PARAM));
		Set<Participant> participants = sessionManager.getParticipants(sessionId);
		if (!CollectionUtils.isEmpty(participants)) {
			for (Participant p: participants) {
				this.notificationService.sendNotification(p.getParticipantPrivateId(), ProtocolElements.SET_AUDIO_STATUS_METHOD, params);

				if ((Objects.isNull(targetIds) || targetIds.isEmpty()) &&
						!sourceId.equals(gson.fromJson(p.getClientMetadata(), JsonObject.class).get("clientData").getAsString())) {
					KurentoParticipant part = (KurentoParticipant) p;
					if (part.isStreaming()) part.getPublisherMediaOptions().setAudioActive(!status.equals(ParticipantMicStatus.off.name()));
				}
			}
		}
		this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
	}

	private void setVideoStatus(RpcConnection rpcConnection, Request<JsonObject> request) {
		String sessionId = getStringParam(request, ProtocolElements.SET_VIDEO_ROOM_ID_PARAM);
		String sourceId = getStringParam(request, ProtocolElements.SET_AUDIO_SOURCE_ID_PARAM);
		List<String> targetIds = getStringListParam(request, ProtocolElements.SET_AUDIO_TARGET_IDS_PARAM);
		String status = getStringParam(request, ProtocolElements.SET_AUDIO_STATUS_PARAM);
		if ((Objects.isNull(targetIds) || targetIds.isEmpty() || !Objects.equals(sourceId, targetIds.get(0)))
				&& sessionManager.getParticipant(sessionId, rpcConnection.getParticipantPrivateId()).getRole() != OpenViduRole.MODERATOR) {
			this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
					null, ErrorCodeEnum.PERMISSION_LIMITED);
			return;
		}

		JsonArray tsArray = new JsonArray();
		if (!Objects.isNull(targetIds) && !targetIds.isEmpty()) {
			targetIds.forEach(t -> {
				KurentoParticipant part = (KurentoParticipant) sessionManager.getParticipants(sessionId).stream().filter(s -> Long.valueOf(t)
						.compareTo(gson.fromJson(s.getClientMetadata(), JsonObject.class).get("clientData")
								.getAsLong()) == 0).findFirst().get();
				if (part.isStreaming())
					part.getPublisherMediaOptions().setVideoActive(!status.equals(ParticipantMicStatus.off.name()));
				tsArray.add(t);
			});
		}

		JsonObject params = new JsonObject();
		params.addProperty(ProtocolElements.SET_VIDEO_ROOM_ID_PARAM, sessionId);
		params.addProperty(ProtocolElements.SET_VIDEO_SOURCE_ID_PARAM, getStringParam(request, ProtocolElements.SET_VIDEO_SOURCE_ID_PARAM));
		params.add(ProtocolElements.SET_VIDEO_TARGET_IDS_PARAM, tsArray);
		params.addProperty(ProtocolElements.SET_VIDEO_STATUS_PARAM, getStringParam(request, ProtocolElements.SET_VIDEO_STATUS_PARAM));

		sessionManager.getParticipants(sessionId).forEach(participant -> {
			this.notificationService.sendNotification(participant.getParticipantPrivateId(),
					ProtocolElements.SET_VIDEO_STATUS_METHOD, params);
			if ((Objects.isNull(targetIds) || targetIds.isEmpty()) && !sourceId.equals(gson.fromJson(participant.getClientMetadata(),
					JsonObject.class).get("clientData").getAsString())) {
				KurentoParticipant part = (KurentoParticipant) participant;
				if (part.isStreaming()) part.getPublisherMediaOptions().setVideoActive(!status.equals(ParticipantMicStatus.off.name()));
			}
		});
		this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
	}

    private void raiseHand(RpcConnection rpcConnection, Request<JsonObject> request) {
		String sessionId = getStringParam(request, ProtocolElements.RAISE_HAND_ROOM_ID_PARAM);
		String sourceId = getStringParam(request, ProtocolElements.RAISE_HAND_SOURCE_ID_PARAM);
		sessionManager.getParticipant(sessionId, rpcConnection.getParticipantPrivateId()).setHandStatus(ParticipantHandStatus.up);

		List<String> notifyClientPrivateIds = sessionManager.getParticipants(sessionId)
				.stream().map(Participant::getParticipantPrivateId).collect(Collectors.toList());
		if (!CollectionUtils.isEmpty(notifyClientPrivateIds)) {
			int raiseHandNum = 0;
			for (Participant p : sessionManager.getParticipants(sessionId)) {
				if (Objects.equals(StreamType.MAJOR, p.getStreamType()) &&
						p.getHandStatus() == ParticipantHandStatus.up) {
					++raiseHandNum;
				}
			}

			JsonObject params = new JsonObject();
			params.addProperty(ProtocolElements.RAISE_HAND_ROOM_ID_PARAM, sessionId);
			params.addProperty(ProtocolElements.RAISE_HAND_SOURCE_ID_PARAM, sourceId);
			params.addProperty(ProtocolElements.RAISE_HAND_NUMBER_PARAM, String.valueOf(raiseHandNum));
			notifyClientPrivateIds.forEach(client -> this.notificationService.sendNotification(client, ProtocolElements.RAISE_HAND_METHOD, params));
		}
		notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }

	private void putDownHand(RpcConnection rpcConnection, Request<JsonObject> request) {
		String sessionId = getStringParam(request, ProtocolElements.PUT_DOWN_HAND_ROOM_ID_PARAM);
		String sourceId = getStringParam(request, ProtocolElements.PUT_DOWN_HAND_SOURCE_ID_PARAM);
		String targetId = request.getParams().has(ProtocolElements.PUT_DOWN_HAND_TARGET_ID_PARAM) ?
				request.getParams().get(ProtocolElements.PUT_DOWN_HAND_TARGET_ID_PARAM).getAsString() : null;
		Set<Participant> participants = sessionManager.getParticipants(sessionId);
		if (!StringUtils.isEmpty(targetId)) {
			sessionManager.getParticipant(sessionId, rpcConnection.getParticipantPrivateId(), StreamType.MAJOR)
					.setHandStatus(ParticipantHandStatus.down);
		} else {
			participants.forEach(part -> part.setHandStatus(ParticipantHandStatus.down));
		}

		JsonObject params = new JsonObject();
		params.addProperty(ProtocolElements.PUT_DOWN_HAND_ROOM_ID_PARAM, sessionId);
		params.addProperty(ProtocolElements.PUT_DOWN_HAND_SOURCE_ID_PARAM, sourceId);
		if (!StringUtils.isEmpty(targetId)) {
			params.addProperty(ProtocolElements.PUT_DOWN_HAND_TARGET_ID_PARAM, targetId);
			int raiseHandNum = 0;
			for (Participant p : sessionManager.getParticipants(sessionId)) {
				if (Objects.equals(ParticipantHandStatus.up, p.getHandStatus()) &&
						Objects.equals(StreamType.MAJOR, p.getStreamType())) {
					++raiseHandNum;
				}
			}
			params.addProperty(ProtocolElements.PUT_DOWN_HAND_RAISEHAND_NUMBER_PARAM, raiseHandNum);
		}
		participants.forEach(participant ->
				this.notificationService.sendNotification(participant.getParticipantPrivateId(), ProtocolElements.PUT_DOWN_HAND_METHOD, params));

		this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
	}

	private void setRollCall(RpcConnection rpcConnection, Request<JsonObject> request) {
		String sessionId = getStringParam(request, ProtocolElements.SET_ROLL_CALL_ROOM_ID_PARAM);
		String sourceId = getStringParam(request, ProtocolElements.SET_ROLL_CALL_SOURCE_ID_PARAM);
		String targetId = getStringParam(request, ProtocolElements.SET_ROLL_CALL_TARGET_ID_PARAM);

		Set<Participant> participants = sessionManager.getParticipants(sessionId);
		participants.stream().filter(part ->
				targetId.equals(gson.fromJson(part.getClientMetadata(), JsonObject.class).get("clientData").getAsString()))
				.findFirst().get().setHandStatus(ParticipantHandStatus.speaker);

		int raiseHandNum = 0;
		for (Participant p : sessionManager.getParticipants(sessionId)) {
			if (p.getHandStatus() == ParticipantHandStatus.up) {
				++raiseHandNum;
			}
		}

		JsonObject params = new JsonObject();
		params.addProperty(ProtocolElements.SET_ROLL_CALL_ROOM_ID_PARAM, sessionId);
		params.addProperty(ProtocolElements.SET_ROLL_CALL_SOURCE_ID_PARAM, sourceId);
		params.addProperty(ProtocolElements.SET_ROLL_CALL_TARGET_ID_PARAM, targetId);
		params.addProperty(ProtocolElements.SET_ROLL_CALL_RAISEHAND_NUMBER_PARAM, raiseHandNum);
		participants.forEach(participant ->
				this.notificationService.sendNotification(participant.getParticipantPrivateId(), ProtocolElements.SET_ROLL_CALL_METHOD, params));

		this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
	}

	private void endRollCall(RpcConnection rpcConnection, Request<JsonObject> request) {
		// TODO, Fixme. Maybe have good way to do it. for example: add a roll call type.
		String sessionId = getStringParam(request, ProtocolElements.END_ROLL_CALL_ROOM_ID_PARAM);
		String sourceId = getStringParam(request, ProtocolElements.END_ROLL_CALL_SOURCE_ID_PARAM);
		String targetId = getStringParam(request, ProtocolElements.END_ROLL_CALL_TARGET_ID_PARAM);

		Set<Participant> participants = sessionManager.getParticipants(sessionId);
		participants.stream().filter(part ->
				targetId.equals(gson.fromJson(part.getClientMetadata(), JsonObject.class).get("clientData").getAsString()))
				.findFirst().get().setHandStatus(ParticipantHandStatus.endSpeaker);
		int raiseHandNum = 0;
		for (Participant p : sessionManager.getParticipants(sessionId)) {
			if (p.getHandStatus() == ParticipantHandStatus.up) {
				++raiseHandNum;
			}
		}

		JsonObject params = new JsonObject();
		params.addProperty(ProtocolElements.END_ROLL_CALL_ROOM_ID_PARAM, sessionId);
		params.addProperty(ProtocolElements.END_ROLL_CALL_SOURCE_ID_PARAM, sourceId);
		params.addProperty(ProtocolElements.END_ROLL_CALL_TARGET_ID_PARAM, targetId);
		params.addProperty(ProtocolElements.END_ROLL_CALL_RAISEHAND_NUMBER_PARAM, raiseHandNum);
		participants.forEach(participant ->
				this.notificationService.sendNotification(participant.getParticipantPrivateId(), ProtocolElements.END_ROLL_CALL_METHOD, params));

		this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
	}

	private void replaceRollCall(RpcConnection rpcConnection, Request<JsonObject> request) {
		// TODO, Fixme. Maybe have good way to do it. for example: add a roll call type.
		String sessionId = getStringParam(request, ProtocolElements.REPLACE_ROLL_CALL_ROOM_ID_PARAM);
		String sourceId = getStringParam(request, ProtocolElements.REPLACE_ROLL_CALL_SOURCE_ID_PARAM);
		String endTargetId = getStringParam(request, ProtocolElements.REPLACE_ROLL_CALL_END_TARGET_ID_PARAM);
		String startTargetId = getStringParam(request, ProtocolElements.REPLACE_ROLL_CALL_START_TARGET_ID_PARAM);

		Set<Participant> participants = sessionManager.getParticipants(sessionId);
		participants.stream().filter(part ->
				endTargetId.equals(gson.fromJson(part.getClientMetadata(), JsonObject.class).get("clientData").getAsString()))
				.findFirst().get().setHandStatus(ParticipantHandStatus.down);

		participants.stream().filter(part ->
				startTargetId.equals(gson.fromJson(part.getClientMetadata(), JsonObject.class).get("clientData").getAsString()))
				.findFirst().get().setHandStatus(ParticipantHandStatus.speaker);

		int raiseHandNum = 0;
		for (Participant p : sessionManager.getParticipants(sessionId)) {
			if (p.getHandStatus() == ParticipantHandStatus.up) {
				++raiseHandNum;
			}
		}

		JsonObject params = new JsonObject();
		params.addProperty(ProtocolElements.REPLACE_ROLL_CALL_ROOM_ID_PARAM, sessionId);
		params.addProperty(ProtocolElements.REPLACE_ROLL_CALL_SOURCE_ID_PARAM, sourceId);
		params.addProperty(ProtocolElements.REPLACE_ROLL_CALL_END_TARGET_ID_PARAM, endTargetId);
		params.addProperty(ProtocolElements.REPLACE_ROLL_CALL_START_TARGET_ID_PARAM, startTargetId);
		params.addProperty(ProtocolElements.REPLACE_ROLL_CALL_RAISEHAND_NUMBER_PARAM, raiseHandNum);
		participants.forEach(participant ->
				this.notificationService.sendNotification(participant.getParticipantPrivateId(), ProtocolElements.REPLACE_ROLL_CALL_METHOD, params));

		this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
	}

	private void lockSession(RpcConnection rpcConnection, Request<JsonObject> request) {
		String sessionId = getStringParam(request, ProtocolElements.LOCK_SESSION_ROOM_ID_PARAM);
		if (sessionManager.getSession(sessionId).isClosed()) {
			this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(), null, ErrorCodeEnum.CONFERENCE_IS_LOCKED);
			return;
		}
		if (sessionManager.getParticipant(sessionId, rpcConnection.getParticipantPrivateId()).getRole() != OpenViduRole.MODERATOR) {
			this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(), null, ErrorCodeEnum.PERMISSION_LIMITED);
			return;
		}

		if (!sessionManager.getSession(sessionId).isLocking() &&
				sessionManager.getSession(sessionId).setLocking(true)) {
			JsonObject params = new JsonObject();
			params.addProperty(ProtocolElements.LOCK_SESSION_ROOM_ID_PARAM, sessionId);

			sessionManager.getParticipants(sessionId).forEach(participant ->
					this.notificationService.sendNotification(participant.getParticipantPrivateId(), ProtocolElements.LOCK_SESSION_METHOD, params));
		}
		this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());

	}

	private void unlockSession(RpcConnection rpcConnection, Request<JsonObject> request) {
		String sessionId = getStringParam(request, ProtocolElements.UNLOCK_SESSION_ROOM_ID_PARAM);
		if (sessionManager.getParticipant(sessionId, rpcConnection.getParticipantPrivateId()).getRole() != OpenViduRole.MODERATOR) {
			this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(), null, ErrorCodeEnum.PERMISSION_LIMITED);
			return;
		}

		if (sessionManager.getSession(sessionId).isLocking() &&
				!sessionManager.getSession(sessionId).setLocking(false)) {
			JsonObject params = new JsonObject();
			params.addProperty(ProtocolElements.UNLOCK_SESSION_ROOM_ID_PARAM, sessionId);

			sessionManager.getParticipants(sessionId).forEach(participant ->
					this.notificationService.sendNotification(participant.getParticipantPrivateId(), ProtocolElements.UNLOCK_SESSION_METHOD, params));
		}
		this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());

	}

    public void joinRoom(RpcConnection rpcConnection, Request<JsonObject> request) {
		String sessionId = getStringParam(request, ProtocolElements.JOINROOM_ROOM_PARAM);
        String clientMetadata = getStringParam(request, ProtocolElements.JOINROOM_METADATA_PARAM);
        String role = getStringParam(request, ProtocolElements.JOINROOM_ROLE_PARAM);
		String secret = getStringParam(request, ProtocolElements.JOINROOM_SECRET_PARAM);
		String platform = getStringParam(request, ProtocolElements.JOINROOM_PLATFORM_PARAM);
		String streamType = getStringParam(request, ProtocolElements.JOINROOM_STREAM_TYPE_PARAM);
		String password = (request.getParams() != null && request.getParams().has(ProtocolElements.JOINROOM_PASSWORD_PARAM)) ?
				request.getParams().get(ProtocolElements.JOINROOM_PASSWORD_PARAM).getAsString() : null;
		String participantPrivatetId = rpcConnection.getParticipantPrivateId();
		SessionPreset preset = sessionManager.getPresetInfo(sessionId);
		ErrorCodeEnum errCode = ErrorCodeEnum.SUCCESS;

		try {
			do {
				// verify room join type
				String joinType = getStringParam(request, ProtocolElements.JOINROOM_TYPE_PARAM);
				if (!rpcConnection.isReconnected() && StreamType.MAJOR.equals(StreamType.valueOf(streamType)) &&
						SessionPresetUseIDEnum.ONLY_MODERATOR.equals(preset.getUseIdTypeInRoom())) {
					if (!isModerator(role) && ParticipantJoinType.active.equals(ParticipantJoinType.valueOf(joinType))) {
						log.error("disable participant active join room:{}", sessionId);
						errCode = ErrorCodeEnum.PERMISSION_LIMITED;
						break;
					}
				}

				ConferenceSearch search = new ConferenceSearch();
				search.setRoomId(sessionId);
				search.setStatus(1);
				List<Conference> conference = conferenceMapper.selectBySearchCondition(search);
				if (conference == null || conference.isEmpty()) {
					log.error("can not find roomId:{} in data layer", sessionId);
					errCode = ErrorCodeEnum.CONFERENCE_NOT_EXIST;
					break;
				}

				// verify conference password
				if (streamType.equals(StreamType.MAJOR.name()) && !Objects.equals(joinType, ParticipantJoinType.invited.name())
                        && !StringUtils.isEmpty(conference.get(0).getPassword()) && !Objects.equals(conference.get(0).getPassword(), password)) {
					log.error("invalid room password:{}", password);
					errCode = ErrorCodeEnum.CONFERENCE_PASSWORD_ERROR;
					break;
				}

				// verify conference ever locked
				if (!rpcConnection.isReconnected() && !Objects.isNull(sessionManager.getSession(sessionId)) &&
						sessionManager.getSession(sessionId).isLocking()) {
					log.error("room:{} is locked.", sessionId);
					errCode = ErrorCodeEnum.CONFERENCE_IS_LOCKED;
					break;
				}

				// check preset and set share power in room info. for example: sharePower.
//		if (Objects.equals(streamType, StreamType.SHARING.name())) {
//			Participant p = sessionManager.getParticipant(rpcConnection.getParticipantPrivateId());
//			if (Objects.isNull(p) || ParticipantSharePowerStatus.off.equals(p.getSharePowerStatus())) {
//				this.notificationService.sendErrorResponseWithDesc(participantPrivatetId, request.getId(),
//						null, ErrorCodeEnum.PERMISSION_LIMITED);
//				return ;
//			}
//		}
				// remove previous participant if reconnect
				updateReconnectInfo(rpcConnection);
				GeoLocation location = null;
				boolean recorder = false;

				// verify room capacity limit.
				if (!Objects.isNull(sessionManager.getSession(sessionId))) {
					Set<Participant> majorParts = sessionManager.getSession(sessionId).getMajorPartEachConnect();
					if (Objects.equals(StreamType.MAJOR.name(), streamType) && majorParts.size() >= preset.getRoomCapacity()) {
						log.error("verify room:{} capacity:{} cur capacity:{}", sessionId, preset.getRoomCapacity(), majorParts.size());
						errCode = ErrorCodeEnum.ROOM_CAPACITY_LIMITED;
						break;
					}
				}

				try {
					recorder = getBooleanParam(request, ProtocolElements.JOINROOM_RECORDER_PARAM);
				} catch (RuntimeException e) {
					// Nothing happens. 'recorder' param to false
				}

				boolean generateRecorderParticipant = false;
				if (openviduConfig.isOpenViduSecret(secret)) {
					sessionManager.newInsecureParticipant(participantPrivatetId);
					if (recorder) {
						generateRecorderParticipant = true;
					}
				}

				if (!sessionManager.formatChecker.isServerMetadataFormatCorrect(clientMetadata)) {
					log.error("Metadata format set in client-side is incorrect");
					errCode = ErrorCodeEnum.SERVER_UNKNOWN_ERROR;
					break;
				}

				Participant participant;
				if (generateRecorderParticipant) {
					participant = sessionManager.newRecorderParticipant(sessionId, participantPrivatetId, clientMetadata, role, streamType);
				} else {
					participant = sessionManager.newParticipant(sessionId, participantPrivatetId, clientMetadata,
							role, streamType, location, platform,
							participantPrivatetId.substring(0, Math.min(16, participantPrivatetId.length())));
				}

				String serialNumber = rpcConnection.getSerialNumber();
				Long userId = rpcConnection.getUserId();
				participant.setPreset(preset);
				participant.setJoinType(ParticipantJoinType.valueOf(joinType));
				if (StringUtils.isEmpty(serialNumber)) {
					User user = userMapper.selectByPrimaryKey(userId);

					// User and dept info.
					UserDeptSearch udSearch = new UserDeptSearch();
					udSearch.setUserId(userId);
					UserDept userDeptCom = userDeptMapper.selectBySearchCondition(udSearch);
					Department userDep = depMapper.selectByPrimaryKey(userDeptCom.getDeptId());

					participant.setAppShowInfo(user.getUsername(), "(" + user.getTitle() + ") " + userDep.getDeptName());
				} else {
					DeviceSearch devSearch = new DeviceSearch();
					devSearch.setSerialNumber(serialNumber);
					Device device = deviceMapper.selectBySearchCondition(devSearch);
					DeviceDeptSearch ddSearch = new DeviceDeptSearch();
					ddSearch.setSerialNumber(serialNumber);
					List<DeviceDept> devDeptCom = deviceDeptMapper.selectBySearchCondition(ddSearch);
					if (Objects.isNull(devDeptCom) || devDeptCom.isEmpty()) {
						log.warn("devDep cant select serialNumber:{}", serialNumber);
						participant.setAppShowInfo(device.getDeviceName(), "(" + device.getDeviceModel() + ") ");
					} else {
						Department devDep = depMapper.selectByPrimaryKey(devDeptCom.get(0).getDeptId());
						participant.setAppShowInfo(device.getDeviceName(), "(" + device.getDeviceModel() + ") " + devDep.getDeptName());
					}
				}

				rpcConnection.setSessionId(sessionId);
				sessionManager.joinRoom(participant, sessionId, conference.get(0), request.getId());
			} while (false);

			if (!ErrorCodeEnum.SUCCESS.equals(errCode)) {
				this.notificationService.sendErrorResponseWithDesc(participantPrivatetId, request.getId(),
						null, errCode);

				log.error("join room:{} failed. errCode:{} message:{}", sessionId, errCode.getCode(), errCode.getMessage());
				if (isModerator(role)) {
					sessionManager.cleanCacheCollections(sessionId);
					cleanSession(sessionId, rpcConnection.getParticipantPrivateId(), false, EndReason.forceCloseSessionByUser);
				}
			}
		} catch (Exception e) {
			log.error("Unknown error e:{}", e);
			if (isModerator(role)) {
				sessionManager.cleanCacheCollections(sessionId);
			}
		}
	}

	private void leaveRoom(RpcConnection rpcConnection, Request<JsonObject> request) {
        String sessionId = getStringParam(request, ProtocolElements.LEAVEROOM_ROOM_ID_PARAM);
        String sourceId = getStringParam(request, ProtocolElements.LEAVEROOM_SOURCE_ID_PARAM);
        String streamType = getStringParam(request, ProtocolElements.LEAVEROOM_STREAM_TYPE_PARAM);
        if (StringUtils.isEmpty(sessionId) || StringUtils.isEmpty(sourceId) || StringUtils.isEmpty(streamType)) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.REQUEST_PARAMS_ERROR);
            return;
        }
		Participant participant;
		try {
			participant = sanityCheckOfSession(rpcConnection, StreamType.valueOf(streamType));
		} catch (OpenViduException e) {
			if (updateReconnectInfo(rpcConnection)) {
				log.info("close previous participant info.");
				notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
			}
			return;
		}

		sessionManager.leaveRoom(participant, request.getId(), EndReason.disconnect, false);
		log.info("Participant {} has left session {}", participant.getParticipantPublicId(),
				rpcConnection.getSessionId());
	}

	private void publishVideo(RpcConnection rpcConnection, Request<JsonObject> request) {
		String streamType = getStringParam(request, ProtocolElements.PUBLISHVIDEO_STREAM_TYPE_PARAM);
		Participant participant;
		try {
			participant = sanityCheckOfSession(rpcConnection, StreamType.valueOf(streamType));
		} catch (OpenViduException e) {
			return;
		}

		if (sessionManager.isPublisherInSession(rpcConnection.getSessionId(), participant)) {
			MediaOptions options = sessionManager.generateMediaOptions(request);
			sessionManager.publishVideo(participant, options, request.getId());
		} else {
			log.error("Error: participant {} is not a publisher", participant.getParticipantPublicId());
			throw new OpenViduException(Code.USER_UNAUTHORIZED_ERROR_CODE,
					"Unable to publish video. The user does not have a valid token");
		}
	}

	private void receiveVideoFrom(RpcConnection rpcConnection, Request<JsonObject> request) {
		Participant participant;
		try {
			participant = sanityCheckOfSession(rpcConnection, "subscribe");
		} catch (OpenViduException e) {
			return;
		}

		String senderName = getStringParam(request, ProtocolElements.RECEIVEVIDEO_SENDER_PARAM);
		senderName = senderName.substring(0, senderName.indexOf("_"));
		String sdpOffer = getStringParam(request, ProtocolElements.RECEIVEVIDEO_SDPOFFER_PARAM);

		sessionManager.subscribe(participant, senderName, sdpOffer, request.getId());
	}

	private void unsubscribeFromVideo(RpcConnection rpcConnection, Request<JsonObject> request) {
		Participant participant;
		try {
			participant = sanityCheckOfSession(rpcConnection, "unsubscribe");
		} catch (OpenViduException e) {
			return;
		}

		String senderName = getStringParam(request, ProtocolElements.UNSUBSCRIBEFROMVIDEO_SENDER_PARAM);
		sessionManager.unsubscribe(participant, senderName, request.getId());
	}

	private void onIceCandidate(RpcConnection rpcConnection, Request<JsonObject> request) {
		Participant participant;

		String endpointName = getStringParam(request, ProtocolElements.ONICECANDIDATE_EPNAME_PARAM);
		String candidate = getStringParam(request, ProtocolElements.ONICECANDIDATE_CANDIDATE_PARAM);
		String sdpMid = getStringParam(request, ProtocolElements.ONICECANDIDATE_SDPMIDPARAM);
		int sdpMLineIndex = getIntParam(request, ProtocolElements.ONICECANDIDATE_SDPMLINEINDEX_PARAM);
		try {
			// TODO. Maybe should add streamType in protocol when invoke OnIceCandidate.
//			participant = sanityCheckOfSession(rpcConnection, "onIceCandidate");
			participant = sanityCheckOfSession(rpcConnection, endpointName, "onIceCandidate");
		} catch (OpenViduException e) {
			return;
		}

		sessionManager.onIceCandidate(participant, endpointName, candidate, sdpMLineIndex, sdpMid, request.getId());
	}

	private void sendMessage(RpcConnection rpcConnection, Request<JsonObject> request) {
		Participant participant;
		try {
			participant = sanityCheckOfSession(rpcConnection, "signal");
		} catch (OpenViduException e) {
			return;
		}

		String message = getStringParam(request, ProtocolElements.SENDMESSAGE_MESSAGE_PARAM);
		sessionManager.sendMessage(participant, message, request.getId());
	}

	private void unpublishVideo(RpcConnection rpcConnection, Request<JsonObject> request) {
		Participant participant;
		try {
			participant = sanityCheckOfSession(rpcConnection, "unpublish");
		} catch (OpenViduException e) {
			return;
		}

		sessionManager.unpublishVideo(participant, null, request.getId(), EndReason.unpublish);
	}

	private void streamPropertyChanged(RpcConnection rpcConnection, Request<JsonObject> request) {
		Participant participant;
		try {
			participant = sanityCheckOfSession(rpcConnection, "onStreamPropertyChanged");
		} catch (OpenViduException e) {
			return;
		}

		String streamId = getStringParam(request, ProtocolElements.STREAMPROPERTYCHANGED_STREAMID_PARAM);
		String property = getStringParam(request, ProtocolElements.STREAMPROPERTYCHANGED_PROPERTY_PARAM);
		JsonElement newValue = getParam(request, ProtocolElements.STREAMPROPERTYCHANGED_NEWVALUE_PARAM);
		String reason = getStringParam(request, ProtocolElements.STREAMPROPERTYCHANGED_REASON_PARAM);

		sessionManager.streamPropertyChanged(participant, request.getId(), streamId, property, newValue, reason);
	}

	private void forceDisconnect(RpcConnection rpcConnection, Request<JsonObject> request) {
		Participant participant;
		try {
			participant = sanityCheckOfSession(rpcConnection, "forceDisconnect");
		} catch (OpenViduException e) {
			return;
		}

		if (sessionManager.isModeratorInSession(rpcConnection.getSessionId(), participant)) {
			String connectionId = getStringParam(request, ProtocolElements.FORCEDISCONNECT_CONNECTIONID_PARAM);
			sessionManager.evictParticipant(
					sessionManager.getSession(rpcConnection.getSessionId()).getParticipantByPublicId(connectionId),
					participant, request.getId(), EndReason.forceDisconnectByUser);
		} else {
			log.error("Error: participant {} is not a moderator", participant.getParticipantPublicId());
			throw new OpenViduException(Code.USER_UNAUTHORIZED_ERROR_CODE,
					"Unable to force disconnect. The user does not have a valid token");
		}
	}

	private void forceUnpublish(RpcConnection rpcConnection, Request<JsonObject> request) {
		Participant participant;
		try {
			participant = sanityCheckOfSession(rpcConnection, "forceUnpublish");
		} catch (OpenViduException e) {
			return;
		}

		if (sessionManager.isModeratorInSession(rpcConnection.getSessionId(), participant)) {
			String streamId = getStringParam(request, ProtocolElements.FORCEUNPUBLISH_STREAMID_PARAM);
			if (sessionManager.unpublishStream(sessionManager.getSession(rpcConnection.getSessionId()), streamId,
					participant, request.getId(), EndReason.forceUnpublishByUser)) {
				notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
						null, ErrorCodeEnum.USER_NOT_STREAMING_ERROR_CODE);
			}
		} else {
			log.error("Error: participant {} is not a moderator", participant.getParticipantPublicId());
			throw new OpenViduException(Code.USER_UNAUTHORIZED_ERROR_CODE,
					"Unable to force unpublish. The user does not have a valid token");
		}
	}

	private void applyFilter(RpcConnection rpcConnection, Request<JsonObject> request) {
		Participant participant;
		try {
			participant = sanityCheckOfSession(rpcConnection, "applyFilter");
		} catch (OpenViduException e) {
			return;
		}

		String filterType = getStringParam(request, ProtocolElements.FILTER_TYPE_PARAM);
		String streamId = getStringParam(request, ProtocolElements.FILTER_STREAMID_PARAM);
		boolean isModerator = this.sessionManager.isModeratorInSession(rpcConnection.getSessionId(), participant);

		// Allow applying filter if the user is a MODERATOR (owning the stream or other
		// user's stream) or if the user is the owner of the stream and has a token
		// configured with this specific filter
		if (isModerator || (this.userIsStreamOwner(rpcConnection.getSessionId(), participant, streamId))) {
//				&& participant.getToken().getKurentoTokenOptions().isFilterAllowed(filterType))) {
			JsonObject filterOptions;
			try {
				filterOptions = new JsonParser().parse(getStringParam(request, ProtocolElements.FILTER_OPTIONS_PARAM))
						.getAsJsonObject();
			} catch (JsonSyntaxException e) {
				throw new OpenViduException(Code.FILTER_NOT_APPLIED_ERROR_CODE,
						"'options' parameter is not a JSON object: " + e.getMessage());
			}
			Participant moderator = isModerator ? participant : null;
			sessionManager.applyFilter(sessionManager.getSession(rpcConnection.getSessionId()), streamId, filterType,
					filterOptions, moderator, request.getId(), "applyFilter");
		} else {
			log.error("Error: participant {} is not a moderator", participant.getParticipantPublicId());
			throw new OpenViduException(Code.USER_UNAUTHORIZED_ERROR_CODE,
					"Unable to apply filter. The user does not have a valid token");
		}
	}

	private void removeFilter(RpcConnection rpcConnection, Request<JsonObject> request) {
		Participant participant;
		try {
			participant = sanityCheckOfSession(rpcConnection, "removeFilter");
		} catch (OpenViduException e) {
			return;
		}
		String streamId = getStringParam(request, ProtocolElements.FILTER_STREAMID_PARAM);
		boolean isModerator = this.sessionManager.isModeratorInSession(rpcConnection.getSessionId(), participant);

		// Allow removing filter if the user is a MODERATOR (owning the stream or other
		// user's stream) or if the user is the owner of the stream
		if (isModerator || this.userIsStreamOwner(rpcConnection.getSessionId(), participant, streamId)) {
			Participant moderator = isModerator ? participant : null;
			sessionManager.removeFilter(sessionManager.getSession(rpcConnection.getSessionId()), streamId, moderator,
					request.getId(), "removeFilter");
		} else {
			log.error("Error: participant {} is not a moderator", participant.getParticipantPublicId());
			throw new OpenViduException(Code.USER_UNAUTHORIZED_ERROR_CODE,
					"Unable to remove filter. The user does not have a valid token");
		}
	}

	private void execFilterMethod(RpcConnection rpcConnection, Request<JsonObject> request) {
		Participant participant;
		try {
			participant = sanityCheckOfSession(rpcConnection, "execFilterMethod");
		} catch (OpenViduException e) {
			return;
		}
		String streamId = getStringParam(request, ProtocolElements.FILTER_STREAMID_PARAM);
		String filterMethod = getStringParam(request, ProtocolElements.FILTER_METHOD_PARAM);
		JsonObject filterParams = new JsonParser().parse(getStringParam(request, ProtocolElements.FILTER_PARAMS_PARAM))
				.getAsJsonObject();
		boolean isModerator = this.sessionManager.isModeratorInSession(rpcConnection.getSessionId(), participant);

		// Allow executing filter method if the user is a MODERATOR (owning the stream
		// or other user's stream) or if the user is the owner of the stream
		if (isModerator || this.userIsStreamOwner(rpcConnection.getSessionId(), participant, streamId)) {
			Participant moderator = isModerator ? participant : null;
			sessionManager.execFilterMethod(sessionManager.getSession(rpcConnection.getSessionId()), streamId,
					filterMethod, filterParams, moderator, request.getId(), "execFilterMethod");
		} else {
			log.error("Error: participant {} is not a moderator", participant.getParticipantPublicId());
			throw new OpenViduException(Code.USER_UNAUTHORIZED_ERROR_CODE,
					"Unable to execute filter method. The user does not have a valid token");
		}
	}

	private void addFilterEventListener(RpcConnection rpcConnection, Request<JsonObject> request) {
		Participant participant;
		try {
			participant = sanityCheckOfSession(rpcConnection, "addFilterEventListener");
		} catch (OpenViduException e) {
			return;
		}
		String streamId = getStringParam(request, ProtocolElements.FILTER_STREAMID_PARAM);
		String eventType = getStringParam(request, ProtocolElements.FILTEREVENTLISTENER_EVENTTYPE_PARAM);
		boolean isModerator = this.sessionManager.isModeratorInSession(rpcConnection.getSessionId(), participant);

		// Allow adding a filter event listener if the user is a MODERATOR (owning the
		// stream or other user's stream) or if the user is the owner of the stream
		if (isModerator || this.userIsStreamOwner(rpcConnection.getSessionId(), participant, streamId)) {
			try {
				sessionManager.addFilterEventListener(sessionManager.getSession(rpcConnection.getSessionId()),
						participant, streamId, eventType);
				this.notificationService.sendResponse(participant.getParticipantPrivateId(), request.getId(),
						new JsonObject());
			} catch (OpenViduException e) {
				this.notificationService.sendErrorResponse(participant.getParticipantPrivateId(), request.getId(),
						new JsonObject(), e);
			}
		} else {
			log.error("Error: participant {} is not a moderator", participant.getParticipantPublicId());
			throw new OpenViduException(Code.USER_UNAUTHORIZED_ERROR_CODE,
					"Unable to add filter event listener. The user does not have a valid token");
		}
	}

	private void removeFilterEventListener(RpcConnection rpcConnection, Request<JsonObject> request) {
		Participant participant;
		try {
			participant = sanityCheckOfSession(rpcConnection, "removeFilterEventListener");
		} catch (OpenViduException e) {
			return;
		}
		String streamId = getStringParam(request, ProtocolElements.FILTER_STREAMID_PARAM);
		String eventType = getStringParam(request, ProtocolElements.FILTEREVENTLISTENER_EVENTTYPE_PARAM);
		boolean isModerator = this.sessionManager.isModeratorInSession(rpcConnection.getSessionId(), participant);

		// Allow removing a filter event listener if the user is a MODERATOR (owning the
		// stream or other user's stream) or if the user is the owner of the stream
		if (isModerator || this.userIsStreamOwner(rpcConnection.getSessionId(), participant, streamId)) {
			try {
				sessionManager.removeFilterEventListener(sessionManager.getSession(rpcConnection.getSessionId()),
						participant, streamId, eventType);
				this.notificationService.sendResponse(participant.getParticipantPrivateId(), request.getId(),
						new JsonObject());
			} catch (OpenViduException e) {
				this.notificationService.sendErrorResponse(participant.getParticipantPrivateId(), request.getId(),
						new JsonObject(), e);
			}
		} else {
			log.error("Error: participant {} is not a moderator", participant.getParticipantPublicId());
			throw new OpenViduException(Code.USER_UNAUTHORIZED_ERROR_CODE,
					"Unable to remove filter event listener. The user does not have a valid token");
		}
	}

	public void leaveRoomAfterConnClosed(String participantPrivateId, EndReason reason) {
		try {
			sessionManager.evictParticipant(this.sessionManager.getParticipant(participantPrivateId), null, null,
					reason);
			log.info("Evicted participant with privateId {}", participantPrivateId);
		} catch (OpenViduException e) {
			log.warn("Unable to evict: {}", e.getMessage());
			log.trace("Unable to evict user", e);
		}
	}

	@Override
	public void afterConnectionEstablished(Session rpcSession) {
		log.info("After connection established for WebSocket session: {}", rpcSession.getSessionId());
		/*if (rpcSession instanceof WebSocketServerSession) {
			InetAddress address;
			HttpHeaders headers = ((WebSocketServerSession) rpcSession).getWebSocketSession().getHandshakeHeaders();
			if (headers.containsKey("x-real-ip")) {
				address = InetAddress.getByName(headers.get("x-real-ip").get(0));
			} else {
				address = ((WebSocketServerSession) rpcSession).getWebSocketSession().getRemoteAddress().getAddress();
			}
			rpcSession.getAttributes().put("remoteAddress", address);

			HttpSession httpSession = (HttpSession) ((WebSocketServerSession) rpcSession).getWebSocketSession()
					.getAttributes().get("httpSession");
			rpcSession.getAttributes().put("httpSession", httpSession);
		}*/
	}

	@Override
	public void afterConnectionClosed(Session rpcSession, String status) throws Exception {
		log.info("After connection closed for WebSocket session: {} - Status: {}", rpcSession.getSessionId(), status);
		String rpcSessionId = rpcSession.getSessionId();
		String message = "";

		// update user online status in cache
        if (notificationService.getRpcConnection(rpcSessionId) != null)
		    cacheManage.updateUserOnlineStatus(notificationService.getRpcConnection(rpcSessionId).getUserUuid(),
				UserOnlineStatusEnum.offline);
		if ("Close for not receive ping from client".equals(status)) {
			message = "Evicting participant with private id {} because of a network disconnection";
		} else if (status == null) { // && this.webSocketBrokenPipeTransportError.remove(rpcSessionId) != null)) {
			try {
				Participant p = sessionManager.getParticipant(rpcSession.getSessionId());
				if (p != null) {
					message = "Evicting participant with private id {} because its websocket unexpectedly closed in the client side";
				}
			} catch (OpenViduException ex) {
				log.error("Exception:\n", ex);
			}
		}

		if (!message.isEmpty()) {
//			RpcConnection rpc = this.notificationService.closeRpcSession(rpcSessionId);
			RpcConnection rpc = this.notificationService.getRpcConnection(rpcSessionId);
			if (rpc != null && rpc.getSessionId() != null) {
				io.openvidu.server.core.Session session = this.sessionManager.getSession(rpc.getSessionId());
				Participant participant;
				if (session != null && (participant = session.getParticipantByPrivateId(rpc.getParticipantPrivateId())) != null) {
					log.info(message, rpc.getParticipantPrivateId());
//					leaveRoomAfterConnClosed(rpc.getParticipantPrivateId(), EndReason.networkDisconnect);
//					cacheManage.updateUserOnlineStatus(rpc.getUserUuid(), UserOnlineStatusEnum.offline);

					notifyUserBreakLine(session.getSessionId(), participant.getParticipantPublicId());
				}
			}
		}

		if (this.webSocketEOFTransportError.remove(rpcSessionId) != null) {
			log.warn(
					"Evicting participant with private id {} because a transport error took place and its web socket connection is now closed",
					rpcSession.getSessionId());
//			this.leaveRoomAfterConnClosed(rpcSessionId, EndReason.networkDisconnect);
			/*try {
				Participant p = this.sessionManager.getParticipant(rpcSessionId);
				RpcConnection c = this.notificationService.getRpcConnection(rpcSessionId);
				notifyUserBreakLine(p.getSessionId(), c.getUserId());
			} catch(OpenViduException e) {
				log.info("exception:{}", e);
			}*/
		}
	}

	@Override
	public void handleTransportError(Session rpcSession, Throwable exception) throws Exception {
		// update user online status in cache
		if (rpcSession != null) {
			if (notificationService.getRpcConnection(rpcSession.getSessionId()) != null)
				cacheManage.updateUserOnlineStatus(notificationService.getRpcConnection(rpcSession.getSessionId()).getUserUuid(),
						UserOnlineStatusEnum.offline);
			log.error("Transport exception for WebSocket session: {} - Exception: {}", rpcSession.getSessionId(),
					exception.getMessage());
			if ("IOException".equals(exception.getClass().getSimpleName())) {
//					&& "Broken pipe".equals(exception.getCause().getMessage())) {
				log.warn("Parcipant with private id {} unexpectedly closed the websocket", rpcSession.getSessionId());
			}
			if ("EOFException".equals(exception.getClass().getSimpleName())) {
				// Store WebSocket connection interrupted exception for this web socket to
				// automatically evict the participant on "afterConnectionClosed" event
				this.webSocketEOFTransportError.put(rpcSession.getSessionId(), true);
			}
		}
	}

	@Override
	public void handleUncaughtException(Session rpcSession, Exception exception) {
		log.error("Uncaught exception for WebSocket session: {} - Exception: {}", rpcSession.getSessionId(), exception);
	}

	@Override
	public List<String> allowedOrigins() {
		return Arrays.asList("*");
	}

	public static List<String> getStringListParam(Request<JsonObject> request, String key) {
		if (request.getParams() == null || request.getParams().get(key) == null || !request.getParams().get(key).isJsonArray()) {
			return null;
		}

		List<String> values = new ArrayList<>();
		request.getParams().get(key).getAsJsonArray().forEach(s -> values.add(s.getAsString()));
		return values;
	}

	public static String getStringParam(Request<JsonObject> request, String key) {
		if (request.getParams() == null || request.getParams().get(key) == null) {
			throw new RuntimeException("Request element '" + key + "' is missing in method '" + request.getMethod()
					+ "'. CHECK THAT 'openvidu-server' AND 'openvidu-browser' SHARE THE SAME VERSION NUMBER");
		}
		return request.getParams().get(key).getAsString();
	}

	public static String getStringOptionalParam(Request<JsonObject> request, String key) {
		if (request.getParams() == null || request.getParams().get(key) == null) {
			return null;
		}

		return request.getParams().get(key).getAsString();
	}

	public static int getIntParam(Request<JsonObject> request, String key) {
		if (request.getParams() == null || request.getParams().get(key) == null) {
			throw new RuntimeException("RMBER");
		}
		return request.getParams().get(key).getAsInt();
	}

	public static Integer getIntOptionalParam(Request<JsonObject> request, String key) {
		if (request.getParams() == null || request.getParams().get(key) == null) {
			return null;
		}

		return request.getParams().get(key).getAsInt();
	}

	public static Float getFloatOptionalParam(Request<JsonObject> request, String key) {
		if (request.getParams() == null || request.getParams().get(key) == null) {
			return null;
		}

		return request.getParams().get(key).getAsFloat();
	}

	public static boolean getBooleanParam(Request<JsonObject> request, String key) {
		if (request.getParams() == null || request.getParams().get(key) == null) {
			throw new RuntimeException("Request element '" + key + "' is missing in method '" + request.getMethod()
					+ "'. CHECK THAT 'openvidu-server' AND 'openvidu-browser' SHARE THE SAME VERSION NUMBER");
		}
		return request.getParams().get(key).getAsBoolean();
	}

	public static long getLongParam(Request<JsonObject> request, String key) {
		if (request.getParams() == null || request.getParams().get(key) == null) {
			throw new RuntimeException("Request element '" + key + "' is missing in method '" + request.getMethod()
					+ "'. CHECK THAT 'openvidu-server' AND 'openvidu-browser' SHARE THE SAME VERSION NUMBER");
		}
		return request.getParams().get(key).getAsLong();
	}

	public static JsonElement getParam(Request<JsonObject> request, String key) {
		if (request.getParams() == null || request.getParams().get(key) == null) {
			throw new RuntimeException("Request element '" + key + "' is missing in method '" + request.getMethod()
					+ "'. CHECK THAT 'openvidu-server' AND 'openvidu-browser' SHARE THE SAME VERSION NUMBER");
		}
		return request.getParams().get(key);
	}

	private Participant sanityCheckOfSession(RpcConnection rpcConnection, String methodName) throws OpenViduException {
		String participantPrivateId = rpcConnection.getParticipantPrivateId();
		String sessionId = rpcConnection.getSessionId();
		String errorMsg;

		if (sessionId == null) { // null when afterConnectionClosed
			errorMsg = "No session information found for participant with privateId " + participantPrivateId
					+ ". Using the admin method to evict the user.";
			log.warn(errorMsg);
			leaveRoomAfterConnClosed(participantPrivateId, null);
			throw new OpenViduException(Code.GENERIC_ERROR_CODE, errorMsg);
		} else {
			// Sanity check: don't call RPC method unless the id checks out
			Participant participant = sessionManager.getParticipant(sessionId, participantPrivateId);
			if (participant != null) {
				errorMsg = "Participant " + participant.getParticipantPublicId() + " is calling method '" + methodName
						+ "' in session " + sessionId;
				log.info(errorMsg);
				return participant;
			} else {
				errorMsg = "Participant with private id " + participantPrivateId + " not found in session " + sessionId
						+ ". Using the admin method to evict the user.";
				log.warn(errorMsg);
				leaveRoomAfterConnClosed(participantPrivateId, null);
				throw new OpenViduException(Code.GENERIC_ERROR_CODE, errorMsg);
			}
		}
	}

	private Participant sanityCheckOfSession(RpcConnection rpcConnection, StreamType streamType) throws OpenViduException {
		Participant participant = sessionManager.getParticipant(rpcConnection.getSessionId(),
				rpcConnection.getParticipantPrivateId(), streamType);
		if (participant == null) {
			leaveRoomAfterConnClosed(rpcConnection.getParticipantPrivateId(), null);
			throw new OpenViduException(Code.GENERIC_ERROR_CODE, "Participant not exists.");
		}
		return participant;
	}

	private Participant sanityCheckOfSession(RpcConnection rpcConnection, String participantPublicId, String methodName) throws OpenViduException {
		String participantPrivateId = rpcConnection.getParticipantPrivateId();
		String sessionId = rpcConnection.getSessionId();
		String errorMsg;

		if (sessionId == null) { // null when afterConnectionClosed
			errorMsg = "No session information found for participant with privateId " + participantPrivateId
					+ ". Using the admin method to evict the user.";
			log.warn(errorMsg);
			leaveRoomAfterConnClosed(participantPrivateId, null);
			throw new OpenViduException(Code.GENERIC_ERROR_CODE, errorMsg);
		} else {
			// Sanity check: don't call RPC method unless the id checks out
			Participant participant = sessionManager.getParticipantByPrivateAndPublicId(sessionId, participantPrivateId, participantPublicId);
			if (participant != null) {
				errorMsg = "Participant " + participant.getParticipantPublicId() + " is calling method '" + methodName
						+ "' in session " + sessionId;
				log.info(errorMsg);
				return participant;
			} else {
				errorMsg = "Participant with private id " + participantPrivateId + " not found in session " + sessionId
						+ ". Using the admin method to evict the user.";
				log.warn(errorMsg);
				leaveRoomAfterConnClosed(participantPrivateId, null);
				throw new OpenViduException(Code.GENERIC_ERROR_CODE, errorMsg);
			}
		}
	}

	private boolean userIsStreamOwner(String sessionId, Participant participant, String streamId) {
		return participant.getParticipantPrivateId()
				.equals(this.sessionManager.getParticipantPrivateIdFromStreamId(sessionId, streamId));
	}

	private void closeRoom(RpcConnection rpcConnection, Request<JsonObject> request) {
		String sessionId = getStringParam(request, ProtocolElements.CLOSE_ROOM_ID_PARAM);
        ErrorCodeEnum errCode = ErrorCodeEnum.SUCCESS;
		if (Objects.isNull(sessionManager.getSession(sessionId)) || sessionManager.getSession(sessionId).isClosed()) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.CONFERENCE_ALREADY_CLOSED);
            return ;
		}


		Participant participant = sessionManager.getParticipant(sessionId, rpcConnection.getParticipantPrivateId());
		if (!Objects.isNull(participant)) {
			if (participant.getRole() != OpenViduRole.MODERATOR)
				errCode = ErrorCodeEnum.PERMISSION_LIMITED;
		} else {
			// once participant reconnected, close the room directly without joining room
			// find the participant related to the previous connection and verify the operation permission
			Map userInfo = cacheManage.getUserInfoByUUID(rpcConnection.getUserUuid());
			participant = sessionManager.getParticipant(sessionId, String.valueOf(userInfo.get("reconnect")));
			if (!Objects.isNull(participant) && participant.getRole() != OpenViduRole.MODERATOR)
				errCode = ErrorCodeEnum.PERMISSION_LIMITED;

		}
		if (!ErrorCodeEnum.SUCCESS.equals(errCode)) {
			this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
					null, errCode);
			return ;
		}

		updateReconnectInfo(rpcConnection);
		this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
        this.sessionManager.unpublishAllStream(sessionId, EndReason.forceCloseSessionByUser);
        this.sessionManager.closeSession(sessionId, EndReason.forceCloseSessionByUser);
	}

	public ErrorCodeEnum cleanSession(String sessionId, String privateId, boolean checkModerator, EndReason reason) {
		if (Objects.isNull(sessionManager.getSession(sessionId))) {
			return ErrorCodeEnum.CONFERENCE_NOT_EXIST;
		}

		if (sessionManager.getSession(sessionId).isClosed()) {
			return ErrorCodeEnum.CONFERENCE_ALREADY_CLOSED;
		}

		if (checkModerator && sessionManager.getParticipant(sessionId, privateId).getRole() != OpenViduRole.MODERATOR) {
			return ErrorCodeEnum.PERMISSION_LIMITED;
		}

		// 1. notify all participant stop publish and receive stream.
		// 2. close session but can not disconnect the connection.
		this.sessionManager.unpublishAllStream(sessionId, reason);
		this.sessionManager.closeSession(sessionId, reason);

		return ErrorCodeEnum.SUCCESS;
	}

	private void inviteParticipant(RpcConnection rpcConnection, Request<JsonObject> request) {
		String sessionId = getStringParam(request, ProtocolElements.INVITE_PARTICIPANT_ID_PARAM);
		String sourceId = getStringParam(request, ProtocolElements.INVITE_PARTICIPANT_SOURCE_ID_PARAM);
		List<String> targetIds = getStringListParam(request, ProtocolElements.INVITE_PARTICIPANT_TARGET_ID_PARAM);
		if (CollectionUtils.isEmpty(targetIds)) {
			this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
					null, ErrorCodeEnum.REQUEST_PARAMS_ERROR);
			return;
		}

		if (sessionManager.getParticipant(sessionId, rpcConnection.getParticipantPrivateId()).getRole() != OpenViduRole.MODERATOR) {
			this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
					null, ErrorCodeEnum.PERMISSION_LIMITED);
			return;
		}

		SessionPreset preset = sessionManager.getPresetInfo(sessionId);
		if (!Objects.isNull(sessionManager.getSession(sessionId))) {
			Set<Participant> majorParts = sessionManager.getSession(sessionId).getMajorPartEachConnect();
			if ((majorParts.size() + targetIds.size()) > preset.getRoomCapacity()) {
				this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
						null, ErrorCodeEnum.ROOM_CAPACITY_LIMITED);
				return;
			}
		}

		Map userInfo = cacheManage.getUserInfoByUUID(rpcConnection.getUserUuid());
		String username = String.valueOf(userInfo.get("username"));
		String deviceName = userInfo.containsKey("deviceName") ? String.valueOf(userInfo.get("deviceName")) : null;

		// find the target rpc connection by targetId list and notify info.
		Collection<RpcConnection> rpcConnections = this.notificationService.getRpcConnections();
		targetIds.forEach(t -> {
			rpcConnections.forEach(c -> {
				if (Objects.equals(Long.valueOf(t), c.getUserId())) {
					JsonObject params = new JsonObject();
					params.addProperty(ProtocolElements.INVITE_PARTICIPANT_ID_PARAM, sessionId);
					params.addProperty(ProtocolElements.INVITE_PARTICIPANT_SOURCE_ID_PARAM, getStringParam(request, ProtocolElements.INVITE_PARTICIPANT_SOURCE_ID_PARAM));
					params.addProperty(ProtocolElements.INVITE_PARTICIPANT_TARGET_ID_PARAM, t);
					params.addProperty(ProtocolElements.INVITE_PARTICIPANT_USERNAME_PARAM, username);
					if (!StringUtils.isEmpty(deviceName)) {
						params.addProperty(ProtocolElements.INVITE_PARTICIPANT_DEVICE_NAME_PARAM, deviceName);
					}
					this.notificationService.sendNotification(c.getParticipantPrivateId(), ProtocolElements.INVITE_PARTICIPANT_METHOD, params);
				}
			});
		});

		this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
	}

	private void refuseInvite(RpcConnection rpcConnection, Request<JsonObject> request) {
		String sessionId = getStringParam(request, ProtocolElements.REFUSE_INVITE_ID_PARAM);
		String sourceId = getStringParam(request, ProtocolElements.REFUSE_INVITE_SOURCE_ID_PARAM);

		Set<Participant> participants = sessionManager.getParticipants(sessionId);
		if (!CollectionUtils.isEmpty(participants)) {
			JsonObject params = new JsonObject();
			params.addProperty(ProtocolElements.REFUSE_INVITE_ID_PARAM, sessionId);
			params.addProperty(ProtocolElements.REFUSE_INVITE_SOURCE_ID_PARAM, sourceId);

			for (Participant p: participants) {
				this.notificationService.sendNotification(p.getParticipantPrivateId(), ProtocolElements.REFUSE_INVITE_METHOD, params);
			}
		}

		this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
	}

	private void getPresetInfo(RpcConnection rpcConnection, Request<JsonObject> request) {
		String sessionId = getStringParam(request, ProtocolElements.GET_PRESET_INFO_ID_PARAM);
		SessionPreset preset = sessionManager.getPresetInfo(sessionId);
		JsonObject params = new JsonObject();

		params.addProperty(ProtocolElements.GET_PRESET_INFO_MIC_STATUS_PARAM, preset.getMicStatusInRoom().name());
		params.addProperty(ProtocolElements.GET_PRESET_INFO_SHARE_POWER_PARAM, preset.getSharePowerInRoom().name());
		params.addProperty(ProtocolElements.GET_PRESET_INFO_VIDEO_STATUS_PARAM, preset.getVideoStatusInRoom().name());
		params.addProperty(ProtocolElements.GET_PRESET_INFO_SUBJECT_PARAM, preset.getRoomSubject());
		this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), params);
	}

	private void setAudioSpeakerStatus(RpcConnection rpcConnection, Request<JsonObject> request) {
		String sessionId = getStringParam(request, ProtocolElements.SET_AUDIO_SPEAKER_ID_PARAM);
		String targetId = getStringOptionalParam(request, ProtocolElements.SET_AUDIO_SPEAKER_TARGET_ID_PARAM);
		String sourceId = getStringParam(request, ProtocolElements.SET_AUDIO_SPEAKER_SOURCE_ID_PARAM);
		String status = getStringParam(request, ProtocolElements.SET_AUDIO_SPEAKER_STATUS_PARAM);


		if (!Objects.equals(sourceId, targetId)
				&& sessionManager.getParticipant(sessionId, rpcConnection.getParticipantPrivateId()).getRole() != OpenViduRole.MODERATOR) {
			this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
					null, ErrorCodeEnum.PERMISSION_LIMITED);
			return;
		}

		if (!StringUtils.isEmpty(targetId)) {
			KurentoParticipant part = (KurentoParticipant) sessionManager.getParticipants(sessionId).stream().filter(s -> Long.valueOf(targetId)
					.compareTo(gson.fromJson(s.getClientMetadata(), JsonObject.class).get("clientData")
							.getAsLong()) == 0).findFirst().get();
			part.setSpeakerStatus(ParticipantSpeakerStatus.valueOf(status));
		}

		JsonObject params = new JsonObject();
		params.addProperty(ProtocolElements.SET_AUDIO_SPEAKER_ID_PARAM, sessionId);
		params.addProperty(ProtocolElements.SET_AUDIO_SPEAKER_SOURCE_ID_PARAM, sourceId);
		params.addProperty(ProtocolElements.SET_AUDIO_SPEAKER_TARGET_ID_PARAM, targetId);
		params.addProperty(ProtocolElements.SET_AUDIO_SPEAKER_STATUS_PARAM, status);
		Set<Participant> participants = sessionManager.getParticipants(sessionId);
		if (!CollectionUtils.isEmpty(participants)) {
			for (Participant p: participants) {
				this.notificationService.sendNotification(p.getParticipantPrivateId(), ProtocolElements.SET_AUDIO_SPEAKER_STATUS_METHOD, params);
			}
		}
		this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
	}

	private void setSharePower(RpcConnection rpcConnection, Request<JsonObject> request) {
		String sessionId = getStringParam(request, ProtocolElements.SET_SHARE_POWER_ID_PARAM);
		String sourceId = getStringParam(request, ProtocolElements.SET_SHARE_POWER_SOURCE_ID_PARAM);
		List<String> targetIds = getStringListParam(request, ProtocolElements.SET_SHARE_POWER_TARGET_IDS_PARAM);
		String status = getStringParam(request, ProtocolElements.SET_SHARE_POWER_STATUS_PARAM);

		if ((Objects.isNull(targetIds) || targetIds.isEmpty() || !Objects.equals(sourceId, targetIds.get(0)))
				&& sessionManager.getParticipant(sessionId, rpcConnection.getParticipantPrivateId()).getRole() != OpenViduRole.MODERATOR) {
			this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
					null, ErrorCodeEnum.PERMISSION_LIMITED);
			return;
		}

		JsonArray tsArray = new JsonArray();
		if (!Objects.isNull(targetIds) && !targetIds.isEmpty()) {
			targetIds.forEach(t -> tsArray.add(t));
		}

		JsonObject params = new JsonObject();
		params.addProperty(ProtocolElements.SET_SHARE_POWER_ID_PARAM, sessionId);
		params.addProperty(ProtocolElements.SET_SHARE_POWER_SOURCE_ID_PARAM, getStringParam(request, ProtocolElements.SET_SHARE_POWER_SOURCE_ID_PARAM));
		params.add(ProtocolElements.SET_SHARE_POWER_TARGET_IDS_PARAM, tsArray);
		params.addProperty(ProtocolElements.SET_SHARE_POWER_STATUS_PARAM, getStringParam(request, ProtocolElements.SET_SHARE_POWER_STATUS_PARAM));
		Set<Participant> participants = sessionManager.getParticipants(sessionId);
		if (!CollectionUtils.isEmpty(participants)) {
			participants.forEach(p -> {
				long userId = gson.fromJson(p.getClientMetadata(), JsonObject.class).get("clientData").getAsLong();
				if ((Objects.isNull(targetIds) || targetIds.isEmpty()) || targetIds.contains(String.valueOf(userId))) {
					p.setSharePowerStatus(ParticipantSharePowerStatus.valueOf(status));
				}
				this.notificationService.sendNotification(p.getParticipantPrivateId(), ProtocolElements.SET_SHARE_POWER_METHOD, params);
			});
		}

		this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
	}

	private void transferModerator(RpcConnection rpcConnection, Request<JsonObject> request) {
		// TODO. Need combine createConference and conferenceControl power. Not only used MODERATOR.
		String sessionId = getStringParam(request, ProtocolElements.TRANSFER_MODERATOR_ID_PARAM);
		String targetId = getStringParam(request, ProtocolElements.TRANSFER_MODERATOR_TARGET_ID_PARAM);
		String sourceId = getStringParam(request, ProtocolElements.TRANSFER_MODERATOR_SOURCE_ID_PARAM);

		if (sessionManager.getParticipant(sessionId, rpcConnection.getParticipantPrivateId()).getRole() != OpenViduRole.MODERATOR) {
			this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
					null, ErrorCodeEnum.PERMISSION_LIMITED);
			return;
		}

		JsonObject params = new JsonObject();

		params.addProperty(ProtocolElements.TRANSFER_MODERATOR_ID_PARAM, sessionId);
		params.addProperty(ProtocolElements.TRANSFER_MODERATOR_SOURCE_ID_PARAM, sourceId);
		params.addProperty(ProtocolElements.TRANSFER_MODERATOR_TARGET_ID_PARAM, targetId);
		sessionManager.getParticipants(sessionId).forEach(p -> {
			long userId = gson.fromJson(p.getClientMetadata(), JsonObject.class).get("clientData").getAsLong();
			if (Objects.equals(String.valueOf(userId), targetId)) {
				p.setRole(OpenViduRole.MODERATOR);
			}

			this.notificationService.sendNotification(p.getParticipantPrivateId(), ProtocolElements.TRANSFER_MODERATOR_METHOD, params);
		});

		this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
	}

	private void getOrgList(RpcConnection rpcConnection, Request<JsonObject> request) {
		Map userInfo = cacheManage.getUserInfoByUUID(rpcConnection.getUserUuid());
		log.info("deptId:{}", userInfo.get("deptId"));
		Long userDeptId = Long.valueOf(String.valueOf(userInfo.get("deptId")));

		// TODO eliminate unnecessary corp info according to the protocol
		JsonObject params = new JsonObject();
		params.addProperty(ProtocolElements.GET_ORG_ID_PARAM, 0);
		params.addProperty(ProtocolElements.GET_ORG_NAME_PARAM, "速递科技");

		JsonArray orgList = new JsonArray();
		List<Department> departments = departmentManage.getSubFirstLevelDepts(userDeptId);
		if (!CollectionUtils.isEmpty(departments)) {
			departments.forEach(dept -> {
				JsonObject org = new JsonObject();
				org.addProperty(ProtocolElements.GET_ORG_ID_PARAM, dept.getId());
				org.addProperty(ProtocolElements.GET_ORG_NAME_PARAM, dept.getDeptName());
				orgList.add(org);
			});
		}
		params.add(ProtocolElements.GET_ORG_LIST_PARAM, orgList);

		this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), params);
	}

	private void getUserDeviceList(RpcConnection rpcConnection, Request<JsonObject> request) {
		Long orgId = getLongParam(request, ProtocolElements.GET_USER_DEVICE_ORGID_PARAM);
		String localSerialNumber = rpcConnection.getSerialNumber();
		Long localUserId = rpcConnection.getUserId();
		Map<String, Long> onlineDeviceList = new HashMap<>();
		Map<Long, String> onlineUserList = new HashMap<>();
		for (RpcConnection c : notificationService.getRpcConnections()) {
			Map userInfo = cacheManage.getUserInfoByUUID(c.getUserUuid());
			if (Objects.isNull(userInfo)) continue;
			String status = String.valueOf(userInfo.get("status"));
			if (Objects.equals(UserOnlineStatusEnum.online.name(), status)) {
				onlineDeviceList.put(c.getSerialNumber(), c.getUserId());
				onlineUserList.put(c.getUserId(), c.getSerialNumber());
				log.info("Status:{}, privateId:{}, userId:{}, serialNumber:{}", status, c.getParticipantPrivateId(), c.getUserId(), c.getSerialNumber());
			}
		}

		JsonObject params = new JsonObject();
		JsonArray userDevList = new JsonArray();
        List<Device> deviceList = deviceManage.getSubDeviceByDeptId(orgId);
        if (!CollectionUtils.isEmpty(deviceList)) {
            deviceList.forEach(device -> {
				// 返回列表中排除自己的设备
            	if (device.getSerialNumber().equals(localSerialNumber))
            		return ;

                JsonObject devObj = new JsonObject();
				devObj.addProperty(ProtocolElements.GET_USER_DEVICE_DEVICE_NAME_PARAM, device.getDeviceName());
				devObj.addProperty("appShowName", device.getDeviceName());
				devObj.addProperty("appShowDesc", "(" + device.getDeviceModel() + ")");
                if (onlineDeviceList.containsKey(device.getSerialNumber())) {
					devObj.addProperty(ProtocolElements.GET_USER_DEVICE_STATUS_PARAM, DeviceStatus.online.name());
					devObj.addProperty(ProtocolElements.GET_USER_DEVICE_USER_ID_PARAM, Long.valueOf(onlineDeviceList.get(device.getSerialNumber())));
				} else {
					devObj.addProperty(ProtocolElements.GET_USER_DEVICE_STATUS_PARAM, DeviceStatus.offline.name());
				}

                userDevList.add(devObj);
            });
        }

        List<User> userList = userManage.getSubUserByDeptId(orgId);
        if (!CollectionUtils.isEmpty(userList)) {
            userList.forEach(user -> {
                // 返回列表中排除自己的用户
                if (user.getId().equals(localUserId))
                    return ;

                JsonObject userObj = new JsonObject();
                userObj.addProperty(ProtocolElements.GET_USER_DEVICE_USER_NAME_PARAM, user.getUsername());
                userObj.addProperty("appShowName", user.getUsername() + " (" + user.getTitle() + ")");
                userObj.addProperty("appShowDesc", "ID: " + user.getUuid());
                userObj.addProperty(ProtocolElements.GET_USER_DEVICE_STATUS_PARAM, onlineUserList.containsKey(user.getId()) ?
                        DeviceStatus.online.name() : DeviceStatus.offline.name());
                userObj.addProperty(ProtocolElements.GET_USER_DEVICE_USER_ID_PARAM, user.getId());

                userDevList.add(userObj);
            });
        }

		params.add(ProtocolElements.GET_USER_DEVICE_LIST_PARAM, userDevList);

		this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), params);
	}

	private void getDeviceInfo(RpcConnection rpcConnection, Request<JsonObject> request) {
		String serialNumber = rpcConnection.getSerialNumber();

		DeviceSearch deviceSearch = new DeviceSearch();
		deviceSearch.setSerialNumber(serialNumber);
		Device device = deviceMapper.selectBySearchCondition(deviceSearch);

		JsonObject params = new JsonObject();
		params.addProperty(ProtocolElements.GET_DEVICE_NANE_PARAM, device.getDeviceName());
		this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), params);
	}

	private void updateDeviceInfo(RpcConnection rpcConnection, Request<JsonObject> request) {
		String deviceName = getStringParam(request, ProtocolElements.UPDATE_DEVICE_NANE_PARAM);
		String serialNumber = rpcConnection.getSerialNumber();

		DeviceSearch deviceSearch = new DeviceSearch();
		deviceSearch.setSerialNumber(serialNumber);
		Device device = deviceMapper.selectBySearchCondition(deviceSearch);

		device.setDeviceName(deviceName);
		device.setUpdateTime(new Date());
		deviceMapper.updateByPrimaryKey(device);
		this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
	}

	private boolean isModerator(String role) {
		// TODO. Fixme. user account have moderator power.
		if (Objects.equals(OpenViduRole.MODERATOR.name(), role)) {
//		if (OpenViduRole.MODERATOR.equals(OpenViduRole.valueOf(role))) {
			return true;
		}
		return false;
	}

	public SessionManager getSessionManager() { return this.sessionManager; }

	public void notifyRoomCountdown(String sessionId, long remainTime) {
		JsonObject params = new JsonObject();

		params.addProperty(ProtocolElements.ROOM_COUNTDOWN_INFO_ID_PARAM, sessionId);
		params.addProperty(ProtocolElements.ROOM_COUNTDOWN_TIME_PARAM, remainTime);
		sessionManager.getParticipants(sessionId).forEach(p ->
				notificationService.sendNotification(p.getParticipantPrivateId(), ProtocolElements.ROOM_COUNTDOWN_METHOD, params));
	}

	private void roomDelay(RpcConnection rpcConnection, Request<JsonObject> request) {
		String sessionId = getStringParam(request, ProtocolElements.ROOM_DELAY_ID_PARAM);

		if (sessionManager.getSession(sessionId).getConfDelayTime() > openviduConfig.getVoipDelayMaxTime() * 60 * 60) {
			log.warn("conference:{} delay too long time:{} hour.", sessionId, openviduConfig.getVoipDelayMaxTime());
			notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.CONFERENCE_TOO_LONG);
			return ;
		}

		sessionManager.getSession(sessionId).incDelayConfCnt();
		sessionManager.getSession(sessionId).getParticipants().forEach(p ->
				notificationService.sendNotification(p.getParticipantPrivateId(), ProtocolElements.ROOM_DELAY_METHOD, new JsonObject()));
		this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
	}

	private void notifyUserBreakLine(String sessionId, String publicId) {
		JsonObject params = new JsonObject();
		params.addProperty(ProtocolElements.USER_BREAK_LINE_CONNECTION_ID_PARAM, publicId);

		sessionManager.getParticipants(sessionId).forEach(p -> {
			RpcConnection rpc = notificationService.getRpcConnection(p.getParticipantPrivateId());
			if (rpc != null) {
				if (Objects.equals(cacheManage.getUserInfoByUUID(rpc.getUserUuid()).get("status"), UserOnlineStatusEnum.online.name())) {
					notificationService.sendNotification(p.getParticipantPrivateId(), ProtocolElements.USER_BREAK_LINE_METHOD, params);
				}
			}
		});
	}

	private void getNotFinishedRoom(RpcConnection rpcConnection, Request<JsonObject> request) {
		JsonObject params = new JsonObject();
		Map userInfo = cacheManage.getUserInfoByUUID(rpcConnection.getUserUuid());
		if (Objects.isNull(userInfo)) {
			log.warn("local userInfo:{}", userInfo);
			notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
					null, ErrorCodeEnum.SERVER_UNKNOWN_ERROR);
			return ;
		}

		String oldPrivateId = String.valueOf(userInfo.get("reconnect"));
		log.info("userInfo status:{}, reconnect:{}", userInfo.get("status"), userInfo.get("reconnect"));
		if (!Objects.equals(UserOnlineStatusEnum.reconnect.name(), userInfo.get("status")) ||
				StringUtils.isEmpty(oldPrivateId)) {
			log.info("---------------------------------------------");
			this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), params);
			return ;
		}

		RpcConnection oldRpcConnection = notificationService.getRpcConnection(oldPrivateId);
		try {
			KurentoParticipant p = (KurentoParticipant) sessionManager.getParticipant(oldRpcConnection.getParticipantPrivateId());
            if (!Objects.isNull(p)) {
                // room info
                params.addProperty(ProtocolElements.GET_NOT_FINISHED_ROOM_ID_PARAM, p.getSessionId());
                params.addProperty(ProtocolElements.GET_NOT_FINISHED_ROOM_SUBJECT_PARAM, p.getRoomSubject());
                String roomPwd = sessionManager.getSession(p.getSessionId()).getConference().getPassword();
                params.addProperty(ProtocolElements.GET_NOT_FINISHED_ROOM_PASSWORD_PARAM, !StringUtils.isEmpty(roomPwd) ? roomPwd : "");
                params.addProperty(ProtocolElements.GET_NOT_FINISHED_ROOM_REMAINTIME_PARAM, p.getSession().getConfRemainTime());

                // participant info.
                params.addProperty(ProtocolElements.GET_NOT_FINISHED_ROOM_ROLE_PARAM, p.getRole().name());
                params.addProperty(ProtocolElements.GET_NOT_FINISHED_ROOM_AUDIOACTIVE_PARAM, p.isStreaming() && p.getPublisherMediaOptions().isAudioActive());
                params.addProperty(ProtocolElements.GET_NOT_FINISHED_ROOM_VIDEOACTIVE_PARAM, p.isStreaming() && p.getPublisherMediaOptions().isVideoActive());
                params.addProperty(ProtocolElements.GET_NOT_FINISHED_ROOM_SPEAKERSTATUS_PARAM, p.getSpeakerStatus().name());
                params.addProperty(ProtocolElements.GET_NOT_FINISHED_ROOM_HANDSTATUS_PARAM, p.getHandStatus().name());
                params.addProperty(ProtocolElements.GET_NOT_FINISHED_ROOM_SHARESTATUS_PARAM, p.getShareStatus().name());
            }
		} catch (OpenViduException e) {
			log.warn("the privateId:{} not belong any session.", oldPrivateId);
		}

		this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), params);
	}

	private boolean updateReconnectInfo(RpcConnection rpcConnection) {
		try {
			Map userInfo = cacheManage.getUserInfoByUUID(rpcConnection.getUserUuid());
			if (Objects.isNull(userInfo)) {
				log.warn("user:{} info is null.", rpcConnection.getUserUuid());
				return false;
			}

			if (Objects.equals(UserOnlineStatusEnum.reconnect.name(), userInfo.get("status"))) {
				log.info("reconnect userId:{} mac:{}", rpcConnection.getUserId(), rpcConnection.getMacAddr());
				String oldPrivateId = String.valueOf(userInfo.get("reconnect"));
				if (StringUtils.isEmpty(oldPrivateId)) {
					log.warn("reconnect privateId:{}", oldPrivateId);
					return false;
				}

				RpcConnection oldRpcConnection = notificationService.getRpcConnection(oldPrivateId);
				cacheManage.updateUserOnlineStatus(rpcConnection.getUserUuid(), UserOnlineStatusEnum.online);
				cacheManage.updateReconnectInfo(rpcConnection.getUserUuid(), "");
				leaveRoomAfterConnClosed(oldPrivateId, EndReason.sessionClosedByServer);
//				accessOut(oldRpcConnection, null);
				sessionManager.accessOut(oldRpcConnection);
				return true;
			}
		} catch (Exception e) {
			log.warn("exception:{}", e);
			return false;
		}

		return true;
	}

	public RpcNotificationService getNotificationService() {
		return notificationService;
	}
}
