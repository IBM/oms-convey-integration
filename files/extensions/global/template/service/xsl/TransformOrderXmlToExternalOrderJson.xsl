<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:template name="remoteapiinput" match="/">
<PublishShipmentAPI URL="" HTTPMethod="POST" >
<Input>
{ 
    "order_number": "<xsl:value-of select="/Shipment/ShipmentLines/ShipmentLine/@OrderNo"/>",
    "client_request_id": "<xsl:value-of select="/Shipment/ShipmentLines/ShipmentLine/@OrderNo"/>",
    "extended_validation": false,
    "order_local_date": "<xsl:value-of select="substring(/Shipment/ShipmentLines/ShipmentLine/OrderRelease/@OrderDate,1,10)"/>",
    "order_local_time": "<xsl:value-of select="substring(/Shipment/ShipmentLines/ShipmentLine/OrderRelease/@OrderDate,12,8)"/>",
    "order_utc_offset": "<xsl:value-of select="substring(/Shipment/ShipmentLines/ShipmentLine/OrderRelease/@OrderDate,20)"/>",
    "customer_billing_info": {
        "customer_id": "<xsl:value-of select="/Shipment/BillToAddress/@PersonID"/>",
        "name": "<xsl:value-of select="/Shipment/BillToAddress/@Company"/>",
        "contact": {
            "person_name": "<xsl:value-of select="/Shipment/BillToAddress/@FirstName"/>
                                <xsl:text> </xsl:text>
                            <xsl:value-of select="/Shipment/BillToAddress/@LastName"/>",
            "email": "<xsl:value-of select="/Shipment/BillToAddress/@EMailID"/>",
            "phone_number": "<xsl:value-of select="/Shipment/BillToAddress/@DayPhone"/>"
        },
        "address": {
            "street_line1": "<xsl:value-of select="/Shipment/BillToAddress/@AddressLine1"/>",
            "street_line2": "<xsl:value-of select="/Shipment/BillToAddress/@AddressLine2"/>",
            "city": "<xsl:value-of select="/Shipment/BillToAddress/@City"/>",
            "postal_code": "<xsl:value-of select="/Shipment/BillToAddress/@ZipCode"/>",
            "state_or_province_code": "<xsl:value-of select="/Shipment/BillToAddress/@State"/>",
            "country_code": "<xsl:value-of select="/Shipment/BillToAddress/@Country"/>"
        }
    },
     "line_items": [
         <xsl:for-each select="/Shipment/Containers">
            <xsl:for-each select="Container">
         {
            "line_item_id": "<xsl:value-of select="@ContainerNo"/>",
            "product_info": {
                "product_id": "<xsl:value-of select="ContainerDetails/ContainerDetail/ShipmentLine/@ItemID"/>",
                "description": "<xsl:value-of select="ContainerDetails/ContainerDetail/ShipmentLine/@ItemDesc"/>"
            },
            "quantity": <xsl:value-of select="ContainerDetails/ContainerDetail/@Quantity"/>,
            "fulfillment_id": "<xsl:value-of select="@ShipmentKey"/>",
            "shipment_id": "<xsl:value-of select="@ShipmentContainerKey"/>"
        }
                <xsl:if test="./following-sibling::Container">,</xsl:if>
            </xsl:for-each>
        </xsl:for-each>

    ],
    "fulfillments": [{
            "fulfillment_id": "<xsl:value-of select="/Shipment/@ShipmentKey"/>",
            "fulfilled_by": "<xsl:value-of select="/Shipment/@ShipNode"/>",
            "origin": {
                "name": "<xsl:value-of select="/Shipment/FromAddress/@Company"/>",
                "contact": {
                    "person_name": "<xsl:value-of select="/Shipment/FromAddress/@FirstName"/>
                                        <xsl:text> </xsl:text>
                                    <xsl:value-of select="/Shipment/FromAddress/@LastName"/>",
                    "email": "<xsl:value-of select="/Shipment/FromAddress/@EMailID"/>",
                    "phone_number": "<xsl:value-of select="/Shipment/FromAddress/@DayPhone"/>"
                },
                "address": {
                    "street_line1": "<xsl:value-of select="/Shipment/FromAddress/@AddressLine1"/>",
                    "street_line2": "<xsl:value-of select="/Shipment/FromAddress/@AddressLine2"/>",
                    "city": "<xsl:value-of select="/Shipment/FromAddress/@City"/>",
                    "postal_code": "<xsl:value-of select="/Shipment/FromAddress/@ZipCode"/>",
                    "state_or_province_code": "<xsl:value-of select="/Shipment/FromAddress/@State"/>",
                    "country_code": "<xsl:value-of select="/Shipment/FromAddress/@Country"/>"
                }
            },
            "destination": {
                "name": "<xsl:value-of select="/Shipment/ToAddress/@Company"/>",
                "contact": {
                    "person_name": "<xsl:value-of select="/Shipment/ToAddress/@FirstName"/>
                                        <xsl:text> </xsl:text>
                                    <xsl:value-of select="/Shipment/ToAddress/@LastName"/>",
                    "email": "<xsl:value-of select="/Shipment/ToAddress/@EMailID"/>",
                    "phone_number": "<xsl:value-of select="/Shipment/ToAddress/@DayPhone"/>"
                },
                "address": {
                    "street_line1": "<xsl:value-of select="/Shipment/ToAddress/@AddressLine1"/>",
                    "street_line2": "<xsl:value-of select="/Shipment/ToAddress/@AddressLine2"/>",
                    "city": "<xsl:value-of select="/Shipment/ToAddress/@City"/>",
                    "postal_code": "<xsl:value-of select="/Shipment/ToAddress/@ZipCode"/>",
                    "state_or_province_code": "<xsl:value-of select="/Shipment/ToAddress/@State"/>",
                    "country_code": "<xsl:value-of select="/Shipment/ToAddress/@Country"/>"
                }
            },
            "fulfillment_local_date": "<xsl:value-of select="substring(/Shipment/@Createts,1,10)"/>",
            "shipment_ids": [
                <xsl:for-each select="/Shipment/Containers">
                    <xsl:for-each select="Container">
                        "<xsl:value-of select="@ShipmentContainerKey"/>"
                        <xsl:if test="./following-sibling::Container">,</xsl:if>
                    </xsl:for-each>
                </xsl:for-each>
            ]
        }
    ],
    "shipments":[
         <xsl:for-each select="/Shipment/Containers">
            <xsl:for-each select="Container">
                {
                    "shipment_id": "<xsl:value-of select="@ShipmentContainerKey"/>",
                    "fulfillment_id": "<xsl:value-of select="@ShipmentKey"/>",
                    "tracking_number": "<xsl:value-of select="@TrackingNo"/>",
                    "carrier_scac": "<xsl:value-of select="@SCAC"/>",
                    "carrier_name": "<xsl:value-of select="@SCAC"/>"
                }
                <xsl:if test="./following-sibling::Container">,</xsl:if>
            </xsl:for-each>
            ]
        </xsl:for-each>
}
</Input>
</PublishShipmentAPI>
</xsl:template>
</xsl:stylesheet>