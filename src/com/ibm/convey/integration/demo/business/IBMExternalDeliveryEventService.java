package com.ibm.convey.integration.demo.business;

import com.ibm.convey.integration.demo.common.CIDConstants;
import com.ibm.sterling.afc.jsonutil.PLTJSONUtils;
import com.ibm.sterling.afc.services.utils.RemoteAPI;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.interop.japi.YIFCustomApi;
import com.yantra.shared.dbi.YFS_Shipment;
import com.yantra.shared.dbi.YFS_Shipment_Container;
import com.yantra.shared.plt.PLTErrorCodes;
import com.yantra.shared.plt.PLTLiterals;
import com.yantra.shared.ycp.YFSContext;
import com.yantra.shared.ydm.YDMErrorCodes;
import com.yantra.ycp.core.YCPEntityApi;
import com.yantra.ydm.common.YDMAPILiterals;
import com.yantra.yfc.core.YFCObject;
import com.yantra.yfc.dom.YFCDocument;
import com.yantra.yfc.dom.YFCElement;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfc.util.YFCConfigurator;
import com.yantra.yfc.util.YFCException;
import com.yantra.yfs.japi.YFSEnvironment;
import org.apache.commons.json.JSONException;
import org.w3c.dom.Document;

import java.util.Properties;

public class IBMExternalDeliveryEventService extends YFCObject implements YIFCustomApi {
    private static YFCLogCategory logger = YFCLogCategory.instance(IBMExternalDeliveryEventService.class);

    private Properties properties = null;

    @Override
    public void setProperties(Properties prop) throws Exception {
        properties = prop;
    }

    private YFSContext  getContext(YFSEnvironment env) {
        if ( env instanceof YFSContext )
            return (YFSContext)env;
        else
            return null;
    }

    public Document acceptEvent(YFSEnvironment oEnv, Document document) throws Exception {
        logger.beginTimer("acceptEvent");
        try	{
            logger.verbose("acceptEvent Input : " + document.toString());
            YFSContext ctx = getContext(oEnv);
            YFCElement eventElem = YFCDocument.getDocumentFor(document).getDocumentElement();
            if (!equals(CIDConstants.SHIPMENT_EVENT, eventElem.getNodeName())) {
                throw new YFCException(PLTErrorCodes.YCP_INVALID_INPUT_DOCUMENT);
            }
            String strTrackingNumber = eventElem.getAttribute(CIDConstants.CONVEY_TRACKING_NUMBER);
            if (!YFCCommon.isVoid(strTrackingNumber)) {
                YFCDocument inDoc = YFCDocument.createDocument(YFS_Shipment_Container.SHIPMENT_CONTAINER_XMLNAME);
                inDoc.getDocumentElement().setAttribute(YFS_Shipment_Container.TRACKING_NO, strTrackingNumber);
                YFCDocument templateDoc = getContainerListTemplate();
                //Document outDoc =  new YDMApiImpl().getShipmentContainerList(oEnv, inDoc.getDocument());
                //YDMTransactionCache oTranCache = YDMFactory.getCache(ctx);
                //YFS_Shipment_Container dbObj = oTranCache.getShipmentContainer(ctx, shipmentContainerKey, null, null, null);
                YFCDocument outDoc = YCPEntityApi.getInstance().invoke(ctx, YDMAPILiterals.YCP_ENTITY_API_LIST + YDMAPILiterals.YDM_SHIPMENT_CONTAINER_XML_ENTITY_NAME, inDoc,templateDoc.getDocumentElement());
                if (outDoc == null) {
                    YFCException ex = new YFCException(PLTErrorCodes.YCP_RECORD_NOT_FOUND);
                    ex.setAttribute(YFS_Shipment_Container.TRACKING_NO, strTrackingNumber);
                    ex.setAttribute(CIDConstants.CONVEY_TRACKING_NUMBER, strTrackingNumber);
                    throw ex;
                }
                logger.verbose("ShipmentContainer List Output : " + outDoc.toString());
                YFCElement outElem = outDoc.getDocumentElement();
                if (!YFCCommon.equals(1, outElem.getIntAttribute(PLTLiterals.YCP_TOTAL_NUMBER_OF_RECORDS))) {
                    YFCException ex = new YFCException(YDMErrorCodes.YDM_CONTAINER_UNIQUE_KEY_MUST_BE_PASSED);
                    ex.setAttribute("MoreInfo", CIDConstants.CONVEY_TRACKING_NUMBER + " must exist and be Unique in OMS for a ShipmentContainer element");
                    ex.setAttribute(YFS_Shipment_Container.TRACKING_NO, strTrackingNumber);
                    ex.setAttribute(CIDConstants.CONVEY_TRACKING_NUMBER, strTrackingNumber);
                    throw ex;
                }
                YFCElement containerElem = outElem.getChildElement(YFS_Shipment_Container.SHIPMENT_CONTAINER_XMLNAME);
                eventElem.setAttribute(YFS_Shipment.SHIPMENT_KEY, containerElem.getAttribute(YFS_Shipment_Container.SHIPMENT_KEY));
                eventElem.setAttribute(YFS_Shipment_Container.SHIPMENT_CONTAINER_KEY, containerElem.getAttribute(YFS_Shipment_Container.SHIPMENT_CONTAINER_KEY));
            }
            else {
                YFCException ex = new YFCException(PLTErrorCodes.YCP_FIELD_MANDATORY);
                ex.setAttribute(YFS_Shipment_Container.TRACKING_NO, strTrackingNumber);
                ex.setAttribute(CIDConstants.CONVEY_TRACKING_NUMBER, strTrackingNumber);
                throw ex;
            }
            return document;
        }
        finally{
            logger.endTimer("acceptEvent");
        }

    }

    public void processEvent(YFSEnvironment oEnv, Document document) throws Exception {
        logger.beginTimer("processEvent");
        try {
            YFSContext ctx = getContext(oEnv);
            YFCElement elem = YFCDocument.getDocumentFor(document).getDocumentElement();
            if (YFCCommon.equals(elem.getAttribute(CIDConstants.CONVEY_EVENT_TYPE), CIDConstants.CONVEY_EVENT_TYPE_TRACKING)) {
                new IBMExternalDeliveryEventHelper().executeEvent(ctx, elem);
            }
            else if (YFCCommon.equals(elem.getAttribute(CIDConstants.CONVEY_EVENT_TYPE), CIDConstants.CONVEY_EVENT_TYPE_EXCEPTION)) {
                //This is container delivery exception events from Convey, such as 'damaged_or_shorted' or 'convey_revised_edd_later', no status change to Container
                // Maybe save as couple of extended columns in OMS YFS_Shipment_Container Entity
                // Maybe save it as Inbox Alert for now
                //YCPInboxManager.createException(ctx, inputDoc);
            }
        }
        finally {
            logger.endTimer("processEvent");
        }

    }

    public Document publishData(YFSEnvironment oEnv, Document document) throws Exception {
        logger.beginTimer("publishData");
        Document outDoc = null;
        try {
            YFCConfigurator.getInstance().setProperty("xslcomponent.useTemplateLoading", "true");
            /* This was used when SDF XSL component could not load the xsl
            XslFlowComponent cmp = new XslFlowComponent();
            cmp.setXslFileName("template/service/xsl/TransformOrderXmlToExternalOrderJson.xsl");
            URL url = YCPTemplateManager.getInstance().getGenericTemplateURL((YFSContext)oEnv, "/", "template/service/xsl/TransformOrderXmlToExternalOrderJson.xsl", "", new HashMap());
            if (url == null) {
                YFCException e = new YFCException("cannot find xsl url from template manager for : /template/service/xsl/TransformOrderXmlToExternalOrderJson.xsl");
                throw e;
            }
            logger.verbose("XSL URL : " + url.toString());
           logger.verbose("Loading xsl....");
            SyncServiceMessage inMsg = new SyncServiceMessage();
            inMsg.setXMLDoc(document);
            Document xslDoc = cmp.send(getContext(oEnv), inMsg).getXMLDoc();
*/
            YFCDocument inputDoc = new IBMExternalDeliveryEventHelper().prepareInputDocumentForRemoteCall(document);
            outDoc = new RemoteAPI().invoke(oEnv, inputDoc.getDocument());
            logger.verbose("Output from Remote APi call : " + outDoc != null ? outDoc.getDocumentElement().toString() : "");
            //processResponse(outDoc);
            return outDoc;
        }
        catch (Exception ex) {
            throw ex;
        }
        finally {
            logger.endTimer("publishData");
        }
    }

    private void processResponse(Document outDoc) {
        YFCDocument resp = YFCDocument.getDocumentFor(outDoc);
        if (YFCCommon.equals(resp.getDocumentElement().getNodeName(), "PublishShipmentAPI")) {
            logger.info("Response is PublishShipmentAPI");
            YFCElement respOutput = resp.getDocumentElement().getChildElement("Output");
            int statusCode = respOutput.getIntAttribute("Status");
            if (statusCode >= 200 && statusCode < 300) {
                String respValue = respOutput.getNodeValue();
                //<Output Status=“200”>{“success”:true,“result”:{“id”:“8e344ebf-5d5f-3252-a5cc-92e1ad865ece”}}</Output>
                logger.info("remoteapi response output value: " + respValue);
                Document output = null;
                try {
                    output = PLTJSONUtils.getDocFromJSON(respValue, "Output");
                    String id = SCXmlUtil.getChildElement(output.getDocumentElement(), "result").getAttribute("id");
                    logger.info("PublishShipmentAPI.Output.result.id : " + id );
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

    }

    private YFCDocument getContainerListTemplate() {
        YFCDocument templateDoc = YFCDocument.createDocument("Containers");
        templateDoc.getDocumentElement().setAttribute(PLTLiterals.YCP_TOTAL_NUMBER_OF_RECORDS, "");
        YFCElement templateElem = templateDoc.getDocumentElement().getChildElement("Container", true);
        templateElem.setAttribute(YFS_Shipment_Container.SHIPMENT_CONTAINER_KEY, "");
        templateElem.setAttribute(YFS_Shipment_Container.SHIPMENT_KEY, "");
        templateElem.setAttribute(YFS_Shipment_Container.TRACKING_NO, "");

        return  templateDoc;
    }
}
