/*
 *
 * Copyright (C) 2017 The LineageOS Project
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.telephony;

import static com.android.internal.telephony.RILConstants.*;

import android.content.Context;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.AsyncResult;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.SignalStrength;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import java.io.IOException;
import java.io.InputStream;

import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.uicc.IccRefreshResponse;

public class MediaTekRIL extends RIL implements CommandsInterface {

    // MediaTek Custom States
    static final int RIL_REQUEST_MTK_BASE = 2000;
    static final int RIL_REQUEST_HANGUP_ALL = (RIL_REQUEST_MTK_BASE + 0);
    static final int RIL_REQUEST_GET_COLP = (RIL_REQUEST_MTK_BASE + 1);
    static final int RIL_REQUEST_SET_COLP = (RIL_REQUEST_MTK_BASE + 2);
    static final int RIL_REQUEST_GET_COLR = (RIL_REQUEST_MTK_BASE + 3);
    static final int RIL_REQUEST_GET_CCM = (RIL_REQUEST_MTK_BASE + 4);
    static final int RIL_REQUEST_GET_ACM = (RIL_REQUEST_MTK_BASE + 5);
    static final int RIL_REQUEST_GET_ACMMAX = (RIL_REQUEST_MTK_BASE + 6);
    static final int RIL_REQUEST_GET_PPU_AND_CURRENCY = (RIL_REQUEST_MTK_BASE + 7);
    static final int RIL_REQUEST_SET_ACMMAX = (RIL_REQUEST_MTK_BASE + 8);
    static final int RIL_REQUEST_RESET_ACM = (RIL_REQUEST_MTK_BASE + 9);
    static final int RIL_REQUEST_SET_PPU_AND_CURRENCY = (RIL_REQUEST_MTK_BASE + 10);
    static final int RIL_REQUEST_RADIO_POWEROFF = (RIL_REQUEST_MTK_BASE + 11);       
    static final int RIL_REQUEST_DUAL_SIM_MODE_SWITCH = (RIL_REQUEST_MTK_BASE + 12); 
    static final int RIL_REQUEST_QUERY_PHB_STORAGE_INFO = (RIL_REQUEST_MTK_BASE + 13);       
    static final int RIL_REQUEST_WRITE_PHB_ENTRY = (RIL_REQUEST_MTK_BASE + 14);      
    static final int RIL_REQUEST_READ_PHB_ENTRY = (RIL_REQUEST_MTK_BASE + 15);       
    static final int RIL_REQUEST_SET_GPRS_CONNECT_TYPE = (RIL_REQUEST_MTK_BASE + 16);
    static final int RIL_REQUEST_SET_GPRS_TRANSFER_TYPE = (RIL_REQUEST_MTK_BASE + 17);
    static final int RIL_REQUEST_MOBILEREVISION_AND_IMEI = (RIL_REQUEST_MTK_BASE + 18);//Add by mtk80372 for Barcode Number
    static final int RIL_REQUEST_QUERY_SIM_NETWORK_LOCK = (RIL_REQUEST_MTK_BASE + 19);
    static final int RIL_REQUEST_SET_SIM_NETWORK_LOCK = (RIL_REQUEST_MTK_BASE + 20);
    static final int RIL_REQUEST_SET_SCRI = (RIL_REQUEST_MTK_BASE + 21);   
    /* cage_vt start */
    static final int RIL_REQUEST_VT_DIAL = (RIL_REQUEST_MTK_BASE + 22);
    static final int RIL_REQUEST_VOICE_ACCEPT = (RIL_REQUEST_MTK_BASE + 32);
    /* cage_vt end */
    static final int RIL_REQUEST_BTSIM_CONNECT = (RIL_REQUEST_MTK_BASE + 23);
    static final int RIL_REQUEST_BTSIM_DISCONNECT_OR_POWEROFF = (RIL_REQUEST_MTK_BASE + 24);
    static final int RIL_REQUEST_BTSIM_POWERON_OR_RESETSIM = (RIL_REQUEST_MTK_BASE + 25);
    static final int RIL_REQUEST_BTSIM_TRANSFERAPDU = (RIL_REQUEST_MTK_BASE + 26);
    static final int RIL_REQUEST_EMERGENCY_DIAL = (RIL_REQUEST_MTK_BASE + 27);
    static final int RIL_REQUEST_SET_NETWORK_SELECTION_MANUAL_WITH_ACT = (RIL_REQUEST_MTK_BASE + 28);
    static final int RIL_REQUEST_QUERY_ICCID = (RIL_REQUEST_MTK_BASE + 29);
    static final int RIL_REQUEST_SIM_AUTHENTICATION = (RIL_REQUEST_MTK_BASE + 30);   
    static final int RIL_REQUEST_USIM_AUTHENTICATION = (RIL_REQUEST_MTK_BASE + 31); 
    static final int RIL_REQUEST_RADIO_POWERON = (RIL_REQUEST_MTK_BASE + 33);
    static final int RIL_REQUEST_GET_SMS_SIM_MEM_STATUS = (RIL_REQUEST_MTK_BASE + 34);
    static final int RIL_REQUEST_FORCE_RELEASE_CALL = (RIL_REQUEST_MTK_BASE + 35);
    static final int RIL_REQUEST_SET_CALL_INDICATION = (RIL_REQUEST_MTK_BASE + 36);
    static final int RIL_REQUEST_REPLACE_VT_CALL = (RIL_REQUEST_MTK_BASE + 37);
    /* 3G switch start */
    static final int RIL_REQUEST_GET_3G_CAPABILITY = (RIL_REQUEST_MTK_BASE + 38);
    static final int RIL_REQUEST_SET_3G_CAPABILITY = (RIL_REQUEST_MTK_BASE + 39);
    /* 3G switch end */
    /* User controlled PLMN selector with Access Technology  begin */
    static final int RIL_REQUEST_GET_POL_CAPABILITY = (RIL_REQUEST_MTK_BASE + 40);
    static final int RIL_REQUEST_GET_POL_LIST = (RIL_REQUEST_MTK_BASE + 41);
    static final int RIL_REQUEST_SET_POL_ENTRY = (RIL_REQUEST_MTK_BASE + 42);
    /* User controlled PLMN selector with Access Technology  end */
    /* UPB start */
    static final int RIL_REQUEST_QUERY_UPB_CAPABILITY = (RIL_REQUEST_MTK_BASE + 43);
    static final int RIL_REQUEST_EDIT_UPB_ENTRY = (RIL_REQUEST_MTK_BASE + 44);
    static final int RIL_REQUEST_DELETE_UPB_ENTRY = (RIL_REQUEST_MTK_BASE + 45);
    static final int RIL_REQUEST_READ_UPB_GAS_LIST = (RIL_REQUEST_MTK_BASE + 46);
    static final int RIL_REQUEST_READ_UPB_GRP = (RIL_REQUEST_MTK_BASE + 47);
    static final int RIL_REQUEST_WRITE_UPB_GRP = (RIL_REQUEST_MTK_BASE + 48);
    /* UPB end */
    static final int RIL_REQUEST_DISABLE_VT_CAPABILITY = (RIL_REQUEST_MTK_BASE + 49);
    static final int RIL_REQUEST_HANGUP_ALL_EX = (RIL_REQUEST_MTK_BASE + 50);
    static final int RIL_REQUEST_SET_SIM_RECOVERY_ON = (RIL_REQUEST_MTK_BASE + 51);
    static final int RIL_REQUEST_GET_SIM_RECOVERY_ON = (RIL_REQUEST_MTK_BASE + 52);
    static final int RIL_REQUEST_SET_TRM = (RIL_REQUEST_MTK_BASE + 53);
    static final int RIL_REQUEST_DETECT_SIM_MISSING = (RIL_REQUEST_MTK_BASE + 54);
    static final int RIL_REQUEST_GET_CALIBRATION_DATA = (RIL_REQUEST_MTK_BASE + 55);

     //For LGE APIs start
    static final int RIL_REQUEST_GET_PHB_STRING_LENGTH = (RIL_REQUEST_MTK_BASE + 56);
    static final int RIL_REQUEST_GET_PHB_MEM_STORAGE = (RIL_REQUEST_MTK_BASE + 57);
    static final int RIL_REQUEST_SET_PHB_MEM_STORAGE = (RIL_REQUEST_MTK_BASE + 58);
    static final int RIL_REQUEST_READ_PHB_ENTRY_EXT = (RIL_REQUEST_MTK_BASE + 59);
    static final int RIL_REQUEST_WRITE_PHB_ENTRY_EXT = (RIL_REQUEST_MTK_BASE + 60);
    
    // requests for read/write EFsmsp
    static final int RIL_REQUEST_GET_SMS_PARAMS = (RIL_REQUEST_MTK_BASE + 61);
    static final int RIL_REQUEST_SET_SMS_PARAMS = (RIL_REQUEST_MTK_BASE + 62);

    // NFC SEEK start
    static final int RIL_REQUEST_SIM_TRANSMIT_BASIC = (RIL_REQUEST_MTK_BASE + 63);
    static final int RIL_REQUEST_SIM_OPEN_CHANNEL = (RIL_REQUEST_MTK_BASE + 64);
    static final int RIL_REQUEST_SIM_CLOSE_CHANNEL = (RIL_REQUEST_MTK_BASE + 65);
    static final int RIL_REQUEST_SIM_TRANSMIT_CHANNEL = (RIL_REQUEST_MTK_BASE + 66);
    static final int RIL_REQUEST_SIM_GET_ATR = (RIL_REQUEST_MTK_BASE + 67);
    // NFC SEEK end

    // CB extension
    static final int RIL_REQUEST_SET_CB_CHANNEL_CONFIG_INFO = (RIL_REQUEST_MTK_BASE + 68);
    static final int RIL_REQUEST_SET_CB_LANGUAGE_CONFIG_INFO = (RIL_REQUEST_MTK_BASE + 69);
    static final int RIL_REQUEST_GET_CB_CONFIG_INFO = (RIL_REQUEST_MTK_BASE + 70);
    static final int RIL_REQUEST_SET_ALL_CB_LANGUAGE_ON = (RIL_REQUEST_MTK_BASE + 71);
    // CB extension
    
    static final int RIL_REQUEST_SET_ETWS = (RIL_REQUEST_MTK_BASE + 72);

    // [New R8 modem FD]
    static final int RIL_REQUEST_SET_FD_MODE = (RIL_REQUEST_MTK_BASE + 73);

    static final int RIL_REQUEST_SIM_OPEN_CHANNEL_WITH_SW = (RIL_REQUEST_MTK_BASE + 74); // NFC SEEK

    static final int RIL_REQUEST_SET_CLIP = (RIL_REQUEST_MTK_BASE + 75);

    //MTK-START [mtk80776] WiFi Calling
    static final int RIL_REQUEST_UICC_SELECT_APPLICATION = (RIL_REQUEST_MTK_BASE + 76);
    static final int RIL_REQUEST_UICC_DEACTIVATE_APPLICATION = (RIL_REQUEST_MTK_BASE + 77);
    static final int RIL_REQUEST_UICC_APPLICATION_IO = (RIL_REQUEST_MTK_BASE + 78);
    static final int RIL_REQUEST_UICC_AKA_AUTHENTICATE = (RIL_REQUEST_MTK_BASE + 79);
    static final int RIL_REQUEST_UICC_GBA_AUTHENTICATE_BOOTSTRAP = (RIL_REQUEST_MTK_BASE + 80);
    static final int RIL_REQUEST_UICC_GBA_AUTHENTICATE_NAF = (RIL_REQUEST_MTK_BASE + 81);
    //MTK-END [mtk80776] WiFi Calling
    static final int RIL_REQUEST_STK_EVDL_CALL_BY_AP = (RIL_REQUEST_MTK_BASE + 82);

    static final int RIL_UNSOL_MTK_BASE = 3000; 
    static final int RIL_UNSOL_NEIGHBORING_CELL_INFO = (RIL_UNSOL_MTK_BASE + 0);
    static final int RIL_UNSOL_NETWORK_INFO = (RIL_UNSOL_MTK_BASE + 1);
    static final int RIL_UNSOL_CALL_FORWARDING = (RIL_UNSOL_MTK_BASE + 2);
    static final int RIL_UNSOL_CRSS_NOTIFICATION = (RIL_UNSOL_MTK_BASE + 3);
    static final int RIL_UNSOL_CALL_PROGRESS_INFO = (RIL_UNSOL_MTK_BASE + 4);
    static final int RIL_UNSOL_PHB_READY_NOTIFICATION = (RIL_UNSOL_MTK_BASE + 5);
    static final int RIL_UNSOL_SPEECH_INFO = (RIL_UNSOL_MTK_BASE + 6);
    static final int RIL_UNSOL_SIM_INSERTED_STATUS = (RIL_UNSOL_MTK_BASE + 7);
    static final int RIL_UNSOL_RADIO_TEMPORARILY_UNAVAILABLE = (RIL_UNSOL_MTK_BASE + 8);
    static final int RIL_UNSOL_ME_SMS_STORAGE_FULL = (RIL_UNSOL_MTK_BASE + 9);
    static final int RIL_UNSOL_SMS_READY_NOTIFICATION = (RIL_UNSOL_MTK_BASE + 10);
    static final int RIL_UNSOL_SCRI_RESULT = (RIL_UNSOL_MTK_BASE + 11);
    /* cage_vt start */
    static final int RIL_UNSOL_VT_STATUS_INFO = (RIL_UNSOL_MTK_BASE + 12);
    static final int RIL_UNSOL_VT_RING_INFO = (RIL_UNSOL_MTK_BASE + 13);
    /* cage_vt end */
    static final int RIL_UNSOL_INCOMING_CALL_INDICATION = (RIL_UNSOL_MTK_BASE + 14);
    static final int RIL_UNSOL_SIM_MISSING = (RIL_UNSOL_MTK_BASE + 15);
    static final int RIL_UNSOL_GPRS_DETACH = (RIL_UNSOL_MTK_BASE + 16);
    //MTK-START [mtk04070][120208][ALPS00233196] ATCI for unsolicited response
    static final int RIL_UNSOL_ATCI_RESPONSE = (RIL_UNSOL_MTK_BASE + 17);
    //MTK-END [mtk04070][120208][ALPS00233196] ATCI for unsolicited response
    static final int RIL_UNSOL_SIM_RECOVERY= (RIL_UNSOL_MTK_BASE + 18);
    static final int RIL_UNSOL_VIRTUAL_SIM_ON = (RIL_UNSOL_MTK_BASE + 19);
    static final int RIL_UNSOL_VIRTUAL_SIM_OFF = (RIL_UNSOL_MTK_BASE + 20);
    static final int RIL_UNSOL_INVALID_SIM = (RIL_UNSOL_MTK_BASE + 21); 
    static final int RIL_UNSOL_RESPONSE_PS_NETWORK_STATE_CHANGED = (RIL_UNSOL_MTK_BASE + 22);
    static final int RIL_UNSOL_RESPONSE_ACMT = (RIL_UNSOL_MTK_BASE + 23);
    static final int RIL_UNSOL_EF_CSP_PLMN_MODE_BIT = (RIL_UNSOL_MTK_BASE + 24);
    static final int RIL_UNSOL_IMEI_LOCK = (RIL_UNSOL_MTK_BASE + 25);
    static final int RIL_UNSOL_RESPONSE_MMRR_STATUS_CHANGED = (RIL_UNSOL_MTK_BASE + 26);
    static final int RIL_UNSOL_SIM_PLUG_OUT = (RIL_UNSOL_MTK_BASE + 27);
    static final int RIL_UNSOL_SIM_PLUG_IN = (RIL_UNSOL_MTK_BASE + 28);
    static final int RIL_UNSOL_RESPONSE_ETWS_NOTIFICATION = (RIL_UNSOL_MTK_BASE + 29);
    static final int RIL_UNSOL_CNAP = (RIL_UNSOL_MTK_BASE + 30);
    static final int RIL_UNSOL_STK_EVDL_CALL = (RIL_UNSOL_MTK_BASE + 31);

    private boolean dataAllowed = false;
    private String voiceRegState = "0";
    private String voiceDataTech = "0";

    public MediaTekRIL(Context context, int preferredNetworkType, int cdmaSubscription, Integer instanceId) {
	    super(context, preferredNetworkType, cdmaSubscription, instanceId);
    }

    @Override
    public void setInitialAttachApn(String apn, String protocol, int authType, String username,
             String password, Message result) {
        riljLog("setInitialAttachApn");

        dataAllowed = true; //If we should attach to an APN, we actually need to register data

        riljLog("Faking VoiceNetworkState");
        mVoiceNetworkStateRegistrants.notifyRegistrants(new AsyncResult(null, null, null));

        if (result != null) {
            AsyncResult.forMessage(result, null, null);
            result.sendToTarget();
         }
    }

    @Override
    protected RILRequest
    processSolicited (Parcel p, int type) {
        int serial, error, request;
        RILRequest rr;
        int dataPosition = p.dataPosition(); // save off position within the Parcel

        serial = p.readInt();
        error = p.readInt();

        rr = mRequestList.get(serial);
        if (rr == null || error != 0 || p.dataAvail() <= 0) {
            p.setDataPosition(dataPosition);
            return super.processSolicited(p, type);
        }

        try { switch (rr.mRequest) {
           case RIL_REQUEST_VOICE_REGISTRATION_STATE:
               String voiceRegStates[] = (String [])responseStrings(p);

               riljLog("VoiceRegistrationState response");

               if (voiceRegStates.length > 0 && voiceRegStates[0] != null) {
                   voiceRegState = voiceRegStates[0];
               }

               if (voiceRegStates.length > 3 && voiceRegStates[3] != null) {
                   voiceDataTech = voiceRegStates[3];
               }

               if (RILJ_LOGD) riljLog(rr.serialString() + "< " + requestToString(rr.mRequest)
                               + " " + retToString(rr.mRequest, voiceRegStates));

               if (rr.mResult != null) {
                       AsyncResult.forMessage(rr.mResult, voiceRegStates, null);
                       rr.mResult.sendToTarget();
               }
               mRequestList.remove(serial);
               break;
           case RIL_REQUEST_DATA_REGISTRATION_STATE:
               String dataRegStates[] = (String [])responseStrings(p);

               riljLog("DataRegistrationState response");

               if (dataRegStates.length > 0) {
                   if (dataRegStates[0] != null) {
                       if (!dataAllowed) {
                           if (Integer.parseInt(dataRegStates[0]) > 0) {
                               riljLog("Modifying dataRegState to 0 from " + dataRegStates[0]);
                               dataRegStates[0] = "0";
                           }
                       } else {
                           if ((Integer.parseInt(dataRegStates[0]) != 1) && (Integer.parseInt(dataRegStates[0]) != 5) &&
                               ((Integer.parseInt(voiceRegState) == 1) || (Integer.parseInt(voiceRegState) == 5))) {
                               riljLog("Modifying dataRegState from " + dataRegStates[0] + " to " + voiceRegState);
                               dataRegStates[0] = voiceRegState;
                               if (dataRegStates.length > 3) {
                                   riljLog("Modifying dataTech from " + dataRegStates[3] + " to " + voiceDataTech);
                                   dataRegStates[3] = voiceDataTech;
                               }
                           }
                       }
                   }
               }

               if (RILJ_LOGD) riljLog(rr.serialString() + "< " + requestToString(rr.mRequest)
                               + " " + retToString(rr.mRequest, dataRegStates));

               if (rr.mResult != null) {
                       AsyncResult.forMessage(rr.mResult, dataRegStates, null);
                       rr.mResult.sendToTarget();
               }
               mRequestList.remove(serial);
               break;
           default:
               p.setDataPosition(dataPosition);
               return super.processSolicited(p, type);
        }} catch (Throwable tr) {
                // Exceptions here usually mean invalid RIL responses

                Rlog.w(RILJ_LOG_TAG, rr.serialString() + "< "
                                + requestToString(rr.mRequest)
                                + " exception, possible invalid RIL response", tr);

                if (rr.mResult != null) {
                        AsyncResult.forMessage(rr.mResult, null, tr);
                        rr.mResult.sendToTarget();
                }
                return rr;
        }

        return rr;
    }

    @Override
    protected void
    processUnsolicited (Parcel p, int type) {
        Object ret;
        int dataPosition = p.dataPosition();
        int response = p.readInt();

        switch(response) {
            case RIL_UNSOL_CALL_PROGRESS_INFO: ret = responseStrings(p); break;
            case RIL_UNSOL_INCOMING_CALL_INDICATION: ret = responseStrings(p); break;
            case RIL_UNSOL_RESPONSE_PS_NETWORK_STATE_CHANGED: ret =  responseVoid(p); break;
            case RIL_UNSOL_SIM_INSERTED_STATUS: ret = responseInts(p); break;
            case RIL_UNSOL_SIM_MISSING: ret = responseInts(p); break;
            case RIL_UNSOL_SIM_PLUG_OUT: ret = responseInts(p); break;
            case RIL_UNSOL_SIM_PLUG_IN: ret = responseInts(p); break;
            case RIL_UNSOL_SMS_READY_NOTIFICATION: ret = responseVoid(p); break;
            case RIL_UNSOL_RESPONSE_RADIO_STATE_CHANGED: ret =  responseVoid(p); break;
            default:
                p.setDataPosition(dataPosition);
                super.processUnsolicited(p, type);
                return;
        }

        // To avoid duplicating code from RIL.java, we rewrite some response codes to fit
        // AOSP's one (when they do the same effect)
        boolean rewindAndReplace = false;
        int newResponseCode = 0;

        switch (response) {
            case RIL_UNSOL_CALL_PROGRESS_INFO:
		rewindAndReplace = true;
		newResponseCode = RIL_UNSOL_RESPONSE_CALL_STATE_CHANGED;
		break;
            case RIL_UNSOL_INCOMING_CALL_INDICATION:
		setCallIndication((String[])ret);
                rewindAndReplace = true;
		newResponseCode = RIL_UNSOL_RESPONSE_CALL_STATE_CHANGED;
		break;
            case RIL_UNSOL_RESPONSE_PS_NETWORK_STATE_CHANGED:
                rewindAndReplace = true;
                newResponseCode = RIL_UNSOL_RESPONSE_VOICE_NETWORK_STATE_CHANGED;
                break;
            case RIL_UNSOL_SIM_INSERTED_STATUS:
            case RIL_UNSOL_SIM_MISSING:
            case RIL_UNSOL_SIM_PLUG_OUT:
            case RIL_UNSOL_SIM_PLUG_IN:
                rewindAndReplace = true;
                newResponseCode = RIL_UNSOL_RESPONSE_SIM_STATUS_CHANGED;
                break;
            case RIL_UNSOL_SMS_READY_NOTIFICATION:
                break;
            case RIL_UNSOL_RESPONSE_RADIO_STATE_CHANGED:
		// intercept and send GPRS_TRANSFER_TYPE and GPRS_CONNECT_TYPE to RIL
	        setRadioStateFromRILInt(p.readInt());
		rewindAndReplace = true;
		newResponseCode = RIL_UNSOL_RESPONSE_RADIO_STATE_CHANGED;
		break;
            default:
                Rlog.i(RILJ_LOG_TAG, "Unprocessed unsolicited known MTK response: " + response);
        }

        if (rewindAndReplace) {
            Rlog.w(RILJ_LOG_TAG, "Rewriting MTK unsolicited response to " + newResponseCode);

            // Rewrite
            p.setDataPosition(dataPosition);
            p.writeInt(newResponseCode);

            // And rewind again in front
            p.setDataPosition(dataPosition);

            super.processUnsolicited(p, type);
        }
    }    

    private void setCallIndication(String[] incomingCallInfo) {

	RILRequest rr = RILRequest.obtain(RIL_REQUEST_SET_CALL_INDICATION, null);

	int callId = Integer.parseInt(incomingCallInfo[0]);
        int callMode = Integer.parseInt(incomingCallInfo[3]);
        int seqNumber = Integer.parseInt(incomingCallInfo[4]);

	// some guess work is needed here, for now, just 0
	callMode = 0;

        rr.mParcel.writeInt(3);

        rr.mParcel.writeInt(callMode);
        rr.mParcel.writeInt(callId);
        rr.mParcel.writeInt(seqNumber);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> "
            + requestToString(rr.mRequest) + " " + callMode + " " + callId + " " + seqNumber);

        send(rr);
    }

    // Override setupDataCall as the MTK RIL needs 8th param CID (hardwired to 1?)
    @Override
    protected Object
    responseSimRefresh(Parcel p) {
        IccRefreshResponse response = new IccRefreshResponse();

        response.refreshResult = p.readInt();
        String rawefId = p.readString();
        response.efId   = rawefId == null ? 0 : Integer.parseInt(rawefId);
        response.aid = p.readString();

        return response;
    }

    @Override
    public void
    setupDataCall(int radioTechnology, int profile, String apn,
            String user, String password, int authType, String protocol,
            Message result) {

        RILRequest rr = RILRequest.obtain(RIL_REQUEST_SETUP_DATA_CALL, result);

        rr.mParcel.writeInt(8);

        rr.mParcel.writeString(Integer.toString(radioTechnology + 2));
        rr.mParcel.writeString(Integer.toString(profile));
        rr.mParcel.writeString(apn);
        rr.mParcel.writeString(user);
        rr.mParcel.writeString(password);
        rr.mParcel.writeString(Integer.toString(authType));
        rr.mParcel.writeString(protocol);
        rr.mParcel.writeString("1");

        if (RILJ_LOGD) riljLog(rr.serialString() + "> "
                + requestToString(rr.mRequest) + " " + radioTechnology + " "
                + profile + " " + apn + " " + user + " "
                + password + " " + authType + " " + protocol + "1");

        send(rr);
    }

    private void setRadioStateFromRILInt (int stateCode) {
        switch (stateCode) {
	case 0: case 1: break; // radio off
	default:
	    {
	        RILRequest rr = RILRequest.obtain(RIL_REQUEST_SET_GPRS_TRANSFER_TYPE, null);

		if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

		rr.mParcel.writeInt(1);
		rr.mParcel.writeInt(1);

		send(rr);
	    }
	    {
	        RILRequest rr = RILRequest.obtain(RIL_REQUEST_SET_GPRS_CONNECT_TYPE, null);

		if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

		rr.mParcel.writeInt(1);
		rr.mParcel.writeInt(1);

		send(rr);
	    }
	}
    }

    @Override
    public void getRadioCapability(Message response) {
        riljLog("getRadioCapability: returning static radio capability");
        if (response != null) {
            Object ret = makeStaticRadioCapability();
            AsyncResult.forMessage(response, ret, null);
            response.sendToTarget();
        }
    }

    @Override
    public void setPreferredNetworkType(int networkType , Message response) {
        riljLog("setPreferredNetworkType: " + networkType);

        if (!setPreferredNetworkTypeSeen) {
            setPreferredNetworkTypeSeen = true;
        }

        super.setPreferredNetworkType(networkType, response);
    }
	
	
	    @Override
    public void
    iccIOForApp (int command, int fileid, String path, int p1, int p2, int p3,
            String data, String pin2, String aid, Message result) {
        if (command == 0xc0 && p3 == 0) {
            Rlog.i("MediaTekRIL", "Override the size for the COMMAND_GET_RESPONSE 0 => 15");
            p3 = 15;
        }
        super.iccIOForApp(command, fileid, path, p1, p2, p3, data, pin2, aid, result);
    }

}
