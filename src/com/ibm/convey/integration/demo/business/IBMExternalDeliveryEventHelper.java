package com.ibm.convey.integration.demo.business;

import com.ibm.convey.integration.demo.common.CIDConstants;
import com.ibm.icu.util.TimeZone;
import com.yantra.shared.dbclasses.YFS_TransactionDBHome;
import com.yantra.shared.dbi.*;
import com.yantra.shared.plt.PLTErrorCodes;
import com.yantra.shared.plt.PLTSharedAPILiterals;
import com.yantra.shared.ycp.YCPFactory;
import com.yantra.shared.ycp.YCPTransactionCache;
import com.yantra.shared.ycp.YFSContext;
import com.yantra.shared.ycp.YSharedErrors;
import com.yantra.shared.ydm.YDMFactory;
import com.yantra.shared.ydm.YDMStatusDefinitions;
import com.yantra.shared.ydm.YDMTransactionCache;
import com.yantra.util.YFCUtils;
import com.yantra.ycp.api.YCPMultiApiImpl;
import com.yantra.yfc.date.YDate;
import com.yantra.yfc.date.YTime;
import com.yantra.yfc.date.YTimestamp;
import com.yantra.yfc.dom.YFCDocument;
import com.yantra.yfc.dom.YFCElement;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfc.util.YFCConfigurator;
import com.yantra.yfc.util.YFCException;
import com.yantra.yfc.util.YFCLocale;
import org.w3c.dom.Document;

import java.util.List;

import static com.yantra.shared.plt.PLTConstants.YCP_YES;

public class IBMExternalDeliveryEventHelper {
    private static YFCLogCategory logger = YFCLogCategory.instance(IBMExternalDeliveryEventHelper.class);

    private YTimestamp currentRDD = null;

    public void validate() {

    }

    // prepare container status/date change input doc
    // evaluate shipment status change
    // prepare shipment change/status change
    // call multiapi

    public void executeEvent(YFSContext ctx, YFCElement eventElem) throws Exception {
        logger.beginTimer("executeEvent");
        try	{
            logger.verbose("IBMExternalDeliveryEventHelper.executeEvent Input : " + eventElem);
            YFCDocument inDoc = YFCDocument.createDocument("MultiApi");
            prepareShipmentContainerChangeInput(ctx, inDoc, eventElem);
            String newStatus = shouldShipmentStatusChange(ctx, eventElem);
            if (newStatus != null) {
                prepareShipmentChangeInput(ctx, inDoc, eventElem, newStatus);
            }
            callMultiAPI(ctx, inDoc);
        }
        finally{
            logger.endTimer("executeEvent");
        }
    }

    private void prepareShipmentContainerChangeInput(YFSContext ctx, YFCDocument inDoc, YFCElement eventElem) {
        YFCElement apiElem = inDoc.getDocumentElement().createChild("API");
        apiElem.setAttribute("Name", "changeShipment");
        YFCElement rootInputElem = apiElem.createChild("Input");
        YFCElement inputElem = rootInputElem.createChild("Shipment");
        inputElem.setAttribute("ShipmentKey", eventElem.getAttribute("ShipmentKey"));
        YFCElement containerRootElem = inputElem.createChild("Containers");
        YFCElement containerElem = containerRootElem.createChild("Container");
        String eventStatus = eventElem.getChildElement(CIDConstants.CONVEY_SHIPMENT_INFO_ELEM, true).getAttribute(CIDConstants.CONVEY_SHIPMENT_INFO_STATUS);
        String status = CIDConstants.conveyToOMSStatusMap.get(eventStatus);
        if (status == null) {
            YFCException ex = new YFCException(YSharedErrors.YFS_INVALID_INPUT_CHANGE_STATUS);
            ex.setAttribute(CIDConstants.CONVEY_SHIPMENT_INFO_ELEM + "." + CIDConstants.CONVEY_SHIPMENT_INFO_STATUS, eventStatus);
            ex.setAttribute("Mapped OMS Status", status);
            throw ex;
        }
        containerElem.setAttribute("Status", status);
        YCPTransactionCache oCache = YCPFactory.getCache(ctx);
        YFS_Status oStatus = oCache.getStatus(ctx, "ORDER_DELIVERY", status);
        containerElem.setAttribute(YFS_Shipment_Container.EXTERNAL_REFERENCE_1, oStatus!=null? oStatus.getStatus_Name() : status);
        containerElem.setAttribute("StatusDate", eventElem.getChildElement(CIDConstants.CONVEY_SHIPMENT_INFO_ELEM, true).getAttribute(CIDConstants.CONVEY_SHIPMENT_INFO_EDD));
        containerElem.setAttribute("ShipmentContainerKey", eventElem.getAttribute("ShipmentContainerKey"));
    }

    private void prepareShipmentChangeInput(YFSContext ctx, YFCDocument inDoc, YFCElement eventElem, String newStatus) {

        String tranId = CIDConstants.dropStatusToTranIDMap.get(newStatus);
        YFS_Transaction oTran = YFS_TransactionDBHome.getInstance().selectWithTranID(ctx, tranId);
        if ( oTran == null ) {
            YFCException ex = new YFCException(PLTErrorCodes.YCP_INVALID_TRANSACTION_ID);
            ex.setAttribute(PLTSharedAPILiterals.YFS_TRANSACTION_ID, tranId);
            throw ex;
        }
        // update shipment for RDD/ADD
        if (currentRDD != null) {
            YFCElement shipmentElem = inDoc.getDocumentElement().getChildElement("API").getChildElement("Input").getChildElement("Shipment");
            if (YFCCommon.equals(YDMStatusDefinitions.YDM_SHIPMENT_STATUS_DELIVERED, newStatus)) {
                shipmentElem.setAttribute(YFS_Shipment.ACTUAL_DELIVERY_DATE, currentRDD);
            } else {
                shipmentElem.setAttribute(YFS_Shipment.REQUESTED_DELIVERY_DATE, currentRDD);
            }
        }

        // make shipmentStatusChange Input
        YFCElement apiElem = inDoc.getDocumentElement().createChild("API");
        apiElem.setAttribute("Name", "changeShipmentStatus");
        YFCElement rootInputElem = apiElem.createChild("Input");
        YFCElement inputElem = rootInputElem.createChild("Shipment");
        inputElem.setAttribute("ShipmentKey", eventElem.getAttribute("ShipmentKey"));
        inputElem.setAttribute("TransactionId", tranId);
        inputElem.setAttribute("AcceptOutOfSequenceUpdates", YCP_YES);
    }

    private String shouldShipmentStatusChange(YFSContext ctx, YFCElement eventElem) {
        logger.beginTimer("shouldShipmentStatusChange");
        try	{
            String newStatus = null;
            String currentEventStatus = eventElem.getChildElement(CIDConstants.CONVEY_SHIPMENT_INFO_ELEM, true).getAttribute(CIDConstants.CONVEY_SHIPMENT_INFO_STATUS);
            String currentOMSStatus = CIDConstants.conveyToOMSStatusMap.get(currentEventStatus);
            logger.debug("Event status is " + currentEventStatus);
            logger.debug("Corresponding OMS status is " + currentOMSStatus);
            if (CIDConstants.statusNotForMoveList.contains(currentOMSStatus)) {
                return newStatus;
            }
            YDMTransactionCache oTranCache = YDMFactory.getCache(ctx);
            YFS_Shipment dbObjShip = oTranCache.getShipment(ctx, eventElem.getAttribute("ShipmentKey"));
            if (dbObjShip != null) {
                List<YFS_Shipment_Container> list = dbObjShip.getContainerList();
                String minStatus = null; YTimestamp maxDate = null;
                for (YFS_Shipment_Container dbObjCont : list) {
                    String statusToCompare = dbObjCont.getStatus();
                    logger.debug("Status is " + statusToCompare + " for ContainerKey " + dbObjCont.getShipment_Container_Key());
                    YTimestamp dateToCompare = dbObjCont.getStatus_Date();
                    YTimestamp currentDate = eventElem.getChildElement(CIDConstants.CONVEY_SHIPMENT_INFO_ELEM, true).getYTimestampAttribute(CIDConstants.CONVEY_SHIPMENT_INFO_EDD);
                    //ActualDeliveryDate="<event_details.local_date+time+offset>" />

                    if (YFCCommon.equals(YDMStatusDefinitions.YDM_SHIPMENT_STATUS_DELIVERED, currentOMSStatus)) {
                        YFCElement eventDtlElem = eventElem.getChildElement(CIDConstants.CONVEY_EVENT_DETAILS_ELEM, true);
                        YDate date = eventDtlElem.getYDateAttribute("local_date");
                        YTime time = eventDtlElem.getTimeAttribute("local_time");
                        String utcOffset = eventDtlElem.getAttribute("utc_offset");

                        YTimestamp actualDelDate = YTimestamp.newMutableTimestamp(date, time);
                        String localtz = getTimeZone(ctx, dbObjShip.getEnterprise_Code());
                        actualDelDate.applyTimeZone(TimeZone.getTimeZone(utcOffset), TimeZone.getTimeZone(localtz));
                        currentDate = actualDelDate;
                    }
                    if (maxDate  == null)
                        maxDate = currentDate;
                    if (YFCCommon.equals(eventElem.getAttribute("ShipmentContainerKey"), dbObjCont.getShipment_Container_Key())) {
                        logger.debug("Convey Event ContainerKey " + dbObjCont.getShipment_Container_Key());

                        if (YFCUtils.compareStrings(statusToCompare, currentOMSStatus) < 0) {
                            statusToCompare = currentOMSStatus;
                        }
                        if(dateToCompare == null || dateToCompare.lt(currentDate,false)) {
                            dateToCompare = currentDate;
                        }
                    }
                    if (YFCUtils.compareStrings(minStatus, statusToCompare) < 0) {
                        minStatus = statusToCompare;
                    }
                    if(dateToCompare != null && dateToCompare.gt(maxDate,false)) {
                        maxDate = dateToCompare;
                    }
                }
                if (YFCUtils.compareStrings(dbObjShip.getStatus(), minStatus) < 0) {
                    newStatus = minStatus;
                    currentRDD = maxDate;
                }
                logger.debug("Min Status across all containers is " +  minStatus);
            }
            logger.debug("Shipment Current Status is " + dbObjShip.getStatus());
            logger.debug("If possible, move Shipment Status to " + newStatus);
            return newStatus;
        }
        finally{
            logger.endTimer("shouldShipmentStatusChange");
        }
    }
    private void callMultiAPI(YFSContext ctx, YFCDocument inputDoc) throws Exception {
        logger.beginTimer("callMultiAPI");
        try	{
            logger.verbose("IBMExternalDeliveryEventHelper.callMultiAPI Input : " + inputDoc.toString());
            YCPMultiApiImpl apiImpl = new YCPMultiApiImpl();
            apiImpl.multiApi(ctx, inputDoc.getOwnerDocument().getDocument());
        }
        finally{
            logger.endTimer("callMultiAPI");
        }
    }

    private String getTimeZone(YFSContext ctx, String entCode) {

        String localtz = null;
        if (ctx.getYFCLocale() != null) {
            localtz = ctx.getYFCLocale().getTimezone();
        }
        if (localtz == null) {
            YFS_Organization org = YCPFactory.getCache(ctx).getOrganization(ctx,entCode);
            localtz = YFCLocale.getYFCLocale(org.getLocale_Code()).getTimezone();
        }
        return localtz;
    }

    /*
<PublishShipmentAPI URL="" HTTPMethod="">
  <RequestHeaders>
    <Header Name="Content-Type" Value="application/json" />
    <Header Name="Authorization" Value="Basic XXX" />
  </RequestHeaders>

  <Input/>
</PublishShipmentAPI>
     */
    public YFCDocument prepareInputDocumentForRemoteCall(Document document) {
        YFCDocument outDoc = YFCDocument.getDocumentFor(document);
        logger.verbose("XSL document : " + outDoc.toString());
        YFCElement inElem = outDoc.getDocumentElement();
        String  url = YFCConfigurator.getInstance().getProperty(CIDConstants.OMS_CONVEY_INTEGRATION_PUBLISH_URL);
        String  authKey = YFCConfigurator.getInstance().getProperty(CIDConstants.OMS_CONVEY_INTEGRATION_PUBLISH_AUTH_KEY);
        inElem.setAttribute("URL", url);
        YFCElement reqHdrs = inElem.getChildElement("RequestHeaders", true);
        YFCElement hdr = reqHdrs.createChild("Header");
        hdr.setAttribute("Name", "Content-Type");
        hdr.setAttribute("Value", "application/json");
        hdr = reqHdrs.createChild("Header");
        hdr.setAttribute("Name", "Authorization");
        hdr.setAttribute("Value", authKey);

        return outDoc;
    }

}
