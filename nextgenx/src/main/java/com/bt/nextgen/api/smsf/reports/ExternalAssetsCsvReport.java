package com.bt.nextgen.api.smsf.reports;

import com.bt.nextgen.api.smsf.constants.AssetClass;
import com.bt.nextgen.api.smsf.constants.PropertyType;
import com.bt.nextgen.api.smsf.model.AssetHoldings;
import com.bt.nextgen.api.smsf.model.ExternalAssetClassValuationDto;
import com.bt.nextgen.api.smsf.model.ExternalAssetDto;
import com.bt.nextgen.api.smsf.model.ExternalAssetHoldingsValuationDto;
import com.bt.nextgen.content.api.model.ContentDto;
import com.bt.nextgen.content.api.model.ContentKey;
import com.bt.nextgen.content.api.service.ContentDtoService;
import com.bt.nextgen.core.api.UriMappingConstants;
import com.bt.nextgen.core.reporting.ReportUtils;
import com.bt.nextgen.core.reporting.stereotype.Report;
import com.bt.nextgen.core.reporting.stereotype.ReportBean;
import com.btfin.panorama.core.security.encryption.EncodedString;
import com.bt.nextgen.core.web.ApiFormatter;
import com.bt.nextgen.reports.account.AccountReport;
import com.bt.nextgen.service.ServiceErrors;
import com.bt.nextgen.service.integration.account.AccountKey;
import com.bt.nextgen.service.integration.externalasset.builder.DateTimeConverter;
import com.bt.nextgen.service.integration.externalasset.builder.ExternalAssetHoldingsConverter;
import com.bt.nextgen.service.integration.externalasset.service.ExternalAssetIntegrationService;
import com.btfin.panorama.core.util.StringUtil;
import com.btfin.panorama.service.exception.FailFastErrorsImpl;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class to generated pdf external assets report
 */

@SuppressWarnings({"squid:S1172"})
@Report("externalAssetsCsvReport")
public class ExternalAssetsCsvReport  extends AccountReport {

    private static final String DISCLAIMER_CONTENT = "DS-IP-0062";

    @Autowired
    private ContentDtoService contentService;

    @Autowired
    private ExternalAssetIntegrationService externalAssetIntegrationService;

    /**
     * Convert date in string format "2015-07-15" to "15 Jul 2015"
     * @param inputDate input date to be converted
     */
    private String setDateFormatToDisplayFormat(String inputDate) {
        String toDate = null;
        if (StringUtils.isNotBlank(inputDate) && inputDate.length()>9) {
            String dateToConvert = inputDate.substring(0,10);
            DateTimeConverter dateTimeConverter = new DateTimeConverter();
            DateTime dateTime = dateTimeConverter.convert(dateToConvert);
            toDate = ApiFormatter.asShortDate(dateTime);
        }
        return toDate;
    }


    private void setClassOrder(ExternalAssetHoldingsValuationDto externalAssetHoldingsValDto, Map <String, ExternalAssetClassValuationDto>classOrder) {

        List<ExternalAssetDto> assetList = new ArrayList<>();
        ExternalAssetClassValuationDto externalAssetClassValuationDto = new ExternalAssetClassValuationDto();

        for (AssetClass assetClass: AssetClass.values()){
            String classId = assetClass.getCode();
            if(classOrder.get(classId)!=null) {
                assetList.addAll(classOrder.get(classId).getAssetList());
            }
        }
        externalAssetClassValuationDto.setAssetList(assetList);
        List <ExternalAssetClassValuationDto> classValuationDtoList = new ArrayList<>();
        classValuationDtoList.add(externalAssetClassValuationDto);
        externalAssetHoldingsValDto.setValuationByAssetClass(classValuationDtoList);
    }

    private void setPdfDetails(ExternalAssetHoldingsValuationDto externalAssetHoldingsValDto, Map<String, ExternalAssetHoldingsValuationDto> reportNameMap) {

        BigDecimal hundred = new BigDecimal("100");

        Map <String, ExternalAssetClassValuationDto>classOrder = new HashMap();

        for ( ExternalAssetClassValuationDto externalAssetClassValuationDto : externalAssetHoldingsValDto.getValuationByAssetClass() ) {
            classOrder.put(externalAssetClassValuationDto.getAssetClass(), externalAssetClassValuationDto);
            for (ExternalAssetDto externalAssetDto : externalAssetClassValuationDto.getAssetList()){
                BigDecimal percent = new BigDecimal(externalAssetDto.getPercentageTotal());
                String percentage = ApiFormatter.asDecimal(percent.multiply(hundred));
                externalAssetDto.setPercentageTotal(percentage); //multiply 100
                String classDesc = StringUtil.toProperCase(AssetClass.getByCode(externalAssetDto.getAssetClass()).getDescription());
                externalAssetDto.setAssetClass(classDesc);

                if(StringUtils.isNotBlank(externalAssetDto.getPropertyType())) {
                    externalAssetDto.setPropertyType(PropertyType.getByCode(externalAssetDto.getPropertyType().toLowerCase()).getShortDesc());
                }
                externalAssetDto.setValueDate(setDateFormatToDisplayFormat(externalAssetDto.getValueDate()));
                externalAssetDto.setMaturityDate(setDateFormatToDisplayFormat(externalAssetDto.getMaturityDate()));
                if (StringUtils.isNotBlank(externalAssetDto.getQuantity())) {
                    externalAssetDto.setQuantity(ApiFormatter.asIntegerString(new BigDecimal(externalAssetDto.getQuantity())));
                }
                if (StringUtils.isNotBlank(externalAssetDto.getMarketValue())) {
                    BigDecimal marketValue = new BigDecimal(externalAssetDto.getMarketValue());
                    externalAssetDto.setMarketValue((String)ReportUtils.toCurrencyString(marketValue));
                }
            }
        }
        setClassOrder(externalAssetHoldingsValDto,classOrder);
        reportNameMap.put("all",externalAssetHoldingsValDto);
    }

    @ReportBean("externalAssetsMapDto")
    public Map<String, ExternalAssetHoldingsValuationDto> getExternalAssetHoldingsValuation(Map<String, String> params) {

        String accountId = params.get(UriMappingConstants.ACCOUNT_ID_URI_MAPPING);
        Map<String, ExternalAssetHoldingsValuationDto> reportNameMap = new HashMap<>();

        AccountKey accountKey = AccountKey.valueOf(EncodedString.toPlainText(accountId));
        List<AccountKey> accountKeys = new ArrayList<>();
        accountKeys.add(accountKey);

        AssetHoldings assetHoldings = externalAssetIntegrationService.getExternalAssets(accountKeys, new DateTime());
        ExternalAssetHoldingsValuationDto externalAssetHoldingsValDto = ExternalAssetHoldingsConverter.toExternalAssetHoldingsValuationDto(assetHoldings);
        setPdfDetails(externalAssetHoldingsValDto,reportNameMap);
        return reportNameMap;
    }

    @ReportBean("valuationDate")
    public DateTime getStartDate(Map <String, String> params)
    {
        return new DateTime();
    }

    @ReportBean("reportType")
    public String getReportName(Map <String, String> params)
    {
        return "External Assets valuation";
    }

    @ReportBean("disclaimer")
    public String getDisclaimer(Map <String, String> params)
    {
        ServiceErrors serviceErrors = new FailFastErrorsImpl();
        ContentKey key = new ContentKey(DISCLAIMER_CONTENT);
        ContentDto content = contentService.find(key, serviceErrors);
		// TODO: Need to use jsoup to filter out here
		String filteredString = content.getContent().replace("<p>", "").replace("</p>", "");
		ContentDto filteredContent = new ContentDto(content.getKey(), filteredString);
        return filteredContent.getContent() !=null ? filteredContent.getContent() : "";
    }

    /**
     * @inheritDoc
     */
    @Override
    @PreAuthorize("@acctPermissionService.canTransact(#root.this.getAccountEncodedId(#params), 'account.portfolio.externalassets.view')")
    public Collection<?> getData(Map<String, Object> params, Map<String, Object> dataCollections) {
        return super.getData(params, dataCollections);
    }

}
