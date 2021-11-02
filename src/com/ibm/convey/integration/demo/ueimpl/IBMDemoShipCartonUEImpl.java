package com.ibm.convey.integration.demo.ueimpl;

import com.ibm.convey.integration.demo.common.CIDConstants;
import com.yantra.shared.dbi.YFS_Property;
import com.yantra.shared.dbi.YFS_Shipment;
import com.yantra.shared.dbi.YFS_Shipment_Container;
import com.yantra.shared.ycp.YFSContext;
import com.yantra.shared.ydm.YDMFactory;
import com.yantra.shared.ydm.YDMTransactionCache;
import com.yantra.ycp.api.YCPEntityApiImpl;
import com.yantra.ycs.japi.ue.YCSshipCartonUserExit;
import com.yantra.yfc.dom.YFCDocument;
import com.yantra.yfc.dom.YFCElement;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfc.util.YFCConfigurator;
import com.yantra.yfc.util.YFCException;
import com.yantra.yfs.japi.YFSUserExitException;

import java.util.List;
import java.util.UUID;

public class IBMDemoShipCartonUEImpl implements YCSshipCartonUserExit{

    private static YFCLogCategory logger = YFCLogCategory.instance(IBMDemoShipCartonUEImpl.class.getName());

    @Override
    public String shipCarton(YFSContext arg0, String arg1) throws YFSUserExitException {
        logger.verbose("YCSshipCartonUserExit shipCarton: arg1: "+arg1);
        return arg1;
    }

    @Override
    public boolean shipCartonContinue(YFSContext arg0, String arg1) throws YFSUserExitException {
        logger.verbose(" YCSshipCartonUserExit shipCartonContinue: returning false ");
        return false;
    }

    @Override
    public String shipCartonOutXML(YFSContext arg0, String arg1) throws YFSUserExitException {
        String shippingLabelPath = YFCConfigurator.getInstance().getProperty("sample.pierbridge.shippinglabel.url");
        String returnLabelPath = YFCConfigurator.getInstance().getProperty("sample.pierbridge.returnlabel.url");
        YFCElement input = YFCDocument.getDocumentFor(arg1).getDocumentElement();
        if(logger.isVerboseEnabled()) {
            logger.verbose(" YCSshipCartonUserExit shipCartonOutXML: input: "+input.toString());
        }

        YFCElement eleSourcePackageLvlDetail = input.getChildElement("PackageLevelDetail");
        YFCElement output = YFCDocument.createDocument("ShipCarton").getDocumentElement();
        String trackingNumber = getTrackingNumber(arg0, input);
        output.setAttribute("TrackingNumber", trackingNumber);
        output.setAttribute("PierbridgeLabelURL", shippingLabelPath);
        YFCElement eleReturnTrackingDetails = output.createChild("ReturnTrackingDetails");
        YFCElement eleReturnTrack = eleReturnTrackingDetails.createChild("ReturnTrackingDetail");
        eleReturnTrack.setAttribute("ReturnTrackingNumber", "012203570389847");
        eleReturnTrack.setAttribute("PierbridgeReturnLabelURL", returnLabelPath);
        eleReturnTrackingDetails.setAttribute("NumberOfReturnTrackingNumbers", "1");
        YFCElement elePackageLvlDetail = output.createChild("PackageLevelDetail");
        elePackageLvlDetail.setAttributes(eleSourcePackageLvlDetail.getAttributes());
        if(logger.isVerboseEnabled()) {
            logger.verbose(" YCSshipCartonUserExit shipCartonOutXML: output: "+output.toString());
        }
        return output.toString();
    }

    private String getTrackingNumber(YFSContext ctx, YFCElement input) {
        try {
            String trackingNo = shouldUpdateTN(ctx, input);
            if (YFCCommon.isVoid(trackingNo)) {
                logger.info("Generating TrackingNo ...");
                if (YFCCommon.equals(input.getAttribute("Carrier"), "PVFT")) {
                    String svalue = YFCConfigurator.getInstance().getProperty(CIDConstants.OMS_CONVEY_INTEGRATION_CARRIER_PVFT_TN_LAST_USED);
                    if (YFCCommon.isVoid(svalue)) {
                        svalue = "PT0000110016";
                    }
                    logger.info("Last Used TrackingNo is " + svalue);
                    int nextSeqNo = Integer.parseInt(svalue.substring(6)) + 1;//110001 is starting
                    trackingNo = svalue.substring(0, 6) + nextSeqNo;
                    modifyProperty(ctx, trackingNo);
                }
                if (YFCCommon.isVoid(trackingNo)) {
                    UUID trackingNumber = UUID.randomUUID();
                    String trackingNoString = String.valueOf(trackingNumber);
                    trackingNo = trackingNoString.substring(0, 10);
                }
                logger.info("TrackingNo generated : " + trackingNo);
            }
            return trackingNo;
        }
        catch (NumberFormatException ex) {
            throw new YFCException(ex);
        }
    }

    private void modifyProperty(YFSContext ctx, String trackingNo) {
        logger.info("Updating property CIDConstants.OMS_CONVEY_INTEGRATION_CARRIER_PVFT_TN_LAST_USED value to " + trackingNo);
        YCPEntityApiImpl api = new YCPEntityApiImpl();
        YFCElement inElem = YFCDocument.createDocument(YFS_Property.PROPERTY_XMLNAME).getDocumentElement();
        inElem.setAttribute(YFS_Property.BASE_PROPERTY_NAME, CIDConstants.OMS_CONVEY_INTEGRATION_CARRIER_PVFT_TN_LAST_USED);
        inElem.setAttribute(YFS_Property.CATEGORY, "yfs");
        inElem.setAttribute(YFS_Property.PROPERTY_VALUE, trackingNo);
        inElem.setAttribute(YFS_Property.PROPERTY_OVERRIDE, "BASE");
        api.manageProperty(ctx, inElem.getOwnerDocument().getDocument());
    }

    private String shouldUpdateTN(YFSContext ctx, YFCElement input) {
        YDMTransactionCache oTranCache = YDMFactory.getCache(ctx);
        YFS_Shipment dbObjShip = oTranCache.getShipment(ctx, input.getChildElement("PackageLevelDetail", true).getAttribute("ShipmentKey"));
        if (dbObjShip != null) {
            String refNotes = input.getChildElement("ExtraFieldsRecord", true).getAttribute("ReferenceNotes");
            String containerscm = refNotes.substring(10);
            List<YFS_Shipment_Container> list = dbObjShip.getContainerList();
            for (YFS_Shipment_Container dbObjCont : list) {
                if (YFCCommon.equals(dbObjCont.getContainer_Scm(), containerscm)) {
                    if (!YFCCommon.isVoid(dbObjCont.getTracking_No())) {
                        logger.debug("TrackingNo is already stamped as : " + dbObjCont.getTracking_No());
                        return dbObjCont.getTracking_No();
                    }
                    break;
                }
            }
        }
        logger.debug("TrackingNo must be generated");
        return null;
    }

}
