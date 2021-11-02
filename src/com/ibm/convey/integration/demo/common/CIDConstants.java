package com.ibm.convey.integration.demo.common;

import com.yantra.shared.ydm.YDMStatusDefinitions;

import java.util.*;

public interface CIDConstants {

    public static final String SHIPMENT_EVENT = "ShipmentEvent";
    public static final String CONVEY_SHIPMENT_ID = "shipment_id";
    public static final String CONVEY_TRACKING_NUMBER = "tracking_number";
    public static final String CONVEY_SHIPMENT_INFO_ELEM = "shipment_info";
    public static final String CONVEY_EVENT_DETAILS_ELEM = "event_details";
    public static final String CONVEY_SHIPMENT_INFO_EDD = "estimated_delivery_date";
    public static final String CONVEY_SHIPMENT_INFO_ORIG_EDD = "original_estimated_delivery_date";
    public static final String CONVEY_SHIPMENT_INFO_STATUS = "status";


    public static final String CONVEY_EVENT_TYPE = "event_type";
    public static final String CONVEY_EVENT_TYPE_TRACKING = "tracking";
    public static final String CONVEY_EVENT_TYPE_EXCEPTION = "exception";
    public static final String CONVEY_EVENT_STATUS_NEW = "new";
    public static final String CONVEY_EVENT_STATUS_QUOTED = "quoted";
    public static final String CONVEY_EVENT_STATUS_SCHED = "scheduled";
    public static final String CONVEY_EVENT_STATUS_PICKUP_APPTMT = "pickup_appointment";
    public static final String CONVEY_EVENT_STATUS_TRANSIT = "in_transit";
    public static final String CONVEY_EVENT_STATUS_OUT_FOR_DEL = "out_for_delivery";
    public static final String CONVEY_EVENT_STATUS_RETURNING_SENDER = "returning_to_sender";
    public static final String CONVEY_EVENT_STATUS_DEL_SENDER = "delivered_to_sender";
    public static final String CONVEY_EVENT_STATUS_UNDEL = "undeliverable";
    public static final String CONVEY_EVENT_STATUS_UNKNOWN = "unknown";
    public static final String CONVEY_EVENT_STATUS_CANCELLED = "canceled";
    public static final String CONVEY_EVENT_STATUS_DEL = "delivered";

    public static final String OMS_EVENT_STATUS_NEW = "1100.70.06.70.1";
    public static final String OMS_EVENT_STATUS_QUOTED = "1100.70.06.70.2";
    public static final String OMS_EVENT_STATUS_SCHED = "1300.ex.10";
    public static final String OMS_EVENT_STATUS_PICKUP_APPTMT = "1300.ex.20";
    public static final String OMS_EVENT_STATUS_TRANSIT = "1400.ex.10";
    public static final String OMS_EVENT_STATUS_OUT_FOR_DEL = "1400.ex.20";
    public static final String OMS_EVENT_STATUS_RETURNING_SENDER = "1400.ex.30";
    public static final String OMS_EVENT_STATUS_DEL_TO_SENDER = "1400.ex.40";
    public static final String OMS_EVENT_STATUS_UNDEL = "1400.ex.80";
    public static final String OMS_EVENT_STATUS_DEL_UNKNOWN = "1400.ex.85";
    public static final String OMS_EVENT_STATUS_DEL_CANCELLED = "1400.ex.99";

    public static final String OMS_TRAN_ID_TRANSIT = "SHIPMENT_IN_TRANSIT.0001.ex";
    public static final String OMS_TRAN_ID_FOR_DEL= "SHIPMENT_FOR_DELIVERY.0001.ex";
    public static final String OMS_TRAN_ID_RETURNING_SENDER = "SHIP_RETURNING_TO_SENDER.0001.ex";
    public static final String OMS_TRAN_ID_DEL_TO_SENDER = "SHIP_DELIVERED_TO_SENDER.0001.ex";
    public static final String OMS_TRAN_ID_UNDEL = "SHIPMENT_UNDELIVERABLE.0001.ex";
    public static final String OMS_TRAN_ID_DEL_UNKNOWN = "SHIP_DELIVERY_UNKNOWN.0001.ex";
    public static final String OMS_TRAN_ID_DEL_CANCELLED = "SHIP_DELIVERY_CANCELLED.0001.ex";
    public static final String OMS_TRAN_ID_DELIVER = "DELIVER_SHIPMENT";

    //properties
    public static final String OMS_CONVEY_INTEGRATION_PUBLISH_URL = "yfs.convey.integration.publish.url";
    public static final String OMS_CONVEY_INTEGRATION_PUBLISH_AUTH_KEY = "yfs.convey.integration.publish.auth.key";
    public static final String OMS_CONVEY_INTEGRATION_CARRIER_PVFT_TN_LAST_USED = "yfs.convey.integration.pvft.tracking.number.last.used";

    public static final List<String> statusNotForMoveList = Collections.unmodifiableList(new ArrayList<String>() {{
        add(OMS_EVENT_STATUS_NEW);
        add(OMS_EVENT_STATUS_QUOTED);
        add(OMS_EVENT_STATUS_SCHED);
        add(OMS_EVENT_STATUS_PICKUP_APPTMT);
    }});

    public static final Map<String, String> conveyToOMSStatusMap = Collections.unmodifiableMap(new HashMap<String, String>() {{
        put(CONVEY_EVENT_STATUS_NEW, OMS_EVENT_STATUS_NEW);
        put(CONVEY_EVENT_STATUS_QUOTED, OMS_EVENT_STATUS_QUOTED);
        put(CONVEY_EVENT_STATUS_SCHED, OMS_EVENT_STATUS_SCHED);
        put(CONVEY_EVENT_STATUS_PICKUP_APPTMT, OMS_EVENT_STATUS_PICKUP_APPTMT);
        put(CONVEY_EVENT_STATUS_TRANSIT, OMS_EVENT_STATUS_TRANSIT);
        put(CONVEY_EVENT_STATUS_OUT_FOR_DEL, OMS_EVENT_STATUS_OUT_FOR_DEL);
        put(CONVEY_EVENT_STATUS_RETURNING_SENDER, OMS_EVENT_STATUS_RETURNING_SENDER);
        put(CONVEY_EVENT_STATUS_DEL_SENDER, OMS_EVENT_STATUS_DEL_TO_SENDER);
        put(CONVEY_EVENT_STATUS_UNDEL, OMS_EVENT_STATUS_UNDEL);
        put(CONVEY_EVENT_STATUS_UNKNOWN, OMS_EVENT_STATUS_DEL_UNKNOWN);
        put(CONVEY_EVENT_STATUS_CANCELLED, OMS_EVENT_STATUS_DEL_CANCELLED);
        put(CONVEY_EVENT_STATUS_DEL, YDMStatusDefinitions.YDM_SHIPMENT_STATUS_DELIVERED);
    }});

    public static final Map<String, String> dropStatusToTranIDMap = Collections.unmodifiableMap(new HashMap<String, String>() {{
        put(OMS_EVENT_STATUS_TRANSIT, OMS_TRAN_ID_TRANSIT);
        put(OMS_EVENT_STATUS_OUT_FOR_DEL, OMS_TRAN_ID_FOR_DEL);
        put(OMS_EVENT_STATUS_RETURNING_SENDER, OMS_TRAN_ID_RETURNING_SENDER);
        put(OMS_EVENT_STATUS_DEL_TO_SENDER, OMS_TRAN_ID_DEL_TO_SENDER);
        put(OMS_EVENT_STATUS_UNDEL, OMS_TRAN_ID_UNDEL);
        put(OMS_EVENT_STATUS_DEL_UNKNOWN, OMS_TRAN_ID_DEL_UNKNOWN);
        put(OMS_EVENT_STATUS_DEL_CANCELLED, OMS_TRAN_ID_DEL_CANCELLED);
        put(YDMStatusDefinitions.YDM_SHIPMENT_STATUS_DELIVERED, OMS_TRAN_ID_DELIVER);
    }});

}
